package llmusagelimits;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;

import llmusagelimits.backend.InferenceBackend;
import llmusagelimits.backend.ModelServerBackend;
import llmusagelimits.estimate.FixedEstimator;
import llmusagelimits.estimate.MovingAverageEstimator;
import llmusagelimits.limiter.TooManyConcurrentRequestsException;
import llmusagelimits.limiter.UsageLimitExceededException;
import llmusagelimits.limiter.UsageLimiter;
import llmusagelimits.model.InferenceRequest;
import llmusagelimits.model.InferenceResult;
import llmusagelimits.model.LimitType;
import llmusagelimits.model.Model;
import llmusagelimits.model.WindowSpec;
import llmusagelimits.plan.Plan;

import static llmusagelimits.model.LimitType.*;
import static llmusagelimits.model.Model.*;

// Self-checks: each one throws if the limiter misbehaves.
public class Main {
    static final Instant T0 = Instant.parse("2026-01-05T00:00:00Z");
    static final String HELLO = "hello world!"; // 12 chars = 3 tokens

    public static void main(String[] args) {
        baseTraceStillHolds();
        ext1PlansAndUpgradeMidWindow();
        ext2MovingAverageEstimate();
        ext3OpusOnlyWeeklyLimit();
        ext4InFlightCap();
        badConfigFailsAtStartup();
        demoWithRealBackend();
        System.out.println("\nAll checks passed.");
    }

    static List<WindowSpec> defaultSpecs() {
        return List.of(WindowSpec.allModels(FIVE_HOUR, Duration.ofHours(5), 1000),
                WindowSpec.allModels(WEEKLY, Duration.ofDays(7), 2500));
    }

    // Phase 1's API and trace, plus the parallel-chats trap, through the new structure.
    static void baseTraceStillHolds() {
        System.out.println("=== Base trace (Phase 1 constructor) ===");
        MutableClock clock = new MutableClock(T0);
        ScriptedBackend backend = new ScriptedBackend();
        UsageLimiter limiter = new UsageLimiter(backend, clock, 1000, 2500);

        backend.nextOutput = 97;
        limiter.handle(req("u1", SONNET));
        expect(limiter.getRemaining("u1", FIVE_HOUR), 800, "t=0h SONNET charged 200");
        clock.set(T0.plus(Duration.ofHours(1)));
        backend.nextOutput = 197;
        limiter.handle(req("u1", OPUS));
        expect(limiter.getRemaining("u1", FIVE_HOUR), 0, "t=1h OPUS overshoot shows 0");
        clock.set(T0.plus(Duration.ofHours(2)));
        expectLimit(() -> limiter.handle(req("u1", HAIKU)), FIVE_HOUR, T0.plus(Duration.ofHours(5)), "t=2h HAIKU");

        backend.nextOutput = 97;
        for (int i = 0; i < 3; i++) limiter.handle(req("u2", SONNET)); // u2 used 600
        backend.duringNextCalls.add(() -> limiter.handle(req("u2", SONNET)));
        backend.duringNextCalls.add(() -> expectLimit(() -> limiter.handle(req("u2", SONNET)), FIVE_HOUR,
                T0.plus(Duration.ofHours(7)), "parallel chat C rejected (600 + 412 reserved)"));
        limiter.handle(req("u2", SONNET));
        expect(limiter.getRemaining("u2", FIVE_HOUR), 0, "A + B settled: used 1000");
        expect(limiter.getReserved("u2", FIVE_HOUR), 0, "nothing left reserved");
    }

