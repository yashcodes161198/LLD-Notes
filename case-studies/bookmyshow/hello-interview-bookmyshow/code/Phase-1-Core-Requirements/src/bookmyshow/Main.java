package bookmyshow;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import bookmyshow.model.BookingStatus;
import bookmyshow.model.Seat;
import bookmyshow.model.SeatType;
import bookmyshow.model.Show;
import bookmyshow.service.BookingNotFoundException;
import bookmyshow.service.BookingSystem;
import bookmyshow.service.HoldExpiredException;
import bookmyshow.service.SeatUnavailableException;
import bookmyshow.service.ShowNotFoundException;

// Self-checks: each one throws if the booking system misbehaves.
public class Main {
    public static void main(String[] args) throws Exception {
        verificationTrace();
        staleCancelDoesNotFreeANewerHold();
        inputValidation();
        concurrentHolds();
        badConfigFailsAtStartup();
        demoWithSystemClock();
        System.out.println("\nAll checks passed.");
    }

    // The VERIFICATION TRACE from bookmyshow.py, line by line.
    static void verificationTrace() {
        System.out.println("=== Verification trace ===");
        MutableClock clock = new MutableClock();
        BookingSystem system = new BookingSystem(List.of(show("S1")), clock);

        expect(ids(system.getAvailableSeats("S1")), List.of("A1", "A2", "A3", "P1"), "all seats free");
        String b1 = system.holdSeats("u1", "S1", List.of("A1", "A2"));
        expect(b1, "B1", "u1 holds A1, A2");
        expect(ids(system.getAvailableSeats("S1")), List.of("A3", "P1"), "A1, A2 held");

        clock.advanceMinutes(1);
        expectThrows(() -> system.holdSeats("u2", "S1", List.of("A2", "A3")), SeatUnavailableException.class,
                "u2 holds A2, A3 -> A2 taken");
        expect(ids(system.getAvailableSeats("S1")), List.of("A3", "P1"), "all-or-nothing: A3 still free");
        expectThrows(() -> system.getBooking("B2"), BookingNotFoundException.class, "failed hold stored nothing");

        clock.advanceMinutes(4); // 0:05
        system.confirmBooking(b1, "pay_1");
        expect(system.getBooking(b1).getStatus(), BookingStatus.CONFIRMED, "B1 confirmed");
        system.confirmBooking(b1, "pay_1");
        expect(system.getBooking(b1).getStatus(), BookingStatus.CONFIRMED, "B1 confirmed again -> no-op");

        String b2 = system.holdSeats("u3", "S1", List.of("A3")); // until 0:15
        clock.advanceMinutes(11); // 0:16
        expectThrows(() -> system.confirmBooking(b2, "pay_2"), HoldExpiredException.class, "B2 paid too late");
        expect(ids(system.getAvailableSeats("S1")), List.of("A3", "P1"), "A3 free again after expiry");
        clock.advanceMinutes(20); // long after: confirmed seats never expire
        expect(ids(system.getAvailableSeats("S1")), List.of("A3", "P1"), "confirmed B1 still holds A1, A2");

        String b3 = system.holdSeats("u4", "S1", List.of("A3"));
        system.cancelBooking(b2);
        expect(system.getBooking(b2).getStatus(), BookingStatus.CANCELLED, "late cancel of expired B2");
        expect(ids(system.getAvailableSeats("S1")), List.of("P1"), "A3 still held by " + b3);

        system.cancelBooking(b1);
        system.cancelBooking(b1);
        expect(ids(system.getAvailableSeats("S1")), List.of("A1", "A2", "P1"), "cancel B1 (twice) frees A1, A2");
        expectThrows(() -> system.confirmBooking(b1, "pay_1"), IllegalStateException.class, "confirm a cancelled booking");
        expectThrows(() -> system.confirmBooking("B9", "x"), BookingNotFoundException.class, "unknown booking");
    }

    // TRICKY #4: workbook 03, Q3. A's late cancel must not free the seat B now holds.
    static void staleCancelDoesNotFreeANewerHold() {
        System.out.println("\n=== The trap: stale cancel after the seat was re-held ===");
        MutableClock clock = new MutableClock();
        BookingSystem system = new BookingSystem(List.of(show("S1")), clock);

        String b1 = system.holdSeats("u1", "S1", List.of("A1"));
        clock.advanceMinutes(11);
        String b2 = system.holdSeats("u2", "S1", List.of("A1")); // B1 expired, so B2 may take A1
        clock.advanceMinutes(1);
        system.cancelBooking(b1);
        clock.advanceMinutes(1);
        expectThrows(() -> system.holdSeats("u3", "S1", List.of("A1")), SeatUnavailableException.class,
                "u3 cannot take A1: B2 still holds it");
        system.confirmBooking(b2, "p2");
        expect(system.getBooking(b2).getStatus(), BookingStatus.CONFIRMED, "B2 confirms, A1 sold exactly once");
    }

