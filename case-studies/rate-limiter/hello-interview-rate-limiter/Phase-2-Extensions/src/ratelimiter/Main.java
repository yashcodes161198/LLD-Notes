package ratelimiter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// Self-checks: each one throws if the limiter misbehaves.
public class Main {
    static final Limit PREMIUM = Limit.tokenBucket(100, 10);

    public static void main(String[] args) throws Exception {
        verificationTraceWithRetryAfter();
        frequentCallerIsNotStarved();
        ext1SlidingWindowLog();
        ext2PerEndpointLimits();
        badConfigFailsAtStartup();
        racesAllowExactlyTheLimit(Limit.tokenBucket(10, 1), 500);
        racesAllowExactlyTheLimit(Limit.slidingWindow(10, 10), 500);
        System.out.println("\nAll checks passed.");
    }

    // Phase 1's trace, now also checking EXT 3's retry-after. FREE = 2 tokens, 1 per second.
    static void verificationTraceWithRetryAfter() {
        System.out.println("=== Verification trace + Retry-After (FREE = token bucket, capacity 2, 1/s) ===");
        ManualClock clock = new ManualClock();
        RateLimiter limiter = new RateLimiter(Map.of(Tier.FREE, Limit.tokenBucket(2, 1), Tier.PREMIUM, PREMIUM), clock);

        expect(limiter.allowRequest("a", Tier.FREE), true, 0, "t=0.0  a  new bucket 2.0 -> 1.0");
        expect(limiter.allowRequest("a", Tier.FREE), true, 0, "t=0.0  a  1.0 -> 0.0");
        expect(limiter.allowRequest("a", Tier.FREE), false, 1.0, "t=0.0  a  0.0 -> denied, one token is 1 s away");
        expect(limiter.allowRequest("b", Tier.FREE), true, 0, "t=0.0  b  its own new bucket");
        clock.advanceSeconds(0.5);
        expect(limiter.allowRequest("a", Tier.FREE), false, 0.5, "t=0.5  a  0.5 -> denied, 0.5 s to go");
        clock.advanceSeconds(0.5);
        expect(limiter.allowRequest("a", Tier.FREE), true, 0, "t=1.0  a  1.0 -> allowed");
        clock.advanceSeconds(59);
        expect(limiter.allowRequest("a", Tier.FREE), true, 0, "t=60   a  capped at 2.0 -> 1.0");
        expect(limiter.allowRequest("a", Tier.FREE), true, 0, "t=60   a  1.0 -> 0.0");
        expect(limiter.allowRequest("a", Tier.FREE), false, 1.0, "t=60   a  denied: the idle minute was not banked");
        expectThrows(() -> limiter.allowRequest("", Tier.FREE), "empty client id");
    }

    static void frequentCallerIsNotStarved() {
        System.out.println("\n=== Polling every 0.5 s on 1 per second ===");
        ManualClock clock = new ManualClock();
        RateLimiter limiter = new RateLimiter(Map.of(Tier.FREE, Limit.tokenBucket(2, 1), Tier.PREMIUM, PREMIUM), clock);
        limiter.allowRequest("poller", Tier.FREE);
        limiter.allowRequest("poller", Tier.FREE); // bucket now empty

        int allowed = 0;
        for (int i = 0; i < 20; i++) {
            clock.advanceSeconds(0.5);
            if (limiter.allowRequest("poller", Tier.FREE).isAllowed()) allowed++;
        }
        check(allowed == 10, "expected 10 of 20 polls allowed, got " + allowed);
        System.out.println("20 polls over 10 s -> " + allowed + " allowed");
    }

    // EXT 1: FREE switched to a sliding window of 2 requests per 1 s. RateLimiter's code didn't change.
    static void ext1SlidingWindowLog() {
        System.out.println("\n=== EXT 1: FREE = sliding window log, 2 per 1 s ===");
        ManualClock clock = new ManualClock();
        RateLimiter limiter = new RateLimiter(Map.of(Tier.FREE, Limit.slidingWindow(2, 1), Tier.PREMIUM, PREMIUM), clock);

        expect(limiter.allowRequest("a", Tier.FREE), true, 0, "t=0.0  1st in window");
        expect(limiter.allowRequest("a", Tier.FREE), true, 0, "t=0.0  2nd in window");
        expect(limiter.allowRequest("a", Tier.FREE), false, 1.0, "t=0.0  3rd -> denied, oldest leaves at t=1.0");
        clock.advanceSeconds(0.5);
        expect(limiter.allowRequest("a", Tier.FREE), false, 0.5, "t=0.5  still 2 in the last second");
        clock.advanceSeconds(0.5);
        expect(limiter.allowRequest("a", Tier.FREE), true, 0, "t=1.0  both t=0 requests have left");
        expect(limiter.allowRequest("a", Tier.FREE), true, 0, "t=1.0  2nd in the new window");
        expect(limiter.allowRequest("a", Tier.FREE), false, 1.0, "t=1.0  3rd -> denied");
        expect(limiter.allowRequest("p", Tier.PREMIUM), true, 0, "t=1.0  a PREMIUM client still gets a token bucket");
    }

