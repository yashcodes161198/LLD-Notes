package parkinglot;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import parkinglot.model.ParkingSpot;
import parkinglot.model.PaymentMethod;
import parkinglot.model.Receipt;
import parkinglot.model.SpotSize;
import parkinglot.model.Ticket;
import parkinglot.model.VehicleType;
import parkinglot.payment.CardPaymentProcessor;
import parkinglot.payment.CashPaymentProcessor;
import parkinglot.payment.PaymentProcessor;
import parkinglot.service.InvalidTicketException;
import parkinglot.service.LotFullException;
import parkinglot.service.ParkingLot;
import parkinglot.service.PaymentFailedException;
import parkinglot.service.VehicleAlreadyParkedException;

import static parkinglot.model.PaymentMethod.CARD;
import static parkinglot.model.PaymentMethod.CASH;
import static parkinglot.model.VehicleType.CAR;
import static parkinglot.model.VehicleType.MOTORCYCLE;
import static parkinglot.model.VehicleType.TRUCK;

// Self-checks: each one throws if the lot misbehaves.
public class Main {
    static final Map<VehicleType, Long> RATES = Map.of(MOTORCYCLE, 100L, CAR, 300L, TRUCK, 600L); // cents per hour

    public static void main(String[] args) {
        verificationTrace();
        badConfigFailsAtStartup();
        demoWithRealProcessors();
        System.out.println("\nAll checks passed.");
    }

    static void verificationTrace() {
        System.out.println("=== Verification trace ===");
        ParkingSpot s1 = new ParkingSpot("S1", 0, SpotSize.SMALL);
        ParkingSpot m1 = new ParkingSpot("M1", 0, SpotSize.MEDIUM);
        ParkingSpot m2 = new ParkingSpot("M2", 1, SpotSize.MEDIUM);
        ParkingSpot l1 = new ParkingSpot("L1", 1, SpotSize.LARGE);
        TestClock clock = new TestClock();
        RecordingProcessor card = new RecordingProcessor();
        RecordingProcessor cash = new RecordingProcessor();
        ParkingLot lot = new ParkingLot(List.of(l1, m2, s1, m1), RATES, Map.of(CARD, card, CASH, cash), clock);

        Ticket t1 = lot.enter("KA-1", CAR);
        expect(t1.getSpot().getId(), "M1", "KA-1 CAR -> M1 (smallest fit, lowest floor)");
        expect(lot.enter("KA-2", CAR).getSpot().getId(), "M2", "KA-2 CAR -> M2");
        Ticket t3 = lot.enter("KA-3", CAR);
        expect(t3.getSpot().getId(), "L1", "KA-3 CAR -> L1 (mediums full, bumped up)");
        expectThrows(() -> lot.enter("KA-4", CAR), LotFullException.class, "KA-4 CAR, S1 too small");
        expectThrows(() -> lot.enter("KA-1", CAR), VehicleAlreadyParkedException.class, "KA-1 again");
        expect(lot.enter("MC-1", MOTORCYCLE).getSpot().getId(), "S1", "MC-1 MOTORCYCLE -> S1");

        clock.advance(Duration.ofMinutes(130));
        Receipt r1 = lot.exit(t1.getId(), CARD);
        expect(r1.getAmountCents(), 900L, "T1 exits after 2h10m -> 3 started hours x 300");
        check(m1.isFree(), "M1 free after T1 exits");

        // The trap: T1 is reused after M1 was handed to someone else.
        Ticket t5 = lot.enter("KA-5", CAR);
        expect(t5.getSpot().getId(), "M1", "KA-5 CAR -> M1 (reused)");
        expectThrows(() -> lot.exit(t1.getId(), CARD), InvalidTicketException.class, "T1 used a second time");
        check(!m1.isFree(), "stale T1 must not free KA-5's spot");
        check(card.charges.size() == 1, "T1 charged exactly once");

        cash.down = true;
        expectThrows(() -> lot.exit(t3.getId(), CASH), PaymentFailedException.class, "T3 cash declined");
        check(!l1.isFree(), "declined payment keeps L1 occupied");
        expectThrows(() -> lot.enter("KA-3", CAR), VehicleAlreadyParkedException.class, "KA-3 still inside after decline");
        expect(lot.exit(t3.getId(), CARD).getAmountCents(), 900L, "T3 retries by card -> car rate in a LARGE spot");
        check(l1.isFree(), "L1 free after T3 pays");

        Ticket t6 = lot.enter("KA-6", CAR);
        clock.advance(Duration.ofMinutes(5));
        expect(lot.exit(t6.getId(), CARD).getAmountCents(), 300L, "5-minute stay -> first hour charged");

        expectThrows(() -> lot.enter(" ", CAR), IllegalArgumentException.class, "blank plate");
        expectThrows(() -> lot.enter("KA-9", null), IllegalArgumentException.class, "null vehicle type");
        expectThrows(() -> lot.exit(null, CARD), IllegalArgumentException.class, "null ticket id");
        expectThrows(() -> lot.exit(t5.getId(), null), IllegalArgumentException.class, "null payment method");
        expectThrows(() -> lot.exit("T999", CARD), InvalidTicketException.class, "unknown ticket");
    }

