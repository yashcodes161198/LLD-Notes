import java.time.*;
import java.util.*;

public class TicketParkingDemo {
    enum VehicleType { BIKE, CAR }
    record Vehicle(String plate, VehicleType type) {
        Vehicle {
            if (plate == null || plate.isBlank()) throw new IllegalArgumentException("Plate required");
            plate = plate.strip().toUpperCase(Locale.ROOT);
            Objects.requireNonNull(type);
        }
    }
    record Ticket(UUID id, Vehicle vehicle, int spotId, Instant enteredAt) {}
    record Receipt(UUID ticketId, Instant exitedAt, long amountPaise) {}

    static final class ParkingSpot {
        final int id;
        final VehicleType type;
        private UUID ticketId;
        ParkingSpot(int id, VehicleType type) { this.id = id; this.type = type; }
        boolean accepts(Vehicle vehicle) { return ticketId == null && type == vehicle.type(); }
    }

    // shortcut: one thread and no payment provider; checkpoint 4 adds both concerns.
    static final class ParkingLot {
        private final List<ParkingSpot> spots = List.of(
                new ParkingSpot(1, VehicleType.BIKE), new ParkingSpot(2, VehicleType.CAR));
        private final Map<UUID, Ticket> active = new HashMap<>();
        private final Map<UUID, Receipt> closed = new HashMap<>();
        private final Map<String, UUID> byPlate = new HashMap<>();

        Ticket enter(Vehicle vehicle, Instant now) {
            Objects.requireNonNull(now);
            if (byPlate.containsKey(vehicle.plate())) throw new IllegalStateException("Vehicle already parked");
            ParkingSpot spot = spots.stream().filter(s -> s.accepts(vehicle)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("No compatible spot"));
            Ticket ticket = new Ticket(UUID.randomUUID(), vehicle, spot.id, now);
            spot.ticketId = ticket.id();
            active.put(ticket.id(), ticket);
            byPlate.put(vehicle.plate(), ticket.id());
            return ticket;
        }

        Receipt exit(UUID ticketId, Instant now) {
            Objects.requireNonNull(now);
            if (closed.containsKey(ticketId)) return closed.get(ticketId);
            Ticket ticket = active.get(ticketId);
            if (ticket == null) throw new IllegalArgumentException("Unknown ticket");
            long fee = fee(ticket, now);
            ParkingSpot spot = spots.stream().filter(s -> s.id == ticket.spotId()).findFirst().orElseThrow();
            if (!ticketId.equals(spot.ticketId)) throw new IllegalStateException("Occupancy mismatch");
            Receipt receipt = new Receipt(ticketId, now, fee);
            spot.ticketId = null;
            active.remove(ticketId);
            byPlate.remove(ticket.vehicle().plate());
            closed.put(ticketId, receipt);
            return receipt;
        }

        private long fee(Ticket ticket, Instant now) {
            Duration stay = Duration.between(ticket.enteredAt(), now);
            if (stay.isNegative()) throw new IllegalArgumentException("Exit before entry");
            long hours = stay.toHours();
            if (!stay.minusHours(hours).isZero()) hours++;
            long ratePaise = ticket.vehicle().type() == VehicleType.BIKE ? 2000 : 5000;
            return Math.multiplyExact(Math.max(1, hours), ratePaise);
        }
    }

    public static void main(String[] args) {
        ParkingLot lot = new ParkingLot();
        Instant start = Instant.parse("2026-01-01T10:00:00Z");
        Ticket ticket = lot.enter(new Vehicle("KA-01-A-100", VehicleType.CAR), start);
        Receipt receipt = lot.exit(ticket.id(), start.plusSeconds(3601));
        if (receipt.amountPaise() != 10000) throw new AssertionError("Round started hours up");
        if (!lot.exit(ticket.id(), start.plusSeconds(7200)).equals(receipt))
            throw new AssertionError("Repeated exit must return the original receipt");
        lot.enter(ticket.vehicle(), start.plusSeconds(7200));
        System.out.println("Checkpoint 2 passed: ticket, fee, repeat exit and re-entry.");
    }
}