    // EXT 2: /login is limited much harder than everything else.
    static void ext2PerEndpointLimits() {
        System.out.println("\n=== EXT 2: per-endpoint limits (FREE /login = 1 token, 1 per 10 s) ===");
        ManualClock clock = new ManualClock();
        RateLimiter limiter = new RateLimiter(
                Map.of(Tier.FREE, Limit.tokenBucket(2, 1), Tier.PREMIUM, PREMIUM),
                Map.of("/login", Map.of(Tier.FREE, Limit.tokenBucket(1, 0.1))),
                clock);

        expect(limiter.allowRequest("a", Tier.FREE, "/login"), true, 0, "/login   override bucket 1 -> 0");
        expect(limiter.allowRequest("a", Tier.FREE, "/login"), false, 10.0, "/login   denied, retry in 10 s");
        expect(limiter.allowRequest("a", Tier.FREE, "/search"), true, 0, "/search  default budget untouched by /login");
        expect(limiter.allowRequest("a", Tier.FREE, "/orders"), true, 0, "/orders  shares the default budget with /search");
        expect(limiter.allowRequest("a", Tier.FREE, "/search"), false, 1.0, "/search  default budget used up");
        expect(limiter.allowRequest("b", Tier.FREE, "/login"), true, 0, "/login   client b has its own bucket");
        expect(limiter.allowRequest("p", Tier.PREMIUM, "/login"), true, 0, "/login   no PREMIUM override -> PREMIUM default");
        expectThrows(() -> limiter.allowRequest("a", Tier.FREE, ""), "empty endpoint");
    }

    static void badConfigFailsAtStartup() {
        System.out.println("\n=== Bad config fails at startup ===");
        expectThrows(() -> new RateLimiter(Map.of(Tier.FREE, Limit.tokenBucket(10, 1)), new ManualClock()), "PREMIUM has no limit");
        expectThrows(() -> Limit.tokenBucket(0, 1), "capacity 0");
        expectThrows(() -> Limit.slidingWindow(10, 0), "window 0 s");
        expectThrows(() -> new Limit(null, 10, 1), "no algorithm");
    }

    // Req 5 for both algorithms: 20 threads hit the same client at the same instant; the limit is 10.
    static void racesAllowExactlyTheLimit(Limit free, int trials) throws Exception {
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int t = 0; t < trials; t++) {
                RateLimiter limiter = new RateLimiter(Map.of(Tier.FREE, free, Tier.PREMIUM, PREMIUM), new ManualClock());
                CyclicBarrier startTogether = new CyclicBarrier(threads);
                List<Future<Decision>> answers = new ArrayList<>();
                for (int i = 0; i < threads; i++) {
                    answers.add(pool.submit(() -> {
                        startTogether.await();
                        return limiter.allowRequest("user1", Tier.FREE);
                    }));
                }
                int allowed = 0;
                for (Future<Decision> answer : answers) {
                    if (answer.get().isAllowed()) allowed++;
                }
                check(allowed == 10, free.getAlgorithm() + " race " + t + " allowed " + allowed + " of 20");
            }
        } finally {
            pool.shutdown();
        }
        if (free.getAlgorithm() == Algorithm.TOKEN_BUCKET) System.out.println();
        System.out.printf("%-18s %d/%d races (20 threads, limit 10) allowed exactly 10%n", free.getAlgorithm(), trials, trials);
    }

    // ---- helpers ----

    static void expect(Decision got, boolean wantAllowed, double wantRetryAfter, String label) {
        check(got.isAllowed() == wantAllowed && Math.abs(got.getRetryAfterSeconds() - wantRetryAfter) < 1e-9,
                label + ": expected " + wantAllowed + "/" + wantRetryAfter + ", got " + got);
        String verdict = got.isAllowed() ? "TRUE" : String.format("FALSE retry %.1fs", got.getRetryAfterSeconds());
        System.out.printf("%-16s %s%n", verdict, label);
    }

    static void expectThrows(Runnable action, String label) {
        try {
            action.run();
        } catch (IllegalArgumentException | NullPointerException e) {
            System.out.println("THROWS " + label + " -> " + e.getMessage());
            return;
        }
        throw new IllegalStateException(label + ": expected an exception");
    }

    static void check(boolean ok, String message) {
        if (!ok) throw new IllegalStateException("CHECK FAILED: " + message);
    }
}
