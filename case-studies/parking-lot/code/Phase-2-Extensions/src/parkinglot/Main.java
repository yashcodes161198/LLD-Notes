package parkinglot;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import parkinglot.model.ParkingSpot;
import parkinglot.model.PaymentMethod;
import parkinglot.model.SpotSize;
import parkinglot.model.Ticket;
import parkinglot.model.VehicleType;
import parkinglot.payment.CardPaymentProcessor;
import parkinglot.payment.CashPaymentProcessor;
import parkinglot.payment.PaymentProcessor;
import parkinglot.pricing.FlatRatePricing;
import parkinglot.pricing.HourlyPricing;
import parkinglot.pricing.PeakHourPricing;
import parkinglot.selection.LowestFloorSelector;
import parkinglot.selection.SmallestFitSelector;
import parkinglot.service.InvalidTicketException;
import parkinglot.service.LotFullException;
import parkinglot.service.ParkingLot;
import parkinglot.service.PaymentFailedException;
import parkinglot.service.VehicleAlreadyParkedException;

import static parkinglot.model.PaymentMethod.CARD;
import static parkinglot.model.PaymentMethod.CASH;
import static parkinglot.model.SpotSize.LARGE;
import static parkinglot.model.SpotSize.MEDIUM;
import static parkinglot.model.SpotSize.SMALL;
import static parkinglot.model.VehicleType.CAR;
import static parkinglot.model.VehicleType.MOTORCYCLE;
import static parkinglot.model.VehicleType.TRUCK;

// Self-checks: each one throws if the lot misbehaves.
public class Main {
    static final Map<VehicleType, Long> RATES = Map.of(MOTORCYCLE, 100L, CAR, 300L, TRUCK, 600L); // cents per hour
    static final Map<VehicleType, Long> PEAK = Map.of(MOTORCYCLE, 200L, CAR, 600L, TRUCK, 1200L);
    static final Map<VehicleType, Long> FLAT = Map.of(MOTORCYCLE, 500L, CAR, 1000L, TRUCK, 2000L);

    public static void main(String[] args) throws Exception {
        baseTrace();
        ext1PricingStrategies();
        ext2SpotSelection();
        ext3AvailabilityBoard();
        ext4ConcurrentGates();
        badConfigFailsAtStartup();
        demoWithRealProcessors();
        System.out.println("\nAll checks passed.");
    }

    // Phase 1's trace through the Phase 1 constructor (hourly + smallest fit by default).
    static void baseTrace() {
        System.out.println("=== Base trace (Phase 1 API) ===");
        ParkingSpot m1 = new ParkingSpot("M1", 0, MEDIUM);
        TestClock clock = new TestClock();
        RecordingProcessor card = new RecordingProcessor();
        RecordingProcessor cash = new RecordingProcessor();
        ParkingLot lot = new ParkingLot(List.of(new ParkingSpot("L1", 1, LARGE), m1, new ParkingSpot("S1", 0, SMALL)),
                RATES, Map.of(CARD, card, CASH, cash), clock);

        Ticket t1 = lot.enter("KA-1", CAR);
        expect(t1.getSpot().getId(), "M1", "KA-1 CAR -> M1");
        expect(lot.enter("KA-2", CAR).getSpot().getId(), "L1", "KA-2 CAR -> L1 (bumped up)");
        expectThrows(() -> lot.enter("KA-3", CAR), LotFullException.class, "KA-3 CAR, lot full for cars");
        expectThrows(() -> lot.enter("KA-1", CAR), VehicleAlreadyParkedException.class, "KA-1 again");

        clock.advance(Duration.ofMinutes(130));
        expect(lot.exit(t1.getId(), CARD).getAmountCents(), 900L, "T1 after 2h10m -> 900");
        lot.enter("KA-4", CAR);
        expectThrows(() -> lot.exit(t1.getId(), CARD), InvalidTicketException.class, "T1 used a second time");
        check(!m1.isFree(), "stale T1 must not free KA-4's spot");
        check(card.count() == 1, "T1 charged exactly once");

        cash.down = true;
        Ticket t4 = lot.enter("MC-1", MOTORCYCLE);
        expectThrows(() -> lot.exit(t4.getId(), CASH), PaymentFailedException.class, "MC-1 cash declined");
        expect(lot.exit(t4.getId(), CARD).getAmountCents(), 100L, "MC-1 retries by card (ticket was restored)");
    }

