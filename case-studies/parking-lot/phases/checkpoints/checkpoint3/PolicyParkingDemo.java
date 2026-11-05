import java.time.*;
import java.util.*;

public class PolicyParkingDemo {
    enum VehicleType { BIKE, CAR }
    record Vehicle(String plate, VehicleType type) {
        Vehicle {
            if (plate == null || plate.isBlank()) throw new IllegalArgumentException("Plate required");
            plate = plate.strip().toUpperCase(Locale.ROOT);
            Objects.requireNonNull(type);
        }
    }
    record Spot(int id, VehicleType type, int distanceFromEntry) {
        Spot {
            Objects.requireNonNull(type);
            if (distanceFromEntry < 0) throw new IllegalArgumentException("Negative distance");
        }
    }
    record Ticket(UUID id, Vehicle vehicle, int spotId, Instant enteredAt) {}
    record Receipt(UUID ticketId, Instant exitedAt, long amountPaise) {}

    interface AllocationStrategy {
        Optional<Spot> choose(List<Spot> eligible);
    }
    static final class FirstAvailable implements AllocationStrategy {
        public Optional<Spot> choose(List<Spot> eligible) { return eligible.stream().findFirst(); }
    }
    static final class NearestEntry implements AllocationStrategy {
        public Optional<Spot> choose(List<Spot> eligible) {
            return eligible.stream().min(Comparator.comparingInt(Spot::distanceFromEntry).thenComparingInt(Spot::id));
        }
    }
    interface PricingStrategy {
        long fee(Ticket ticket, Instant until);
    }
    static final class HourlyPricing implements PricingStrategy {
        public long fee(Ticket ticket, Instant until) {
            Duration stay = Duration.between(ticket.enteredAt(), until);
            if (stay.isNegative()) throw new IllegalArgumentException("Exit before entry");
            long hours = stay.toHours();
            if (!stay.minusHours(hours).isZero()) hours++;
            return Math.multiplyExact(Math.max(1, hours), ticket.vehicle().type() == VehicleType.BIKE ? 2000 : 5000);
        }
    }
    static final class FlatPricing implements PricingStrategy {
        private final long amountPaise;
        FlatPricing(long amountPaise) {
            if (amountPaise < 0) throw new IllegalArgumentException("Negative fee");
            this.amountPaise = amountPaise;
        }
        public long fee(Ticket ticket, Instant until) {
            if (until.isBefore(ticket.enteredAt())) throw new IllegalArgumentException("Exit before entry");
            return amountPaise;
        }
    }

    // shortcut: single-threaded; checkpoint 4 makes the whole allocation operation atomic.
    static final class ParkingLot {
        private final List<Spot> spots;
        private final AllocationStrategy allocation;
        private final PricingStrategy pricing;
        private final Map<Integer, UUID> occupancy = new HashMap<>();
        private final Map<UUID, Ticket> active = new HashMap<>();
        private final Map<UUID, Receipt> closed = new HashMap<>();
        private final Map<String, UUID> byPlate = new HashMap<>();

        ParkingLot(List<Spot> spots, AllocationStrategy allocation, PricingStrategy pricing) {
            this.spots = List.copyOf(spots);
            if (spots.stream().map(Spot::id).distinct().count() != spots.size())
                throw new IllegalArgumentException("Duplicate spot IDs");
            this.allocation = Objects.requireNonNull(allocation);
            this.pricing = Objects.requireNonNull(pricing);
        }

        Ticket enter(Vehicle vehicle, Instant now) {
            Objects.requireNonNull(now);
            if (byPlate.containsKey(vehicle.plate())) throw new IllegalStateException("Vehicle already parked");
            List<Spot> eligible = spots.stream()
                    .filter(s -> s.type() == vehicle.type() && !occupancy.containsKey(s.id())).toList();
            Spot spot = allocation.choose(eligible).orElseThrow(() -> new IllegalStateException("No compatible spot"));
            if (!eligible.contains(spot)) throw new IllegalStateException("Strategy returned an ineligible spot");
            Ticket ticket = new Ticket(UUID.randomUUID(), vehicle, spot.id(), now);
            occupancy.put(spot.id(), ticket.id());
            active.put(ticket.id(), ticket);
            byPlate.put(vehicle.plate(), ticket.id());
            return ticket;
        }

        Receipt exit(UUID ticketId, Instant now) {
            Objects.requireNonNull(now);
            if (closed.containsKey(ticketId)) return closed.get(ticketId);
            Ticket ticket = active.get(ticketId);
            if (ticket == null) throw new IllegalArgumentException("Unknown ticket");
            long amount = pricing.fee(ticket, now);
            if (amount < 0) throw new IllegalStateException("Pricing returned a negative fee");
            if (!ticketId.equals(occupancy.get(ticket.spotId()))) throw new IllegalStateException("Occupancy mismatch");
            Receipt receipt = new Receipt(ticketId, now, amount);
            occupancy.remove(ticket.spotId());
            active.remove(ticketId);
            byPlate.remove(ticket.vehicle().plate());
            closed.put(ticketId, receipt);
            return receipt;
        }
    }

    public static void main(String[] args) {
        List<Spot> spots = List.of(new Spot(1, VehicleType.CAR, 100), new Spot(2, VehicleType.CAR, 10));
        Instant start = Instant.parse("2026-01-01T10:00:00Z");
        Vehicle car = new Vehicle("KA-01-A-100", VehicleType.CAR);
        ParkingLot first = new ParkingLot(spots, new FirstAvailable(), new HourlyPricing());
        if (first.enter(car, start).spotId() != 1) throw new AssertionError("First available");
        ParkingLot nearest = new ParkingLot(spots, new NearestEntry(), new FlatPricing(7500));
        Ticket ticket = nearest.enter(car, start);
        if (ticket.spotId() != 2) throw new AssertionError("Nearest entry");
        if (nearest.exit(ticket.id(), start.plusSeconds(3601)).amountPaise() != 7500)
            throw new AssertionError("Flat pricing");
        System.out.println("Checkpoint 3 passed: interchangeable allocation and pricing.");
    }
}
