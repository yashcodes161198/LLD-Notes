package llmusagelimits;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import llmusagelimits.backend.InferenceBackend;
import llmusagelimits.backend.ModelServerBackend;
import llmusagelimits.limiter.UsageLimitExceededException;
import llmusagelimits.limiter.UsageLimiter;
import llmusagelimits.model.InferenceRequest;
import llmusagelimits.model.InferenceResult;
import llmusagelimits.model.LimitType;
import llmusagelimits.model.Model;

import static llmusagelimits.model.LimitType.FIVE_HOUR;
import static llmusagelimits.model.LimitType.WEEKLY;
import static llmusagelimits.model.Model.*;

// Self-checks: each one throws if the limiter misbehaves.
public class Main {
    static final Instant T0 = Instant.parse("2026-01-05T00:00:00Z");
    static final String HELLO = "hello world!"; // 12 chars = 3 tokens

    public static void main(String[] args) throws Exception {
        verificationTrace();
        parallelChatsCannotOvershoot();
        parallelChatsOnRealThreads();
        failedInferenceChargesNothing();
        resetDuringFlightKeepsReservation();
        inputValidation();
        badConfigFailsAtStartup();
        demoWithRealBackend();
        System.out.println("\nAll checks passed.");
    }

    // The trace from llm_usage_limits.py. Limits: 5h 1000, weekly 2500.
    static void verificationTrace() {
        System.out.println("=== Verification trace ===");
        MutableClock clock = new MutableClock(T0);
        ScriptedBackend backend = new ScriptedBackend();
        UsageLimiter limiter = new UsageLimiter(backend, clock, 1000, 2500);

        backend.nextOutput = 97;   // SONNET: (3 + 97) * 2 = 200
        limiter.handle(req("u1", SONNET));
        expect(limiter.getRemaining("u1", FIVE_HOUR), 800, "t=0h SONNET charged 200 to 5h");
        expect(limiter.getRemaining("u1", WEEKLY), 2300, "t=0h SONNET charged 200 to weekly");
        expect(limiter.getReserved("u1", FIVE_HOUR), 0, "t=0h reservation released");

        clock.set(T0.plus(Duration.ofHours(1)));
        backend.nextOutput = 197;  // OPUS: (3 + 197) * 5 = 1000, admitted because 200 + 0 < 1000
        limiter.handle(req("u1", OPUS));
        expect(limiter.getRemaining("u1", FIVE_HOUR), 0, "t=1h OPUS overshoots to 1200, shows 0 (Req 5)");

        clock.set(T0.plus(Duration.ofHours(2)));
        expectLimit(() -> limiter.handle(req("u1", HAIKU)), FIVE_HOUR, T0.plus(Duration.ofHours(5)), "t=2h HAIKU");

        clock.set(T0.plus(Duration.ofHours(5)));
        backend.nextOutput = 97;   // HAIKU: 100
        limiter.handle(req("u1", HAIKU));
        expect(limiter.getRemaining("u1", FIVE_HOUR), 900, "t=5h 5h window rolled, HAIKU charged 100");
        expect(limiter.getRemaining("u1", WEEKLY), 1200, "t=5h weekly used 1300");

        backend.nextOutput = 197;
        clock.set(T0.plus(Duration.ofHours(10)));
        limiter.handle(req("u1", OPUS));
        clock.set(T0.plus(Duration.ofHours(15)));
        limiter.handle(req("u1", OPUS));
        expect(limiter.getRemaining("u1", WEEKLY), 0, "t=15h weekly used 3300");

        clock.set(T0.plus(Duration.ofHours(16)));
        expectLimit(() -> limiter.handle(req("u1", HAIKU)), WEEKLY, T0.plus(Duration.ofDays(7)),
                "t=16h both full -> reports the LATER reset (weekly)");

        clock.set(T0.plus(Duration.ofDays(7)));
        backend.nextOutput = 97;
        limiter.handle(req("u1", HAIKU));
        expect(limiter.getRemaining("u1", WEEKLY), 2400, "t=7d both windows rolled, admitted");
    }

