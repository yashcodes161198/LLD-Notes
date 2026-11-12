package amazonlocker;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import amazonlocker.hardware.DoorActuator;
import amazonlocker.hardware.SolenoidDoorActuator;
import amazonlocker.locker.AccessCodeExpiredException;
import amazonlocker.locker.InvalidAccessCodeException;
import amazonlocker.locker.Locker;
import amazonlocker.locker.NoAvailableCompartmentException;
import amazonlocker.model.Compartment;
import amazonlocker.model.Size;

import static amazonlocker.model.Size.*;

// Self-checks: each one throws if the locker misbehaves.
public class Main {
    public static void main(String[] args) {
        verificationTrace();
        reusedCompartmentIsNotReopened();
        lockerFullAndBadInput();
        badConfigFailsAtStartup();
        demoWithRealHardware();
        System.out.println("\nAll checks passed.");
    }

    // The VERIFICATION TRACE from amazon_locker.py.
    static void verificationTrace() {
        System.out.println("=== Verification trace ===");
        Fixture f = new Fixture();

        String code = f.locker.depositPackage(MEDIUM);
        expect(f.door.lastOpened(), "M1", "deposit(MEDIUM) -> M1 opens, code " + code);
        check(f.c("M1").isOccupied(), "M1 occupied after deposit");

        expectThrows(() -> f.locker.pickup(""), InvalidAccessCodeException.class, "pickup(\"\")");
        expectThrows(() -> f.locker.pickup("ZZZZZZ"), InvalidAccessCodeException.class, "pickup(unknown code)");

        f.clock.advance(Duration.ofDays(3));
        f.locker.pickup(code);
        expect(f.door.lastOpened(), "M1", "pickup on day 3 -> M1 opens");
        check(!f.c("M1").isOccupied(), "M1 free after pickup");
        expectThrows(() -> f.locker.pickup(code), InvalidAccessCodeException.class, "same code twice -> invalid");

        String late = f.locker.depositPackage(MEDIUM);
        f.clock.advance(Duration.ofDays(9));
        int opensBefore = f.door.opened.size();
        expectThrows(() -> f.locker.pickup(late), AccessCodeExpiredException.class, "pickup on day 9 -> expired");
        check(f.c("M1").isOccupied(), "M1 stays occupied after an expired pickup (Req 4)");
        check(f.door.opened.size() == opensBefore, "door not opened on an expired pickup");

        expect(f.locker.openExpiredCompartments(), List.of("M1"), "openExpiredCompartments -> M1");
        check(!f.c("M1").isOccupied(), "M1 freed by staff run");

        f.clock.advance(Duration.ofDays(1));
        expectThrows(() -> f.locker.pickup(late), AccessCodeExpiredException.class, "pickup on day 10 -> still 'expired', not 'invalid'");
    }

    // TRICKY #6: the video version reopened a reused compartment on the next staff run.
    static void reusedCompartmentIsNotReopened() {
        System.out.println("\n=== TRICKY #6: a reused compartment is not reopened ===");
        Fixture f = new Fixture();
        f.locker.depositPackage(LARGE);                       // L1, never picked up
        f.clock.advance(Duration.ofDays(8));
        expect(f.locker.openExpiredCompartments(), List.of("L1"), "staff run 1 -> L1");

        String fresh = f.locker.depositPackage(LARGE);        // L1 reused for a new customer
        expect(f.door.lastOpened(), "L1", "new package goes into L1");
        expect(f.locker.openExpiredCompartments(), List.of(), "staff run 2 -> opens nothing");
        check(f.c("L1").isOccupied(), "new customer's package still locked in L1");

        f.locker.pickup(fresh);
        check(!f.c("L1").isOccupied(), "new customer picks up normally");
        expect(f.locker.openExpiredCompartments(), List.of(), "staff run 3 -> still nothing (idempotent)");
    }

    static void lockerFullAndBadInput() {
        System.out.println("\n=== Locker full, bad input ===");
        Fixture f = new Fixture();
        f.locker.depositPackage(SMALL);
        expectThrows(() -> f.locker.depositPackage(SMALL), NoAvailableCompartmentException.class, "second SMALL, only one S");
        f.locker.depositPackage(MEDIUM);
        f.locker.depositPackage(MEDIUM);
        expectThrows(() -> f.locker.depositPackage(MEDIUM), NoAvailableCompartmentException.class, "third MEDIUM");
        check(!f.c("L1").isOccupied(), "no fallback in Phase 1: L1 untouched");
        expectThrows(() -> f.locker.depositPackage(null), IllegalArgumentException.class, "null size");
        expectThrows(() -> f.locker.pickup(null), InvalidAccessCodeException.class, "null code");
    }

    static void badConfigFailsAtStartup() {
        System.out.println("\n=== Bad config fails at startup ===");
        DoorActuator door = new RecordingDoor();
        expectThrows(() -> new Locker(List.of()), IllegalArgumentException.class, "no compartments");
        expectThrows(() -> new Locker(List.of(new Compartment("A", SMALL, door), new Compartment("A", LARGE, door))),
                IllegalArgumentException.class, "duplicate compartment id");
        expectThrows(() -> new Compartment("X", null, door), IllegalArgumentException.class, "compartment without size");
    }

    // Production wiring: the stub actuator prints instead of driving a latch.
    static void demoWithRealHardware() {
        System.out.println("\n=== Demo with the stub door actuator ===");
        DoorActuator door = new SolenoidDoorActuator();
        Locker locker = new Locker(List.of(new Compartment("S1", SMALL, door), new Compartment("L1", LARGE, door)));
        String code = locker.depositPackage(LARGE);
        System.out.println("OK     driver deposited LARGE, code " + code);
        locker.pickup(code);
        System.out.println("OK     customer picked up with " + code);
    }

    // ---- helpers ----

    static class Fixture {
        final RecordingDoor door = new RecordingDoor();
        final TestClock clock = new TestClock();
        final List<Compartment> compartments = List.of(
                new Compartment("S1", SMALL, door), new Compartment("M1", MEDIUM, door),
                new Compartment("M2", MEDIUM, door), new Compartment("L1", LARGE, door));
        final Locker locker = new Locker(compartments, clock);

        Compartment c(String id) {
            return compartments.stream().filter(x -> x.getId().equals(id)).findFirst().orElseThrow();
        }
    }

    // Test double: records which doors were opened.
    static class RecordingDoor implements DoorActuator {
        final List<String> opened = new ArrayList<>();

        @Override
        public void open(String compartmentId) { opened.add(compartmentId); }

        String lastOpened() { return opened.get(opened.size() - 1); }
    }

    // A clock the checks can move forward.
    static class TestClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

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