    static void ext1PricingStrategies() {
        System.out.println("\n=== EXT 1: pricing strategies ===");
        TestClock clock = new TestClock(Instant.parse("2026-01-01T07:30:00Z"));
        PeakHourPricing peak = new PeakHourPricing(RATES, PEAK, LocalTime.of(8, 0), LocalTime.of(18, 0), ZoneOffset.UTC);
        ParkingLot lot = new ParkingLot(List.of(new ParkingSpot("M1", 0, MEDIUM), new ParkingSpot("M2", 0, MEDIUM)),
                peak, new SmallestFitSelector(), processors(), clock);

        Ticket t = lot.enter("KA-1", CAR);
        clock.advance(Duration.ofMinutes(100));
        expect(lot.exit(t.getId(), CARD).getAmountCents(), 900L, "07:30-09:10 -> 07:30 off-peak 300 + 08:30 peak 600");

        lot.setPricingStrategy(new FlatRatePricing(FLAT));
        Ticket e = lot.enter("KA-2", CAR);
        clock.advance(Duration.ofHours(7));
        expect(lot.exit(e.getId(), CARD).getAmountCents(), 1000L, "event day: flat 1000 for 7 hours");

        lot.setPricingStrategy(new HourlyPricing(RATES));
        Ticket h = lot.enter("KA-3", CAR);
        clock.advance(Duration.ofMinutes(61));
        expect(lot.exit(h.getId(), CARD).getAmountCents(), 600L, "back to hourly: 61 min -> 2 hours");
        expectThrows(() -> lot.setPricingStrategy(null), IllegalArgumentException.class, "null pricing");
    }

    static void ext2SpotSelection() {
        System.out.println("\n=== EXT 2: spot selection ===");
        List<ParkingSpot> spots = List.of(new ParkingSpot("F2-M", 2, MEDIUM), new ParkingSpot("F0-L", 0, LARGE));
        ParkingLot smallest = new ParkingLot(spots, new HourlyPricing(RATES), new SmallestFitSelector(), processors(), new TestClock());
        expect(smallest.enter("KA-1", CAR).getSpot().getId(), "F2-M", "smallest fit -> MEDIUM on floor 2");

        List<ParkingSpot> spots2 = List.of(new ParkingSpot("F2-M", 2, MEDIUM), new ParkingSpot("F0-L", 0, LARGE));
        ParkingLot nearest = new ParkingLot(spots2, new HourlyPricing(RATES), new LowestFloorSelector(), processors(), new TestClock());
        expect(nearest.enter("KA-1", CAR).getSpot().getId(), "F0-L", "lowest floor -> LARGE on floor 0");
        expect(nearest.enter("KA-2", CAR).getSpot().getId(), "F2-M", "next car -> floor 2");
    }

    static void ext3AvailabilityBoard() {
        System.out.println("\n=== EXT 3: availability board ===");
        ParkingLot lot = new ParkingLot(List.of(new ParkingSpot("S1", 0, SMALL), new ParkingSpot("M1", 0, MEDIUM),
                new ParkingSpot("M2", 0, MEDIUM), new ParkingSpot("L1", 1, LARGE)), RATES, processors(), new TestClock());
        expect(lot.freeSpotsBySize(), Map.of(SMALL, 1, MEDIUM, 2, LARGE, 1), "empty lot");
        Ticket t = lot.enter("KA-1", CAR);
        lot.enter("TR-1", TRUCK);
        expect(lot.freeSpotsBySize(), Map.of(SMALL, 1, MEDIUM, 1, LARGE, 0), "after a car and a truck");
        lot.exit(t.getId(), CARD);
        expect(lot.freeSpotsBySize(), Map.of(SMALL, 1, MEDIUM, 2, LARGE, 0), "after the car leaves");
    }