    // THE TRAP: u2 has used 600 of 1000; chat A is running, chat B starts, then chat C.
    // The backend starts the next chat while the current one is still "generating".
    static void parallelChatsCannotOvershoot() {
        System.out.println("\n=== Parallel chats (the trap) ===");
        MutableClock clock = new MutableClock(T0);
        ScriptedBackend backend = new ScriptedBackend();
        UsageLimiter limiter = new UsageLimiter(backend, clock, 1000, 2500);
        backend.nextOutput = 97;
        for (int i = 0; i < 3; i++) limiter.handle(req("u2", SONNET)); // used 600
        expect(limiter.getRemaining("u2", FIVE_HOUR), 400, "u2 has 400 left");

        List<String> log = new ArrayList<>();
        backend.duringNextCalls.add(() -> {          // runs inside chat A's model call
            log.add("A running, reserved=" + limiter.getReserved("u2", FIVE_HOUR));
            limiter.handle(req("u2", SONNET));        // chat B
        });
        backend.duringNextCalls.add(() -> {          // runs inside chat B's model call
            log.add("B running, reserved=" + limiter.getReserved("u2", FIVE_HOUR));
            expectLimit(() -> limiter.handle(req("u2", SONNET)), FIVE_HOUR, T0.plus(Duration.ofHours(5)),
                    "chat C: 600 used + 412 reserved >= 1000");
        });
        limiter.handle(req("u2", SONNET));            // chat A

        check(log.equals(List.of("A running, reserved=206", "B running, reserved=412")), "reservations seen: " + log);
        check(backend.calls == 5, "exactly A and B reached the model, got " + (backend.calls - 3));
        expect(limiter.getRemaining("u2", FIVE_HOUR), 0, "A + B settled: used 1000");
        expect(limiter.getReserved("u2", FIVE_HOUR), 0, "nothing left reserved");
    }

