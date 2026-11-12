package parkinglot.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parkinglot.model.ParkingSpot;
import parkinglot.model.PaymentMethod;
import parkinglot.model.Receipt;
import parkinglot.model.Ticket;
import parkinglot.model.VehicleType;
import parkinglot.payment.PaymentProcessor;

// Orchestrator. Spots own their physical state; the lot owns the relationships (ticket <-> spot <-> plate).
public class ParkingLot {
    private final List<ParkingSpot> spots;                         // sorted best-fit first
    private final Map<VehicleType, Long> hourlyRateCents;          // Req 3
    private final Map<PaymentMethod, PaymentProcessor> processors; // Req 4
    private final Clock clock;

    private final Map<String, Ticket> activeTickets = new HashMap<>(); // ticketId -> ticket; the ONLY proof a ticket is valid
    private final Map<String, String> ticketByPlate = new HashMap<>(); // Req 1: plate -> active ticketId
    private long nextTicketNumber = 1;

    public ParkingLot(List<ParkingSpot> spots, Map<VehicleType, Long> hourlyRateCents,
                      Map<PaymentMethod, PaymentProcessor> processors, Clock clock) {
        if (spots == null || spots.isEmpty()) throw new IllegalArgumentException("at least one spot required");
        if (hourlyRateCents == null || processors == null || clock == null) {
            throw new IllegalArgumentException("rates, processors and clock required");
        }
        Set<String> ids = new HashSet<>();
        for (ParkingSpot spot : spots) {
            if (spot == null || !ids.add(spot.getId())) throw new IllegalArgumentException("null or duplicate spot " + spot);
        }
        for (VehicleType type : VehicleType.values()) {
            Long rate = hourlyRateCents.get(type);
            if (rate == null || rate < 0) throw new IllegalArgumentException("no valid rate for " + type);
        }
        for (PaymentMethod method : PaymentMethod.values()) {
            if (processors.get(method) == null) throw new IllegalArgumentException("no processor for " + method);
        }
        // Sorted once, so the first free fitting spot is the best one: smallest size, then lowest floor.
        List<ParkingSpot> sorted = new ArrayList<>(spots);
        sorted.sort(Comparator.comparing(ParkingSpot::getSize)
                .thenComparingInt(ParkingSpot::getFloor)
                .thenComparing(ParkingSpot::getId));
        this.spots = List.copyOf(sorted);
        this.hourlyRateCents = new EnumMap<>(hourlyRateCents);
        this.processors = new EnumMap<>(processors);
        this.clock = clock;
    }

    /*
     * Req 1. Core logic: reject a plate that is already inside, take the smallest free spot the
     *   vehicle fits in (a bigger one if its own size is full), issue a ticket stamped with now.
     * Edge cases: blank plate / null type -> IllegalArgumentException; plate inside ->
     *   VehicleAlreadyParkedException; nothing fits -> LotFullException. All checks before any mutation.
     */
    public Ticket enter(String licensePlate, VehicleType type) {
        if (licensePlate == null || licensePlate.isBlank()) throw new IllegalArgumentException("invalid plate");
        if (type == null) throw new IllegalArgumentException("invalid vehicle type");
        if (ticketByPlate.containsKey(licensePlate)) throw new VehicleAlreadyParkedException(licensePlate);

        ParkingSpot spot = findSpot(type);
        if (spot == null) throw new LotFullException(type);

        Ticket ticket = new Ticket("T" + nextTicketNumber++, licensePlate, type, spot, clock.instant());
        spot.occupy();
        activeTickets.put(ticket.getId(), ticket);
        ticketByPlate.put(licensePlate, ticket.getId());
        return ticket;
    }

    /*
     * Req 2-4. Core logic: look the ticket up in the ACTIVE map, price the stay, charge, and only
     *   after the charge succeeds retire the ticket and free the spot.
     * Edge cases: unknown or already-used ticket -> InvalidTicketException (never touches a spot);
     *   charge fails -> PaymentFailedException with nothing changed, so the driver can retry.
     */
    public Receipt exit(String ticketId, PaymentMethod method) {
        if (ticketId == null || method == null) throw new IllegalArgumentException("ticket and payment method required");
        // The trap: trusting the Ticket object alone would let a reused ticket free a spot that now
        // holds someone else's car. Validity lives here, and an exited ticket is removed from it.
        Ticket ticket = activeTickets.get(ticketId);
        if (ticket == null) throw new InvalidTicketException(ticketId);

        Instant now = clock.instant();
        long fee = calculateFee(ticket, now);
        try {
            processors.get(method).charge(ticketId, fee);
        } catch (RuntimeException e) {
            throw new PaymentFailedException(ticketId, e); // Req 4: gate stays closed, ticket still active
        }

        activeTickets.remove(ticketId);
        ticketByPlate.remove(ticket.getLicensePlate());
        ticket.getSpot().vacate();
        return new Receipt(ticket, now, fee, method);
    }

    // Req 3. ??? decision: priced by vehicle type, not spot size, so a car bumped to a LARGE spot
    // pays the car rate. Every started hour counts; the first hour is always charged.
    private long calculateFee(Ticket ticket, Instant exitTime) {
        long seconds = Duration.between(ticket.getEntryTime(), exitTime).getSeconds();
        long hours = Math.max(1, (seconds + 3599) / 3600);
        return hours * hourlyRateCents.get(ticket.getVehicleType());
    }

    // Linear scan: a lot has hundreds of spots, not millions.
    private ParkingSpot findSpot(VehicleType type) {
        for (ParkingSpot spot : spots) {
            if (spot.isFree() && type.fitsIn(spot.getSize())) return spot;
        }
        return null;
    }
}