    static void ext1PlansAndUpgradeMidWindow() {
        System.out.println("\n=== EXT 1: plans, upgrade mid-window ===");
        MutableClock clock = new MutableClock(T0);
        ScriptedBackend backend = new ScriptedBackend();
        UsageLimiter limiter = new UsageLimiter(backend, clock, defaultSpecs(), new FixedEstimator(100), 10);

        backend.nextOutput = 197;
        limiter.handle(req("u1", OPUS));                    // 1000 of 1000 (FREE)
        expectLimit(() -> limiter.handle(req("u1", HAIKU)), FIVE_HOUR, T0.plus(Duration.ofHours(5)), "FREE user is out");

        limiter.setPlan("u1", Plan.PRO);                    // 5000 / 12500, used kept
        expect(limiter.getRemaining("u1", FIVE_HOUR), 4000, "PRO: limit 5000, used 1000 kept (no free refill)");
        limiter.handle(req("u1", OPUS));
        expect(limiter.getRemaining("u1", FIVE_HOUR), 3000, "PRO user admitted in the same window");

        limiter.setPlan("u1", Plan.FREE);                   // downgrade: used 2000 > limit 1000
        expect(limiter.getRemaining("u1", FIVE_HOUR), 0, "downgrade: used > limit shows 0");
        expectLimit(() -> limiter.handle(req("u1", HAIKU)), FIVE_HOUR, T0.plus(Duration.ofHours(5)), "downgraded user blocked until reset");
        expect(limiter.getRemaining("new-user", FIVE_HOUR), 1000, "a user with no plan gets FREE limits");
        expectThrows(() -> limiter.setPlan("u1", null), IllegalArgumentException.class, "null plan");
    }

    static void ext2MovingAverageEstimate() {
        System.out.println("\n=== EXT 2: moving-average estimate ===");
        ScriptedBackend backend = new ScriptedBackend();
        List<WindowSpec> roomy = List.of(WindowSpec.allModels(FIVE_HOUR, Duration.ofHours(5), 100_000),
                WindowSpec.allModels(WEEKLY, Duration.ofDays(7), 100_000));
        UsageLimiter limiter = new UsageLimiter(backend, new MutableClock(T0), roomy, new MovingAverageEstimator(100), 10);
        long[] seen = new long[1];

        backend.duringNextCalls.add(() -> seen[0] = limiter.getReserved("coder", FIVE_HOUR));
        backend.nextOutput = 997;                           // a long code answer
        limiter.handle(req("coder", SONNET));
        expect(seen[0], 206, "first request reserves the seed: (3 + 100) x 2");

        backend.duringNextCalls.add(() -> seen[0] = limiter.getReserved("coder", FIVE_HOUR));
        backend.nextOutput = 97;
        limiter.handle(req("coder", SONNET));
        expect(seen[0], 564, "after a 997-token reply: avg 279 -> (3 + 279) x 2");

        backend.duringNextCalls.add(() -> seen[0] = limiter.getReserved("chatter", FIVE_HOUR));
        limiter.handle(req("chatter", SONNET));
        expect(seen[0], 206, "another user still gets the seed");
    }

    static void ext3OpusOnlyWeeklyLimit() {
        System.out.println("\n=== EXT 3: OPUS-only weekly limit ===");
        MutableClock clock = new MutableClock(T0);
        ScriptedBackend backend = new ScriptedBackend();
        List<WindowSpec> specs = List.of(
                WindowSpec.allModels(FIVE_HOUR, Duration.ofHours(5), 1000),
                WindowSpec.allModels(WEEKLY, Duration.ofDays(7), 2500),
                new WindowSpec(OPUS_WEEKLY, Duration.ofDays(7), 1000, EnumSet.of(OPUS)));
        UsageLimiter limiter = new UsageLimiter(backend, clock, specs, new FixedEstimator(100), 10);

        backend.nextOutput = 197;
        limiter.handle(req("u1", OPUS));                    // OPUS_WEEKLY used 1000
        expect(limiter.getRemaining("u1", OPUS_WEEKLY), 0, "OPUS charged to its own window");

        clock.set(T0.plus(Duration.ofHours(6)));            // 5h window rolled
        expectLimit(() -> limiter.handle(req("u1", OPUS)), OPUS_WEEKLY, T0.plus(Duration.ofDays(7)), "OPUS blocked by OPUS_WEEKLY");

        long[] opusReserved = new long[1];
        backend.duringNextCalls.add(() -> opusReserved[0] = limiter.getReserved("u1", OPUS_WEEKLY));
        backend.nextOutput = 97;
        limiter.handle(req("u1", HAIKU));
        expect(opusReserved[0], 0, "HAIKU reserves nothing in OPUS_WEEKLY");
        expect(limiter.getReserved("u1", OPUS_WEEKLY), 0, "and settle releases nothing from it");
        expect(limiter.getRemaining("u1", WEEKLY), 1400, "HAIKU still counts against WEEKLY");
    }

