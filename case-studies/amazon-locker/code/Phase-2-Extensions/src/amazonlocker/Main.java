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
import amazonlocker.locker.InvalidReservationException;
import amazonlocker.locker.Locker;
import amazonlocker.locker.NoAvailableCompartmentException;
import amazonlocker.model.Compartment;
import amazonlocker.model.CompartmentStatus;

import static amazonlocker.model.CompartmentStatus.*;
import static amazonlocker.model.Size.*;

// Self-checks: each one throws if the locker misbehaves.
public class Main {
    public static void main(String[] args) {
        baseBehaviourStillHolds();
        ext1SizeFallback();
        ext2OutOfService();
        ext3TwoPhaseDeposit();
        badConfigFailsAtStartup();
        demoWithRealHardware();
        System.out.println("\nAll checks passed.");
    }

    // The Phase 1 trace, now through reserve + confirm inside depositPackage.
    static void baseBehaviourStillHolds() {
        System.out.println("=== Base trace ===");
        Fixture f = new Fixture();
        String code = f.locker.depositPackage(MEDIUM);
        expect(f.c("M1").getStatus(), OCCUPIED, "deposit(MEDIUM) -> M1 occupied");
        expectThrows(() -> f.locker.pickup(""), InvalidAccessCodeException.class, "pickup(\"\")");
        expectThrows(() -> f.locker.pickup("ZZZZZZ"), InvalidAccessCodeException.class, "pickup(unknown code)");
        f.clock.advance(Duration.ofDays(3));
        f.locker.pickup(code);
        expect(f.c("M1").getStatus(), AVAILABLE, "pickup on day 3 -> M1 free");

        String late = f.locker.depositPackage(MEDIUM);
        f.clock.advance(Duration.ofDays(9));
        expectThrows(() -> f.locker.pickup(late), AccessCodeExpiredException.class, "pickup on day 9 -> expired");
        expect(f.c("M1").getStatus(), OCCUPIED, "M1 stays occupied (Req 4)");
        expect(f.locker.openExpiredCompartments(), List.of("M1"), "staff run 1 -> M1");

        f.locker.depositPackage(MEDIUM);                      // M1 reused
        expect(f.locker.openExpiredCompartments(), List.of(), "staff run 2 -> reused M1 not reopened (TRICKY #6)");
        expectThrows(() -> f.locker.pickup(late), AccessCodeExpiredException.class, "late customer still gets 'expired'");
    }

    // EXT 1: a smaller package falls back to a larger compartment, never the other way.
    static void ext1SizeFallback() {
        System.out.println("\n=== EXT 1: size fallback ===");
        Fixture f = new Fixture();
        f.locker.depositPackage(SMALL);                       // S1
        f.locker.depositPackage(SMALL);
        expect(f.door.lastOpened(), "M1", "2nd SMALL -> M1 (next size up)");
        f.locker.depositPackage(SMALL);
        f.locker.depositPackage(SMALL);
        expect(f.door.lastOpened(), "L1", "4th SMALL -> L1");
        expectThrows(() -> f.locker.depositPackage(SMALL), NoAvailableCompartmentException.class, "5th SMALL -> full");
        expectThrows(() -> f.locker.depositPackage(LARGE), NoAvailableCompartmentException.class,
                "LARGE never uses a smaller compartment");
    }

    // EXT 2: broken compartments are skipped, and only an empty one can be taken out of service.
    static void ext2OutOfService() {
        System.out.println("\n=== EXT 2: out of service ===");
        Fixture f = new Fixture();
        f.locker.markOutOfService("M1");
        f.locker.depositPackage(MEDIUM);
        expect(f.door.lastOpened(), "M2", "M1 broken -> MEDIUM goes to M2");
        expectThrows(() -> f.locker.markOutOfService("M2"), IllegalStateException.class,
                "can't take M2 out of service while it holds a package");
        f.locker.returnToService("M1");
        f.locker.depositPackage(MEDIUM);
        expect(f.door.lastOpened(), "M1", "M1 back in service -> used again");
        expectThrows(() -> f.locker.markOutOfService("Z9"), IllegalArgumentException.class, "unknown compartment");
    }

    // EXT 3: no code until the deposit is confirmed; cancel and timeout give the compartment back.
    static void ext3TwoPhaseDeposit() {
        System.out.println("\n=== EXT 3: reserve -> confirm ===");
        Fixture f = new Fixture();
        String r1 = f.locker.reserveCompartment(LARGE);
        expect(f.c("L1").getStatus(), RESERVED, "reserve(LARGE) -> L1 reserved, door open");
        expectThrows(() -> f.locker.reserveCompartment(LARGE), NoAvailableCompartmentException.class,
                "a reserved compartment is not handed out twice");
        String code = f.locker.confirmDeposit(r1);
        expect(f.c("L1").getStatus(), OCCUPIED, "confirm -> L1 occupied, code issued");
        expectThrows(() -> f.locker.confirmDeposit(r1), InvalidReservationException.class, "confirm twice");
        f.locker.pickup(code);

        String r2 = f.locker.reserveCompartment(LARGE);
        f.locker.cancelReservation(r2);
        expect(f.c("L1").getStatus(), AVAILABLE, "cancel -> L1 available");
        expectThrows(() -> f.locker.cancelReservation(r2), InvalidReservationException.class, "cancel twice");

        String r3 = f.locker.reserveCompartment(LARGE);
        f.clock.advance(Duration.ofMinutes(5));
        expectThrows(() -> f.locker.confirmDeposit(r3), InvalidReservationException.class, "confirm after the timeout");
        expect(f.c("L1").getStatus(), AVAILABLE, "timed-out reservation released L1");

        f.locker.reserveCompartment(LARGE);                   // driver walks away
        f.clock.advance(Duration.ofMinutes(5));
        f.locker.reserveCompartment(LARGE);
        expect(f.c("L1").getStatus(), RESERVED, "next reserve reclaims the abandoned L1 lazily");
    }

    static void badConfigFailsAtStartup() {
        System.out.println("\n=== Bad config fails at startup ===");
        DoorActuator door = new RecordingDoor();
        expectThrows(() -> new Locker(List.of()), IllegalArgumentException.class, "no compartments");
        expectThrows(() -> new Locker(List.of(new Compartment("A", SMALL, door), new Compartment("A", LARGE, door))),
                IllegalArgumentException.class, "duplicate compartment id");
        Compartment c = new Compartment("X", SMALL, door);
        expectThrows(c::markOccupied, IllegalStateException.class, "OCCUPIED without a reservation");
    }

    // Production wiring: the stub actuator prints instead of driving a latch.
    static void demoWithRealHardware() {
        System.out.println("\n=== Demo with the stub door actuator ===");
        DoorActuator door = new SolenoidDoorActuator();
        Locker locker = new Locker(List.of(new Compartment("S1", SMALL, door), new Compartment("L1", LARGE, door)));
        String reservation = locker.reserveCompartment(SMALL);
        String code = locker.confirmDeposit(reservation);
        System.out.println("OK     driver reserved + confirmed SMALL, code " + code);
        String code2 = locker.depositPackage(SMALL);
        System.out.println("OK     second SMALL fell back to L1, code " + code2);
        locker.pickup(code);
        locker.pickup(code2);
        System.out.println("OK     both customers picked up");
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