    static void badConfigFailsAtStartup() {
        System.out.println("\n=== Bad config fails at startup ===");
        RecordingProcessor p = new RecordingProcessor();
        List<ParkingSpot> spots = List.of(new ParkingSpot("A", 0, SpotSize.MEDIUM));
        Map<PaymentMethod, PaymentProcessor> both = Map.of(CARD, p, CASH, p);
        Clock clock = new TestClock();
        expectThrows(() -> new ParkingLot(List.of(), RATES, both, clock), IllegalArgumentException.class, "no spots");
        expectThrows(() -> new ParkingLot(List.of(new ParkingSpot("A", 0, SpotSize.SMALL), new ParkingSpot("A", 1, SpotSize.LARGE)),
                RATES, both, clock), IllegalArgumentException.class, "duplicate spot id");
        expectThrows(() -> new ParkingLot(spots, Map.of(CAR, 300L, MOTORCYCLE, 100L), both, clock),
                IllegalArgumentException.class, "TRUCK has no rate");
        expectThrows(() -> new ParkingLot(spots, RATES, Map.of(CARD, p), clock), IllegalArgumentException.class,
                "CASH has no processor");
        expectThrows(() -> new ParkingSpot("B", -1, SpotSize.SMALL), IllegalArgumentException.class, "negative floor");
    }

    // Production wiring: the stub processors print instead of calling a provider.
    static void demoWithRealProcessors() {
        System.out.println("\n=== Demo with the stub processors ===");
        TestClock clock = new TestClock();
        ParkingLot lot = new ParkingLot(
                List.of(new ParkingSpot("G-S1", 0, SpotSize.SMALL), new ParkingSpot("G-M1", 0, SpotSize.MEDIUM),
                        new ParkingSpot("1-L1", 1, SpotSize.LARGE)),
                RATES, Map.of(CARD, new CardPaymentProcessor(), CASH, new CashPaymentProcessor()), clock);
        Ticket bike = lot.enter("MH-12-BIKE", MOTORCYCLE);
        Ticket truck = lot.enter("MH-12-TRUCK", TRUCK);
        clock.advance(Duration.ofMinutes(90));
        expect(lot.exit(bike.getId(), CASH).getAmountCents(), 200L, "bike 1h30m by cash");
        expect(lot.exit(truck.getId(), CARD).getAmountCents(), 1200L, "truck 1h30m by card");
    }

    // ---- helpers ----

    // Test double: records charges, optionally always declines.
    static class RecordingProcessor implements PaymentProcessor {
        boolean down;
        final List<String> charges = new ArrayList<>();

        @Override
        public void charge(String ticketId, long amountCents) {
            if (down) throw new IllegalStateException("declined");
            charges.add(ticketId + ":" + amountCents);
        }
    }

    static class TestClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T10:00:00Z");

        void advance(Duration d) { now = now.plus(d); }

        @Override public Instant instant() { return now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
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
