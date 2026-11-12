package parkinglot.service;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import parkinglot.model.ParkingSpot;
import parkinglot.model.PaymentMethod;
import parkinglot.model.Receipt;
import parkinglot.model.SpotSize;
import parkinglot.model.Ticket;
import parkinglot.model.VehicleType;
import parkinglot.payment.PaymentProcessor;
import parkinglot.pricing.HourlyPricing;
import parkinglot.pricing.PricingStrategy;
import parkinglot.selection.SmallestFitSelector;
import parkinglot.selection.SpotSelector;

// Orchestrator. Spots own their physical state; the lot owns the relationships (ticket <-> spot <-> plate).
// EXT 4: safe for many entry/exit gates at once, with no lock held across the payment call.
public class ParkingLot {
    private static final String ENTERING = ""; // plate claimed, ticket not issued yet

    private final List<ParkingSpot> spots;
    private final Map<VehicleType, List<ParkingSpot>> candidatesByType; // EXT 2: selector's order, computed once
    private final Map<PaymentMethod, PaymentProcessor> processors;
    private final Clock clock;
    private volatile PricingStrategy pricing; // EXT 1: swappable at runtime (event days)

    private final Map<String, Ticket> activeTickets = new ConcurrentHashMap<>(); // the ONLY proof a ticket is valid
    private final Map<String, String> ticketByPlate = new ConcurrentHashMap<>(); // plate -> ticketId (or ENTERING)
    private final AtomicLong nextTicketNumber = new AtomicLong(1);

    // Phase 1 constructor still works: hourly pricing, smallest-fit selection.
    public ParkingLot(List<ParkingSpot> spots, Map<VehicleType, Long> hourlyRateCents,
                      Map<PaymentMethod, PaymentProcessor> processors, Clock clock) {
        this(spots, new HourlyPricing(hourlyRateCents), new SmallestFitSelector(), processors, clock);
    }

    public ParkingLot(List<ParkingSpot> spots, PricingStrategy pricing, SpotSelector selector,
                      Map<PaymentMethod, PaymentProcessor> processors, Clock clock) {
        if (spots == null || spots.isEmpty()) throw new IllegalArgumentException("at least one spot required");
        if (pricing == null || selector == null || processors == null || clock == null) {
            throw new IllegalArgumentException("pricing, selector, processors and clock required");
        }
        Set<String> ids = new HashSet<>();
        for (ParkingSpot spot : spots) {
            if (spot == null || !ids.add(spot.getId())) throw new IllegalArgumentException("null or duplicate spot " + spot);
        }
        for (PaymentMethod method : PaymentMethod.values()) {
            if (processors.get(method) == null) throw new IllegalArgumentException("no processor for " + method);
        }
        this.spots = List.copyOf(spots);
        Map<VehicleType, List<ParkingSpot>> byType = new EnumMap<>(VehicleType.class);
        for (VehicleType type : VehicleType.values()) {
            byType.put(type, List.copyOf(selector.candidates(this.spots, type)));
        }
        this.candidatesByType = byType;
        this.processors = new EnumMap<>(processors);
        this.clock = clock;
        this.pricing = pricing;
    }

    // EXT 1. ??? decision: the strategy in force at exit prices the whole stay.
    public void setPricingStrategy(PricingStrategy pricing) {
        if (pricing == null) throw new IllegalArgumentException("pricing required");
        this.pricing = pricing;
    }

    // Req 1 + EXT 4. Claim the plate first (putIfAbsent) so one plate at two gates gets one ticket,
    // then claim a spot by CAS. A full lot releases the plate claim before throwing.
    public Ticket enter(String licensePlate, VehicleType type) {
        if (licensePlate == null || licensePlate.isBlank()) throw new IllegalArgumentException("invalid plate");
        if (type == null) throw new IllegalArgumentException("invalid vehicle type");
        if (ticketByPlate.putIfAbsent(licensePlate, ENTERING) != null) throw new VehicleAlreadyParkedException(licensePlate);

        ParkingSpot spot = claimSpot(type);
        if (spot == null) {
            ticketByPlate.remove(licensePlate);
            throw new LotFullException(type);
        }
        Ticket ticket = new Ticket("T" + nextTicketNumber.getAndIncrement(), licensePlate, type, spot, clock.instant());
        activeTickets.put(ticket.getId(), ticket);
        ticketByPlate.put(licensePlate, ticket.getId());
        return ticket;
    }

    // Req 2-4 + EXT 4. remove() claims the ticket atomically: of two gates exiting the same ticket,
    // exactly one gets it. A failed charge puts it back, so nothing changes and the driver can retry.
    public Receipt exit(String ticketId, PaymentMethod method) {
        if (ticketId == null || method == null) throw new IllegalArgumentException("ticket and payment method required");
        Ticket ticket = activeTickets.remove(ticketId);
        if (ticket == null) throw new InvalidTicketException(ticketId);

        Instant now = clock.instant();
        long fee;
        try {
            fee = pricing.feeCents(ticket.getVehicleType(), ticket.getEntryTime(), now);
            processors.get(method).charge(ticketId, fee);
        } catch (RuntimeException e) {
            activeTickets.put(ticketId, ticket); // restore: no side effect happened
            throw new PaymentFailedException(ticketId, e);
        }

        ticketByPlate.remove(ticket.getLicensePlate());
        ticket.getSpot().vacate();
        return new Receipt(ticket, now, fee, method);
    }

    // EXT 3: display board. A snapshot; it can be stale by the time a driver reaches the spot.
    public Map<SpotSize, Integer> freeSpotsBySize() {
        Map<SpotSize, Integer> free = new EnumMap<>(SpotSize.class);
        for (SpotSize size : SpotSize.values()) free.put(size, 0);
        for (ParkingSpot spot : spots) {
            if (spot.isFree()) free.merge(spot.getSize(), 1, Integer::sum);
        }
        return free;
    }

    private ParkingSpot claimSpot(VehicleType type) {
        for (ParkingSpot spot : candidatesByType.get(type)) {
            if (spot.tryOccupy()) return spot;
        }
        return null;
    }
}