    // Same trap with 20 real threads released at once: exactly 5 fit (0, 206, 412, 618, 824 < 1000).
    static void parallelChatsOnRealThreads() throws InterruptedException {
        System.out.println("\n=== 20 chats on real threads ===");
        GateBackend backend = new GateBackend();
        UsageLimiter limiter = new UsageLimiter(backend, Clock.fixed(T0, ZoneOffset.UTC), 1000, 2500);
        AtomicInteger rejected = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Thread t = new Thread(() -> {
                try {
                    limiter.handle(req("u3", SONNET));
                } catch (UsageLimitExceededException e) {
                    rejected.incrementAndGet();
                }
            });
            threads.add(t);
            t.start();
        }
        long deadline = System.currentTimeMillis() + 5000;
        while (backend.entered.get() + rejected.get() < 20) { // every chat is either in the model or rejected
            check(System.currentTimeMillis() < deadline, "threads did not all decide in time");
            Thread.sleep(1);
        }
        check(backend.entered.get() == 5, "5 chats run in parallel, got " + backend.entered.get());
        expect(limiter.getReserved("u3", FIVE_HOUR), 5 * 206, "5 estimates held while they run");
        backend.gate.countDown();
        for (Thread t : threads) t.join();
        expect(rejected.get(), 15, "the other 15 were rejected");
        expect(limiter.getRemaining("u3", WEEKLY), 1500, "used exactly 5 x 200 = 1000, no unbounded overshoot");
        expect(limiter.getReserved("u3", FIVE_HOUR), 0, "all reservations released");
    }

    static void failedInferenceChargesNothing() {
        System.out.println("\n=== Backend failure (TRICKY #2) ===");
        ScriptedBackend backend = new ScriptedBackend();
        UsageLimiter limiter = new UsageLimiter(backend, new MutableClock(T0), 1000, 2500);
        backend.fail = true;
        for (int i = 0; i < 10; i++) {
            expectThrowsQuiet(() -> limiter.handle(req("u5", OPUS)), IllegalStateException.class);
        }
        System.out.println("THROWS 10 backend timeouts re-thrown to the caller");
        expect(limiter.getReserved("u5", FIVE_HOUR), 0, "no phantom reservation after 10 failures");
        expect(limiter.getRemaining("u5", FIVE_HOUR), 1000, "charged nothing");
        backend.fail = false;
        backend.nextOutput = 97;
        limiter.handle(req("u5", SONNET));
        expect(limiter.getRemaining("u5", FIVE_HOUR), 800, "next request still admitted");
    }

    // Admitted at 4h59m, settles at 5h01m: the reset must not wipe the in-flight reservation.
    static void resetDuringFlightKeepsReservation() {
        System.out.println("\n=== Window resets mid-request (TRICKY #3) ===");
        MutableClock clock = new MutableClock(T0);
        ScriptedBackend backend = new ScriptedBackend();
        UsageLimiter limiter = new UsageLimiter(backend, clock, 1000, 2500);
        backend.nextOutput = 97;
        limiter.handle(req("u4", SONNET));                       // 5h window: T0 .. T0+5h
        clock.set(T0.plus(Duration.ofMinutes(299)));
        backend.duringNextCalls.add(() -> clock.set(T0.plus(Duration.ofMinutes(301))));
        limiter.handle(req("u4", SONNET));
        expect(limiter.getReserved("u4", FIVE_HOUR), 0, "reserved back to 0, not -206");
        expect(limiter.getRemaining("u4", FIVE_HOUR), 800, "actual charged to the new 5h window");
        expect(limiter.getRemaining("u4", WEEKLY), 2100, "weekly has both charges");
    }

    static void inputValidation() {
        System.out.println("\n=== Input validation ===");
        UsageLimiter limiter = new UsageLimiter(new ScriptedBackend(), new MutableClock(T0), 1000, 2500);
        expectThrows(() -> new InferenceRequest("", SONNET, List.of(HELLO)), IllegalArgumentException.class, "empty user");
        expectThrows(() -> new InferenceRequest("u1", null, List.of(HELLO)), IllegalArgumentException.class, "no model");
        expectThrows(() -> new InferenceRequest("u1", SONNET, List.of()), IllegalArgumentException.class, "no messages");
        expectThrows(() -> limiter.handle(null), IllegalArgumentException.class, "null request");
        expect(limiter.getRemaining("never-seen", WEEKLY), 2500, "unknown user has the full limit");
    }

    static void badConfigFailsAtStartup() {
        System.out.println("\n=== Bad config fails at startup ===");
        expectThrows(() -> new UsageLimiter(null, Clock.systemUTC(), 1000, 2500), IllegalArgumentException.class, "no backend");
        expectThrows(() -> new UsageLimiter(new ScriptedBackend(), Clock.systemUTC(), 0, 2500), IllegalArgumentException.class, "zero limit");
    }

    // Production wiring: the stub backend prints instead of calling a model server.
    static void demoWithRealBackend() {
        System.out.println("\n=== Demo with the stub backend ===");
        UsageLimiter limiter = new UsageLimiter(new ModelServerBackend(), Clock.systemUTC(), 1000, 2500);
        limiter.handle(new InferenceRequest("alice", SONNET, List.of("Summarise this paragraph for me please")));
        limiter.handle(new InferenceRequest("alice", OPUS, List.of("Write a haiku")));
        System.out.println("alice remaining: 5h=" + limiter.getRemaining("alice", FIVE_HOUR)
                + " weekly=" + limiter.getRemaining("alice", WEEKLY));
        check(limiter.getRemaining("alice", FIVE_HOUR) < 1000, "alice was charged");
    }

    // ---- test doubles ----

    // Returns 3 input tokens and nextOutput output tokens; can fail, or run a hook mid-call.
    static class ScriptedBackend implements InferenceBackend {
        long nextOutput = 97;
        boolean fail;
        int calls;
        final Deque<Runnable> duringNextCalls = new ArrayDeque<>();

        @Override
        public InferenceResult generate(Model model, List<String> messages) {
            calls++;
            if (fail) throw new IllegalStateException("backend timeout");
            Runnable during = duringNextCalls.poll();
            if (during != null) during.run();
            return new InferenceResult("ok", 3, nextOutput);
        }
    }

    // Holds every admitted call until the gate opens, so all chats are in flight together.
    static class GateBackend implements InferenceBackend {
        final AtomicInteger entered = new AtomicInteger();
        final CountDownLatch gate = new CountDownLatch(1);

        @Override
        public InferenceResult generate(Model model, List<String> messages) {
            entered.incrementAndGet();
            try {
                gate.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            return new InferenceResult("ok", 3, 97);
        }
    }

    static class MutableClock extends Clock {
        private volatile Instant now;

        MutableClock(Instant start) { this.now = start; }

        void set(Instant instant) { this.now = instant; }

        @Override public Instant instant() { return now; }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    }

    // ---- helpers ----

    static InferenceRequest req(String user, Model model) {
        return new InferenceRequest(user, model, List.of(HELLO));
    }

    static void expect(long got, long want, String label) {
        check(got == want, label + ": expected " + want + ", got " + got);
        System.out.println("OK     " + label + "  (" + got + ")");
    }

    static void expectLimit(Runnable action, LimitType type, Instant resetAt, String label) {
        try {
            action.run();
        } catch (UsageLimitExceededException e) {
            check(e.getLimitType() == type && e.getResetAt().equals(resetAt),
                    label + ": expected " + type + " until " + resetAt + ", got " + e.getMessage());
            System.out.println("THROWS " + label + " -> " + e.getMessage());
            return;
        }
        throw new IllegalStateException("CHECK FAILED: " + label + ": expected UsageLimitExceededException");
    }

    static void expectThrows(Runnable action, Class<? extends RuntimeException> type, String label) {
        expectThrowsQuiet(action, type);
        System.out.println("THROWS " + label);
    }

    static void expectThrowsQuiet(Runnable action, Class<? extends RuntimeException> type) {
        try {
            action.run();
        } catch (RuntimeException e) {
            check(type.isInstance(e), "expected " + type.getSimpleName() + ", got " + e);
            return;
        }
        throw new IllegalStateException("CHECK FAILED: expected " + type.getSimpleName());
    }

    static void check(boolean ok, String message) {
        if (!ok) throw new IllegalStateException("CHECK FAILED: " + message);
    }
}