    static void inputValidation() {
        System.out.println("\n=== Input validation ===");
        BookingSystem system = new BookingSystem(List.of(show("S1")), new MutableClock());
        expectThrows(() -> system.holdSeats("u1", "S1", List.of()), IllegalArgumentException.class, "empty seat list");
        expectThrows(() -> system.holdSeats("u1", "S1", List.of("A1", "A1")), IllegalArgumentException.class, "duplicate seat");
        expectThrows(() -> system.holdSeats("", "S1", List.of("A1")), IllegalArgumentException.class, "empty user");
        expectThrows(() -> system.holdSeats("u1", "S1", List.of("A1", "Z9")), IllegalArgumentException.class, "unknown seat");
        expect(ids(system.getAvailableSeats("S1")), List.of("A1", "A2", "A3", "P1"), "invalid seat held nothing");
        expectThrows(() -> system.holdSeats("u1", "S9", List.of("A1")), ShowNotFoundException.class, "unknown show");
        expectThrows(() -> system.getAvailableSeats("S9"), ShowNotFoundException.class, "seats of unknown show");
        String b1 = system.holdSeats("u1", "S1", List.of("A1"));
        expectThrows(() -> system.confirmBooking(b1, " "), IllegalArgumentException.class, "blank payment ref");
        expectThrows(() -> system.cancelBooking(null), BookingNotFoundException.class, "null booking id");
    }

    // Req 2: "exactly one wins", 200 rounds of 20 threads, plus overlapping multi-seat holds.
    static void concurrentHolds() throws InterruptedException {
        System.out.println("\n=== Concurrency ===");
        for (int round = 0; round < 200; round++) {
            BookingSystem system = new BookingSystem(List.of(show("S1")), new MutableClock());
            List<String> winners = race(20, i -> system.holdSeats("u" + i, "S1", List.of("P1")));
            check(winners.size() == 1, "round " + round + ": expected 1 winner for P1, got " + winners);
        }
        System.out.println("OK     200 rounds x 20 threads on P1 -> exactly 1 winner each");

        for (int round = 0; round < 200; round++) {
            BookingSystem system = new BookingSystem(List.of(show("S1")), new MutableClock());
            List<String> winners = race(20, i -> system.holdSeats("u" + i, "S1",
                    i % 2 == 0 ? List.of("A1", "A2") : List.of("A2", "A3")));
            check(winners.size() == 1, "round " + round + ": overlapping holds, expected 1 winner, got " + winners);
            List<String> free = ids(system.getAvailableSeats("S1"));
            check(free.equals(List.of("A3", "P1")) || free.equals(List.of("A1", "P1")),
                    "round " + round + ": the loser left a partial hold: free=" + free);
        }
        System.out.println("OK     200 rounds of [A1,A2] vs [A2,A3] -> 1 winner, no partial holds");
    }

    static void badConfigFailsAtStartup() {
        System.out.println("\n=== Bad config fails at startup ===");
        expectThrows(() -> new BookingSystem(List.of(show("S1"), show("S1")), new MutableClock()),
                IllegalArgumentException.class, "duplicate show id");
        expectThrows(() -> new Show("S2", List.of(new Seat("A1", SeatType.REGULAR), new Seat("A1", SeatType.PREMIUM))),
                IllegalArgumentException.class, "duplicate seat id in a show");
        expectThrows(() -> new Show("S3", List.of()), IllegalArgumentException.class, "show with no seats");
        expectThrows(() -> new BookingSystem(List.of(show("S1")), null), IllegalArgumentException.class, "no clock");
    }

    // Production wiring: the real clock, two shows of the same screen layout.
    static void demoWithSystemClock() {
        System.out.println("\n=== Demo with the system clock ===");
        BookingSystem system = new BookingSystem(List.of(show("6pm"), show("9pm")), Clock.systemUTC());
        String b = system.holdSeats("alice", "6pm", List.of("P1"));
        system.confirmBooking(b, "pay_alice");
        expect(ids(system.getAvailableSeats("6pm")), List.of("A1", "A2", "A3"), "6pm: P1 booked");
        expect(ids(system.getAvailableSeats("9pm")), List.of("A1", "A2", "A3", "P1"), "9pm: same seat P1 still free");
    }

    // ---- helpers ----

    // Test clock that only moves when told to.
    static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-09T18:00:00Z");

        void advanceMinutes(long minutes) { now = now.plus(Duration.ofMinutes(minutes)); }

        @Override public Instant instant() { return now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
    }

    interface Attempt { String run(int i); }

    // Starts n threads at once; returns the results of the ones that didn't throw SeatUnavailableException.
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
                } catch (SeatUnavailableException expected) {
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