    static void ext4ConcurrentGates() throws Exception {
        System.out.println("\n=== EXT 4: concurrent gates ===");
        for (int round = 0; round < 50; round++) {
            RecordingProcessor card = new RecordingProcessor();
            ParkingLot lot = new ParkingLot(List.of(new ParkingSpot("M1", 0, MEDIUM), new ParkingSpot("M2", 0, MEDIUM),
                    new ParkingSpot("L1", 1, LARGE)), RATES, Map.of(CARD, card, CASH, card), new TestClock());

            List<Object> samePlate = race(Collections.nCopies(4, () -> lot.enter("DUP", CAR)));
            check(count(samePlate, Ticket.class) == 1, "same plate at 4 gates -> exactly one ticket, got " + samePlate);
            check(count(samePlate, VehicleAlreadyParkedException.class) == 3, "the other 3 get AlreadyParked");

            List<Callable<Object>> cars = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                String plate = "CAR-" + i;
                cars.add(() -> lot.enter(plate, CAR));
            }
            List<Object> parked = race(cars);
            check(count(parked, Ticket.class) == 2, "8 cars, 2 spots left -> exactly 2 tickets, got " + parked);
            check(count(parked, LotFullException.class) == 6, "the other 6 get LotFull");
            parked.addAll(samePlate);
            long distinctSpots = parked.stream().filter(o -> o instanceof Ticket)
                    .map(o -> ((Ticket) o).getSpot().getId()).distinct().count();
            check(distinctSpots == 3, "no spot double-booked");

            Ticket t = (Ticket) parked.stream().filter(o -> o instanceof Ticket).findFirst().orElseThrow();
            List<Object> exits = race(Collections.nCopies(4, () -> lot.exit(t.getId(), CARD)));
            check(exits.size() - count(exits, InvalidTicketException.class) == 1, "one ticket, 4 gates -> exactly one exit");
            check(card.count() == 1, "charged exactly once");
            check(t.getSpot().isFree(), "spot freed once");
        }
        System.out.println("OK     50 rounds: same plate x4 -> 1 ticket, 8 cars/2 spots -> 2 tickets, same ticket x4 -> 1 exit, 1 charge");
    }

    static void badConfigFailsAtStartup() {
        System.out.println("\n=== Bad config fails at startup ===");
        List<ParkingSpot> spots = List.of(new ParkingSpot("A", 0, MEDIUM));
        Clock clock = new TestClock();
        expectThrows(() -> new ParkingLot(spots, null, new SmallestFitSelector(), processors(), clock),
                IllegalArgumentException.class, "null pricing");
        expectThrows(() -> new ParkingLot(spots, new HourlyPricing(RATES), null, processors(), clock),
                IllegalArgumentException.class, "null selector");
        expectThrows(() -> new ParkingLot(spots, Map.of(CAR, 300L), processors(), clock), IllegalArgumentException.class,
                "Phase 1 constructor, missing rates");
        expectThrows(() -> new HourlyPricing(Map.of(CAR, 300L, MOTORCYCLE, 100L, TRUCK, -1L)), IllegalArgumentException.class,
                "negative rate");
        expectThrows(() -> new PeakHourPricing(RATES, PEAK, LocalTime.of(18, 0), LocalTime.of(8, 0), ZoneOffset.UTC),
                IllegalArgumentException.class, "peak window backwards");
        expectThrows(() -> new FlatRatePricing(Map.of(CAR, 1000L)), IllegalArgumentException.class, "flat rate missing types");
        expectThrows(() -> new ParkingLot(spots, RATES, Map.of(CARD, new RecordingProcessor()), clock),
                IllegalArgumentException.class, "CASH has no processor");
    }

    // Production wiring: the stub processors print instead of calling a provider.
    static void demoWithRealProcessors() {
        System.out.println("\n=== Demo with the stub processors ===");
        TestClock clock = new TestClock(Instant.parse("2026-01-01T17:00:00Z"));
        ParkingLot lot = new ParkingLot(
                List.of(new ParkingSpot("G-S1", 0, SMALL), new ParkingSpot("G-M1", 0, MEDIUM), new ParkingSpot("1-L1", 1, LARGE)),
                new PeakHourPricing(RATES, PEAK, LocalTime.of(8, 0), LocalTime.of(18, 0), ZoneOffset.UTC),
                new LowestFloorSelector(),
                Map.of(CARD, new CardPaymentProcessor(), CASH, new CashPaymentProcessor()), clock);
        Ticket bike = lot.enter("MH-12-BIKE", MOTORCYCLE);
        Ticket truck = lot.enter("MH-12-TRUCK", TRUCK);
        clock.advance(Duration.ofMinutes(90));
        expect(lot.exit(bike.getId(), CASH).getAmountCents(), 300L, "bike 17:00-18:30 -> peak 200 + off-peak 100");
        expect(lot.exit(truck.getId(), CARD).getAmountCents(), 1800L, "truck 17:00-18:30 -> peak 1200 + off-peak 600");
    }

    // ---- helpers ----

    // Test double: records charges (thread-safe), optionally always declines.
    static class RecordingProcessor implements PaymentProcessor {
        volatile boolean down;
        final List<String> charges = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void charge(String ticketId, long amountCents) {
            if (down) throw new IllegalStateException("declined");
            charges.add(ticketId + ":" + amountCents);
        }

        int count() { return charges.size(); }
    }

    static class TestClock extends Clock {
        private volatile Instant now;

        TestClock() { this(Instant.parse("2026-01-01T10:00:00Z")); }
        TestClock(Instant start) { now = start; }

        void advance(Duration d) { now = now.plus(d); }

        @Override public Instant instant() { return now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
    }

    static Map<PaymentMethod, PaymentProcessor> processors() {
        RecordingProcessor p = new RecordingProcessor();
        return Map.of(CARD, p, CASH, p);
    }

    // Runs every action on its own thread, released together; returns each result or the exception it threw.
    static List<Object> race(List<Callable<Object>> actions) throws InterruptedException {
        CountDownLatch startGun = new CountDownLatch(1);
        List<Object> results = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();
        for (Callable<Object> action : actions) {
            Thread t = new Thread(() -> {
                try {
                    startGun.await();
                    results.add(action.call());
                } catch (Exception e) {
                    results.add(e);
                }
            });
            threads.add(t);
            t.start();
        }
        startGun.countDown();
        for (Thread t : threads) t.join();
        return results;
    }

    static long count(List<Object> results, Class<?> type) {
        return results.stream().filter(type::isInstance).count();
    }

    static void expect(Object got, Object want, String label) {
        check(Objects.equals(got, want), label + ": expected " + want + ", got " + got);
        System.out.println("OK     " + label + "  " + got);
    }

    static void expectThrows(Runnable action, Class<? extends RuntimeException> type, String label) {
        try {
            action.run();
        } catch (RuntimeException e) {
            check(type.isInstance(e), label + ": expected " + type.getSimpleName() + ", got " + e);
            System.out.println("THROWS " + label + " -> " + e.getMessage());
            return;
        }
        throw new IllegalStateException(label + ": expected an exception");
    }

    static void check(boolean ok, String message) {
        if (!ok) throw new IllegalStateException("CHECK FAILED: " + message);
    }
}