    static void ext4InFlightCap() {
        System.out.println("\n=== EXT 4: in-flight cap ===");
        ScriptedBackend backend = new ScriptedBackend();
        UsageLimiter limiter = new UsageLimiter(backend, new MutableClock(T0), defaultSpecs(), new FixedEstimator(100), 2);
        backend.nextOutput = 97;
        backend.duringNextCalls.add(() -> limiter.handle(req("u1", HAIKU)));      // chat B inside A
        backend.duringNextCalls.add(() -> expectThrows(() -> limiter.handle(req("u1", HAIKU)),
                TooManyConcurrentRequestsException.class, "chat C: 2 already in flight"));
        limiter.handle(req("u1", HAIKU));                                         // chat A
        limiter.handle(req("u1", HAIKU));
        System.out.println("OK     after A and B finish, a new chat is admitted");

        UsageLimiter tight = new UsageLimiter(backend, new MutableClock(T0), defaultSpecs(), new FixedEstimator(100), 1);
        backend.nextOutput = 197;
        tight.handle(req("u2", OPUS));                                            // out of 5h usage
        for (int i = 0; i < 3; i++) {
            expectLimitQuiet(() -> tight.handle(req("u2", HAIKU)));
        }
        tight.setPlan("u2", Plan.PRO);
        backend.nextOutput = 97;
        tight.handle(req("u2", HAIKU));
        System.out.println("OK     3 limit rejections did not leak the in-flight slot");
    }

    static void badConfigFailsAtStartup() {
        System.out.println("\n=== Bad config fails at startup ===");
        ScriptedBackend b = new ScriptedBackend();
        Clock c = Clock.systemUTC();
        FixedEstimator e = new FixedEstimator(100);
        expectThrows(() -> new UsageLimiter(b, c, List.of(), e, 10), IllegalArgumentException.class, "no windows");
        expectThrows(() -> new UsageLimiter(b, c, List.of(WindowSpec.allModels(WEEKLY, Duration.ofDays(7), 1),
                WindowSpec.allModels(WEEKLY, Duration.ofDays(1), 1)), e, 10), IllegalArgumentException.class, "duplicate window type");
        expectThrows(() -> new UsageLimiter(b, c, defaultSpecs(), null, 10), IllegalArgumentException.class, "no estimator");
        expectThrows(() -> new UsageLimiter(b, c, defaultSpecs(), e, 0), IllegalArgumentException.class, "maxInFlight 0");
        expectThrows(() -> new WindowSpec(OPUS_WEEKLY, Duration.ofDays(7), 10, EnumSet.noneOf(Model.class)),
                IllegalArgumentException.class, "window with no models");
        UsageLimiter twoWindows = new UsageLimiter(b, c, defaultSpecs(), e, 10);
        expectThrows(() -> twoWindows.getRemaining("u1", OPUS_WEEKLY), IllegalArgumentException.class, "unconfigured window read");
    }

    // Production wiring: stub backend, plans, moving-average estimate, OPUS window, cap of 5.
    static void demoWithRealBackend() {
        System.out.println("\n=== Demo with the stub backend ===");
        List<WindowSpec> specs = List.of(
                WindowSpec.allModels(FIVE_HOUR, Duration.ofHours(5), 1000),
                WindowSpec.allModels(WEEKLY, Duration.ofDays(7), 2500),
                new WindowSpec(OPUS_WEEKLY, Duration.ofDays(7), 600, EnumSet.of(OPUS)));
        UsageLimiter limiter = new UsageLimiter(new ModelServerBackend(), Clock.systemUTC(), specs,
                new MovingAverageEstimator(100), 5);
        limiter.setPlan("alice", Plan.MAX);
        limiter.handle(new InferenceRequest("alice", SONNET, List.of("Summarise this paragraph for me please")));
        limiter.handle(new InferenceRequest("alice", OPUS, List.of("Write a haiku")));
        System.out.println("alice remaining: 5h=" + limiter.getRemaining("alice", FIVE_HOUR)
                + " weekly=" + limiter.getRemaining("alice", WEEKLY)
                + " opus=" + limiter.getRemaining("alice", OPUS_WEEKLY));
        check(limiter.getRemaining("alice", OPUS_WEEKLY) < 600 * 20, "alice's OPUS use was charged");
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

    static void expectLimitQuiet(Runnable action) {
        expectThrowsQuiet(action, UsageLimitExceededException.class);
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
