package bookmyshow;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import bookmyshow.model.BookingStatus;
import bookmyshow.model.Seat;
import bookmyshow.model.SeatType;
import bookmyshow.model.Show;
import bookmyshow.notifier.SeatReleaseListener;
import bookmyshow.notifier.WaitlistNotifier;
import bookmyshow.pricing.FlatPricing;
import bookmyshow.pricing.SeatTypePricing;
import bookmyshow.room.RoomSchedule;
import bookmyshow.room.SlotUnavailableException;
import bookmyshow.room.TimeSlot;
import bookmyshow.service.BookingNotFoundException;
import bookmyshow.service.BookingSystem;
import bookmyshow.service.HoldExpiredException;
import bookmyshow.service.SeatUnavailableException;
import bookmyshow.service.ShowNotFoundException;

// Self-checks: each one throws if the booking system misbehaves.
public class Main {
    public static void main(String[] args) throws Exception {
        baseTraceStillPasses();
        ext1Pricing();
        ext2ReleaseNotifications();
        ext3RoomSchedule();
        concurrentHolds();
        badConfigFailsAtStartup();
        demoWithRealStubs();
        System.out.println("\nAll checks passed.");
    }

    // Phase 1's trace through the Phase 1 constructor: the old API still works.
    static void baseTraceStillPasses() {
        System.out.println("=== Base trace (Phase 1 API) ===");
        MutableClock clock = new MutableClock();
        BookingSystem system = new BookingSystem(List.of(show("S1")), clock);
        String b1 = system.holdSeats("u1", "S1", List.of("A1", "A2"));
        expectThrows(() -> system.holdSeats("u2", "S1", List.of("A2", "A3")), SeatUnavailableException.class, "A2 taken");
        expect(ids(system.getAvailableSeats("S1")), List.of("A3", "P1"), "all-or-nothing");
        system.confirmBooking(b1, "pay_1");
        system.confirmBooking(b1, "pay_1");
        expect(system.getBooking(b1).getStatus(), BookingStatus.CONFIRMED, "confirm is idempotent");
        expect(system.getBooking(b1).getAmount(), 0L, "default pricing is free");

        String b2 = system.holdSeats("u3", "S1", List.of("A3"));
        clock.advanceMinutes(11);
        expectThrows(() -> system.confirmBooking(b2, "p2"), HoldExpiredException.class, "B2 paid too late");
        String b3 = system.holdSeats("u4", "S1", List.of("A3"));
        system.cancelBooking(b2);
        expectThrows(() -> system.holdSeats("u5", "S1", List.of("A3")), SeatUnavailableException.class,
                "trap: late cancel of B2 left " + b3 + "'s A3 alone");
        expectThrows(() -> system.confirmBooking("B99", "x"), BookingNotFoundException.class, "unknown booking");
        expectThrows(() -> system.holdSeats("u1", "S9", List.of("A1")), ShowNotFoundException.class, "unknown show");
    }

    // EXT 1: Strategy, priced once at hold time.
    static void ext1Pricing() {
        System.out.println("\n=== EXT 1: pricing ===");
        BookingSystem system = new BookingSystem(List.of(show("S1")), new MutableClock(), prices(), List.of());
        String b1 = system.holdSeats("u1", "S1", List.of("A1", "P1"));
        expect(system.getBooking(b1).getAmount(), 25_000L + 40_000L, "REGULAR + PREMIUM");
        expectThrows(() -> system.holdSeats("u2", "S1", List.of("A2", "Z9")), IllegalArgumentException.class,
                "unknown seat fails before anything is held");
        expect(ids(system.getAvailableSeats("S1")), List.of("A2", "A3"), "A2 not held by the failed request");

        BookingSystem flat = new BookingSystem(List.of(show("S1")), new MutableClock(), new FlatPricing(10_000), List.of());
        expect(flat.getBooking(flat.holdSeats("u1", "S1", List.of("A1", "A2", "A3"))).getAmount(), 30_000L, "flat x3");
    }

