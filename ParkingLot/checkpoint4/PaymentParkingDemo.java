import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public class PaymentParkingDemo {
    enum VehicleType { BIKE, CAR }
    enum TicketStatus { ACTIVE, PAYMENT_PENDING, PAID, CLOSED }
    enum PaymentResult { SUCCESS, DECLINED, UNKNOWN }

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
    record Quote(Instant pricedUntil, long amountPaise) {}
    record Receipt(UUID ticketId, Instant exitedAt, long amountPaise, String paymentKey) {}

    interface AllocationStrategy { Optional<Spot> choose(List<Spot> eligible); }
    static final class NearestEntry implements AllocationStrategy {
        public Optional<Spot> choose(List<Spot> eligible) {
            return eligible.stream().min(Comparator.comparingInt(Spot::distanceFromEntry).thenComparingInt(Spot::id));
        }
    }
    interface PricingStrategy { long fee(Ticket ticket, Instant until); }
    static final class HourlyPricing implements PricingStrategy {
        public long fee(Ticket ticket, Instant until) {
            Duration stay = Duration.between(ticket.enteredAt(), until);
            if (stay.isNegative()) throw new IllegalArgumentException("Exit before entry");
            long hours = stay.toHours();
            if (!stay.minusHours(hours).isZero()) hours++;
            return Math.multiplyExact(Math.max(1, hours), ticket.vehicle().type() == VehicleType.BIKE ? 2000 : 5000);
        }
    }

    interface PaymentProcessor {
        // Same key + amount must resolve the same logical attempt, without a second charge.
        PaymentResult charge(String key, long amountPaise);
    }

    static final class FakePayments implements PaymentProcessor {
        private record Attempt(long amountPaise, PaymentResult result) {}
        private final Map<String, Attempt> attempts = new HashMap<>();
        private final Deque<PaymentResult> script = new ArrayDeque<>();
        private int successfulCharges;

        FakePayments(PaymentResult... outcomes) { script.addAll(List.of(outcomes)); }

        public synchronized PaymentResult charge(String key, long amountPaise) {
            Attempt prior = attempts.get(key);
            if (prior != null) {
                if (prior.amountPaise() != amountPaise) throw new IllegalArgumentException("Key reused with different amount");
                return prior.result();
            }
            PaymentResult reply = script.isEmpty() ? PaymentResult.SUCCESS : script.removeFirst();
            // UNKNOWN simulates a successful charge whose response was lost.
            PaymentResult actual = reply == PaymentResult.UNKNOWN ? PaymentResult.SUCCESS : reply;
            attempts.put(key, new Attempt(amountPaise, actual));
            if (actual == PaymentResult.SUCCESS) successfulCharges++;
            return reply;
        }

        synchronized int successfulCharges() { return successfulCharges; }
    }

    private static final class Session {
        final Ticket ticket;
        TicketStatus status = TicketStatus.ACTIVE;
        Quote quote;
        int attemptNumber;
        String paymentKey;
        Instant paidAt;
        Receipt receipt;

        Session(Ticket ticket) { this.ticket = ticket; }
    }

    static final class ParkingLot {
        private final List<Spot> spots;
        private final AllocationStrategy allocation;
        private final PricingStrategy pricing;
        private final PaymentProcessor payments;
        private final Map<Integer, UUID> occupancy = new HashMap<>();
        private final Map<String, UUID> byPlate = new HashMap<>();
        private final Map<UUID, Session> sessions = new HashMap<>();

        ParkingLot(List<Spot> spots, AllocationStrategy allocation, PricingStrategy pricing, PaymentProcessor payments) {
            this.spots = List.copyOf(spots);
            if (spots.stream().map(Spot::id).distinct().count() != spots.size())
                throw new IllegalArgumentException("Duplicate spot IDs");
            this.allocation = Objects.requireNonNull(allocation);
            this.pricing = Objects.requireNonNull(pricing);
            this.payments = Objects.requireNonNull(payments);
        }

        synchronized Ticket enter(Vehicle vehicle, Instant now) {
            Objects.requireNonNull(vehicle);
            Objects.requireNonNull(now);
            if (byPlate.containsKey(vehicle.plate())) throw new IllegalStateException("Vehicle already parked");
            List<Spot> eligible = spots.stream()
                    .filter(s -> s.type() == vehicle.type() && !occupancy.containsKey(s.id())).toList();
            Spot spot = allocation.choose(eligible).orElseThrow(() -> new IllegalStateException("No compatible spot"));
            if (!eligible.contains(spot)) throw new IllegalStateException("Strategy returned an ineligible spot");
            Ticket ticket = new Ticket(UUID.randomUUID(), vehicle, spot.id(), now);
            occupancy.put(spot.id(), ticket.id());
            byPlate.put(vehicle.plate(), ticket.id());
            sessions.put(ticket.id(), new Session(ticket));
            return ticket;
        }

        // shortcut: the whole operation is locked for a fast in-memory fake;
        // move real network payment outside this lock with durable attempt state.
        synchronized PaymentResult pay(UUID ticketId, Instant now) {
            Objects.requireNonNull(now);
            Session session = session(ticketId);
            if (session.status == TicketStatus.PAID || session.status == TicketStatus.CLOSED)
                return PaymentResult.SUCCESS;
            if (now.isBefore(session.ticket.enteredAt())) throw new IllegalArgumentException("Payment before entry");
            if (session.quote == null) {
                long fee = pricing.fee(session.ticket, now);
                if (fee < 0) throw new IllegalStateException("Negative fee");
                session.quote = new Quote(now, fee);
            }
            if (session.status == TicketStatus.ACTIVE) {
                session.attemptNumber++;
                session.paymentKey = ticketId + ":" + session.attemptNumber;
                session.status = TicketStatus.PAYMENT_PENDING;
            }
            // An exception deliberately leaves PAYMENT_PENDING and preserves this key.
            PaymentResult result = Objects.requireNonNull(payments.charge(session.paymentKey, session.quote.amountPaise()));
            if (result == PaymentResult.SUCCESS) {
                session.paidAt = now;
                session.status = TicketStatus.PAID;
            }
            if (result == PaymentResult.DECLINED) session.status = TicketStatus.ACTIVE;
            return result;
        }

        synchronized Receipt exit(UUID ticketId, Instant now) {
            Objects.requireNonNull(now);
            Session session = session(ticketId);
            if (session.status == TicketStatus.CLOSED) return session.receipt;
            if (session.status != TicketStatus.PAID) throw new IllegalStateException("Payment required");
            if (now.isBefore(session.paidAt)) throw new IllegalArgumentException("Exit before payment confirmation");
            Ticket ticket = session.ticket;
            if (!ticketId.equals(occupancy.get(ticket.spotId()))) throw new IllegalStateException("Occupancy mismatch");
            Receipt receipt = new Receipt(ticketId, now, session.quote.amountPaise(), session.paymentKey);
            occupancy.remove(ticket.spotId());
            byPlate.remove(ticket.vehicle().plate());
            session.receipt = receipt;
            session.status = TicketStatus.CLOSED;
            return receipt;
        }

        synchronized TicketStatus status(UUID ticketId) { return session(ticketId).status; }
        synchronized int available(VehicleType type) {
            Objects.requireNonNull(type);
            return (int) spots.stream().filter(s -> s.type() == type && !occupancy.containsKey(s.id())).count();
        }
        private Session session(UUID id) {
            Session session = sessions.get(id);
            if (session == null) throw new IllegalArgumentException("Unknown ticket");
            return session;
        }
    }

    static final class EntranceGate {
        private final ParkingLot lot;
        EntranceGate(ParkingLot lot) { this.lot = lot; }
        Ticket enter(Vehicle vehicle, Instant now) { return lot.enter(vehicle, now); }
    }
    static final class ExitGate {
        private final ParkingLot lot;
        ExitGate(ParkingLot lot) { this.lot = lot; }
        PaymentResult pay(UUID ticketId, Instant now) { return lot.pay(ticketId, now); }
        Receipt exit(UUID ticketId, Instant now) { return lot.exit(ticketId, now); }
    }

    private static final Instant START = Instant.parse("2026-01-01T10:00:00Z");
    private static final Vehicle CAR = new Vehicle("KA-01-A-100", VehicleType.CAR);
    private static ParkingLot lot(PaymentProcessor payments) {
        return new ParkingLot(List.of(new Spot(1, VehicleType.CAR, 10)), new NearestEntry(), new HourlyPricing(), payments);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void rejects(Class<? extends RuntimeException> type, Runnable action) {
        try { action.run(); }
        catch (RuntimeException e) {
            if (type.isInstance(e)) return;
            throw e;
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private static void checkPricing() {
        PricingStrategy pricing = new HourlyPricing();
        Ticket ticket = new Ticket(UUID.randomUUID(), CAR, 1, START);
        check(pricing.fee(ticket, START) == 5000, "Minimum one hour");
        check(pricing.fee(ticket, START.plusSeconds(3600)) == 5000, "Exact hour");
        check(pricing.fee(ticket, START.plusSeconds(3600).plusNanos(1)) == 10000, "Fraction after hour");
        rejects(IllegalArgumentException.class, () -> pricing.fee(ticket, START.minusSeconds(1)));
    }

    private static void checkPaymentLifecycle() {
        FakePayments payments = new FakePayments(PaymentResult.DECLINED, PaymentResult.UNKNOWN);
        ParkingLot lot = lot(payments);
        EntranceGate entry = new EntranceGate(lot);
        ExitGate exit = new ExitGate(lot);
        Ticket ticket = entry.enter(CAR, START);
        rejects(IllegalStateException.class, () -> entry.enter(CAR, START));
        rejects(IllegalStateException.class, () -> entry.enter(new Vehicle("OTHER", VehicleType.CAR), START));
        rejects(IllegalArgumentException.class, () -> exit.pay(UUID.randomUUID(), START));
        rejects(IllegalStateException.class, () -> exit.exit(ticket.id(), START));
        Instant end = START.plusSeconds(3601);
        check(exit.pay(ticket.id(), end) == PaymentResult.DECLINED, "Decline");
        check(lot.status(ticket.id()) == TicketStatus.ACTIVE, "Decline allows a new attempt");
        check(lot.available(VehicleType.CAR) == 0, "Decline retains spot");
        check(exit.pay(ticket.id(), end) == PaymentResult.UNKNOWN, "Lost response");
        check(lot.status(ticket.id()) == TicketStatus.PAYMENT_PENDING, "Unknown remains pending");
        rejects(IllegalStateException.class, () -> exit.exit(ticket.id(), end));
        check(exit.pay(ticket.id(), end.plusSeconds(3600)) == PaymentResult.SUCCESS, "Retry resolves same attempt");
        check(lot.available(VehicleType.CAR) == 0, "Paid vehicle still owns spot");
        Receipt receipt = exit.exit(ticket.id(), end.plusSeconds(3600));
        check(receipt.amountPaise() == 10000, "Quote stays frozen during retries");
        check(exit.exit(ticket.id(), end.plusSeconds(7200)).equals(receipt), "Repeat exit is idempotent");
        check(exit.pay(ticket.id(), end) == PaymentResult.SUCCESS, "Repeat payment returns success");
        check(payments.successfulCharges() == 1, "Exactly one successful charge");
        check(lot.available(VehicleType.CAR) == 1, "Exit releases spot");
        Ticket next = entry.enter(CAR, end.plusSeconds(7200));
        exit.exit(ticket.id(), end.plusSeconds(7201));
        check(lot.available(VehicleType.CAR) == 0, "Old ticket cannot release the next visit");
        check(!ticket.id().equals(next.id()), "New visit gets a new identity");
    }

    private static void checkThrowingPayment() {
        FakePayments provider = new FakePayments();
        PaymentProcessor flaky = new PaymentProcessor() {
            boolean first = true;
            public PaymentResult charge(String key, long amount) {
                PaymentResult result = provider.charge(key, amount);
                if (first) { first = false; throw new IllegalStateException("Response lost"); }
                return result;
            }
        };
        ParkingLot lot = lot(flaky);
        Ticket ticket = lot.enter(CAR, START);
        rejects(IllegalStateException.class, () -> lot.pay(ticket.id(), START));
        check(lot.status(ticket.id()) == TicketStatus.PAYMENT_PENDING, "Exception preserves attempt");
        check(lot.pay(ticket.id(), START) == PaymentResult.SUCCESS, "Retry after exception");
        check(provider.successfulCharges() == 1, "No duplicate charge after exception");
    }

    private static void checkConcurrentGates() throws Exception {
        ParkingLot lot = lot(new FakePayments());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                final int number = i;
                results.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    try {
                        new EntranceGate(lot).enter(new Vehicle("CAR-" + number, VehicleType.CAR), START);
                        return true;
                    } catch (IllegalStateException e) {
                        if (!"No compatible spot".equals(e.getMessage())) throw e;
                        return false;
                    }
                }));
            }
            check(ready.await(5, TimeUnit.SECONDS), "Workers ready");
            go.countDown();
            int winners = 0;
            for (Future<Boolean> result : results) if (result.get(5, TimeUnit.SECONDS)) winners++;
            check(winners == 1, "Only one gate gets the last spot");
        } finally { pool.shutdownNow(); }
    }

    public static void main(String[] args) throws Exception {
        checkPricing();
        checkPaymentLifecycle();
        checkThrowingPayment();
        checkConcurrentGates();
        ParkingLot lot = lot(new FakePayments());
        rejects(IllegalStateException.class, () -> lot.enter(new Vehicle("BIKE-1", VehicleType.BIKE), START));
        rejects(IllegalArgumentException.class, () -> new Vehicle(" ", VehicleType.CAR));
        rejects(IllegalArgumentException.class, () -> new ParkingLot(
                List.of(new Spot(1, VehicleType.CAR, 1), new Spot(1, VehicleType.CAR, 2)),
                new NearestEntry(), new HourlyPricing(), new FakePayments()));
        System.out.println("Checkpoint 4 passed: pricing boundaries, payment failures/retries, lifecycle, validation and concurrent gates.");
    }
}