    // EXT 2: Observer + sweeper. Only seats that really came back are reported.
    static void ext2ReleaseNotifications() {
        System.out.println("\n=== EXT 2: release notifications + expiry sweeper ===");
        MutableClock clock = new MutableClock();
        RecordingListener listener = new RecordingListener();
        BookingSystem system = new BookingSystem(List.of(show("S1"), show("S2")), clock, new FlatPricing(0),
                List.of(listener, new FailingListener()));

        String b1 = system.holdSeats("u1", "S1", List.of("A1", "A2"));
        system.cancelBooking(b1);
        expect(listener.events, List.of("S1 [A1, A2]"), "cancel notifies the freed seats (failing listener isolated)");
        system.cancelBooking(b1);
        expect(listener.events.size(), 1, "second cancel: nothing freed, no event");

        String b2 = system.holdSeats("u2", "S1", List.of("A3"));
        system.holdSeats("u3", "S2", List.of("P1"));
        expect(system.releaseExpiredHolds(), 0, "sweep before expiry frees nothing");
        clock.advanceMinutes(11);
        String b4 = system.holdSeats("u4", "S1", List.of("A3")); // re-holds expired B2's seat
        expect(system.releaseExpiredHolds(), 1, "sweep frees S2/P1 only; A3 belongs to " + b4);
        expect(listener.events, List.of("S1 [A1, A2]", "S2 [P1]"), "sweeper notified S2 [P1]");
        system.cancelBooking(b2);
        expect(listener.events.size(), 2, "late cancel of B2 frees nothing, no event");
        expect(ids(system.getAvailableSeats("S1")), List.of("A1", "A2", "P1"), "A3 still held by " + b4);
    }

    // EXT 3: same skeleton, overlap check instead of seat equality.
    static void ext3RoomSchedule() throws InterruptedException {
        System.out.println("\n=== EXT 3: meeting-room schedule ===");
        RoomSchedule room = new RoomSchedule("Everest");
        String r1 = room.reserve("u1", slot(10, 11));
        expectThrows(() -> room.reserve("u2", slot(10, 12)), SlotUnavailableException.class, "10-12 overlaps 10-11");
        expectThrows(() -> room.reserve("u2", slot(9, 11)), SlotUnavailableException.class, "9-11 overlaps 10-11");
        room.reserve("u2", slot(11, 12));
        System.out.println("OK     11-12 right after 10-11 (half-open)");
        room.reserve("u3", slot(9, 10));
        System.out.println("OK     9-10 right before 10-11");
        room.cancel(r1);
        room.cancel(r1);
        room.reserve("u4", slot(10, 11));
        expect(room.getReservedSlots().size(), 3, "cancel (twice) freed 10-11 and u4 took it");

        for (int round = 0; round < 100; round++) {
            RoomSchedule contested = new RoomSchedule("R" + round);
            List<String> winners = race(20, i -> contested.reserve("u" + i, slot(14, 15 + i % 2)));
            check(winners.size() == 1, "round " + round + ": overlapping room slots, expected 1 winner, got " + winners);
        }
        System.out.println("OK     100 rounds x 20 threads on overlapping slots -> exactly 1 winner each");
    }

    static void concurrentHolds() throws InterruptedException {
        System.out.println("\n=== Concurrency (still holds with pricing + listeners) ===");
        for (int round = 0; round < 100; round++) {
            BookingSystem system = new BookingSystem(List.of(show("S1")), new MutableClock(), prices(),
                    List.of(new RecordingListener()));
            List<String> winners = race(20, i -> system.holdSeats("u" + i, "S1",
                    i % 2 == 0 ? List.of("A1", "A2") : List.of("A2", "A3")));
            check(winners.size() == 1, "round " + round + ": expected 1 winner, got " + winners);
        }
        System.out.println("OK     100 rounds of [A1,A2] vs [A2,A3] -> exactly 1 winner each");
    }

    static void badConfigFailsAtStartup() {
        System.out.println("\n=== Bad config fails at startup ===");
        expectThrows(() -> new SeatTypePricing(Map.of(SeatType.REGULAR, 100L)), IllegalArgumentException.class,
                "PREMIUM has no price");
        expectThrows(() -> new FlatPricing(-1), IllegalArgumentException.class, "negative flat price");
        expectThrows(() -> new BookingSystem(List.of(show("S1")), new MutableClock(), null, List.of()),
                IllegalArgumentException.class, "no pricing strategy");
        List<SeatReleaseListener> withNull = new ArrayList<>();
        withNull.add(null);
        expectThrows(() -> new BookingSystem(List.of(show("S1")), new MutableClock(), prices(), withNull),
                IllegalArgumentException.class, "null listener");
        expectThrows(() -> slot(11, 10), IllegalArgumentException.class, "slot ends before it starts");
        expectThrows(() -> new BookingSystem(List.of(show("S1"), show("S1")), new MutableClock()),
                IllegalArgumentException.class, "duplicate show id");
    }

    // Production wiring: system clock, seat-type pricing, the stub waitlist notifier.
    static void demoWithRealStubs() {
        System.out.println("\n=== Demo with the stub notifier ===");
        BookingSystem system = new BookingSystem(List.of(show("6pm")), Clock.systemUTC(), prices(),
                List.of(new WaitlistNotifier()));
        String b = system.holdSeats("alice", "6pm", List.of("P1", "A1"));
        expect(system.getBooking(b).getAmount(), 65_000L, "alice's total");
        system.cancelBooking(b);
        expect(ids(system.getAvailableSeats("6pm")), List.of("A1", "A2", "A3", "P1"), "all free again");
    }

    // ---- helpers ----

    static class RecordingListener implements SeatReleaseListener {
        final List<String> events = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void onSeatsReleased(String showId, List<String> seatIds) { events.add(showId + " " + seatIds); }
    }

    static class FailingListener implements SeatReleaseListener {
        @Override
        public void onSeatsReleased(String showId, List<String> seatIds) { throw new IllegalStateException("push provider down"); }
    }

    static SeatTypePricing prices() {
        return new SeatTypePricing(Map.of(SeatType.REGULAR, 25_000L, SeatType.PREMIUM, 40_000L));
    }

    static TimeSlot slot(int startHour, int endHour) {
        Instant day = Instant.parse("2026-11-09T00:00:00Z");
        return new TimeSlot(day.plus(Duration.ofHours(startHour)), day.plus(Duration.ofHours(endHour)));
    }

    // Test clock that only moves when told to.
    static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-09T18:00:00Z");

        void advanceMinutes(long minutes) { now = now.plus(Duration.ofMinutes(minutes)); }

        @Override public Instant instant() { return now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
    }

    interface Attempt { String run(int i); }

    // Starts n threads at once; returns the results of the ones that didn't lose (seat or slot unavailable).
    static List<String> race(int n, Attempt attempt) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        List<String> winners = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < n; i++) {
            int id = i;
            pool.submit(() -> {
                try {
                    start.await();
                    winners.add(attempt.run(id));
                } catch (SeatUnavailableException | SlotUnavailableException expected) {
                    // lost the race
                } catch (Exception e) {
                    winners.add("ERROR " + e);
                }
            });
        }
        start.countDown();
        pool.shutdown();
        check(pool.awaitTermination(10, TimeUnit.SECONDS), "race timed out");
        return winners;
    }

    static Show show(String id) {
        return new Show(id, List.of(new Seat("A1", SeatType.REGULAR), new Seat("A2", SeatType.REGULAR),
                new Seat("A3", SeatType.REGULAR), new Seat("P1", SeatType.PREMIUM)));
    }

    static List<String> ids(List<Seat> seats) {
        List<String> ids = new ArrayList<>();
        for (Seat seat : seats) ids.add(seat.getId());
        return ids;
    }

    static void expect(Object got, Object want, String label) {
        check(got.equals(want), label + ": expected " + want + ", got " + got);
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
