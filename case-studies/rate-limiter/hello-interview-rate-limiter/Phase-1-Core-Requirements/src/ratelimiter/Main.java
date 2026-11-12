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
    public static void main(String[] args) throws Exception {
        verificationTrace();
        frequentCallerIsNotStarved();
        badConfigFailsAtStartup();
        racesAllowExactlyTheLimit(500);
        System.out.println("\nAll checks passed.");
    }

    // The trace from rate_limiter.py. For readability, FREE = 2 tokens, refilling 1 per second.
    static void verificationTrace() {
        System.out.println("=== Verification trace (FREE = capacity 2, 1 per second) ===");
        ManualClock clock = new ManualClock();
        RateLimiter limiter = newLimiter(new Limit(2, 1), clock);

        expect(limiter.allowRequest("a", Tier.FREE), true,  "t=0.0  a  new bucket 2.0 -> take one -> 1.0");
        expect(limiter.allowRequest("a", Tier.FREE), true,  "t=0.0  a  1.0 -> take one -> 0.0");
        expect(limiter.allowRequest("a", Tier.FREE), false, "t=0.0  a  0.0 < 1 -> denied, nothing consumed");
        expect(limiter.allowRequest("b", Tier.FREE), true,  "t=0.0  b  its own new bucket (Req 4)");
        clock.advanceSeconds(0.5);
        expect(limiter.allowRequest("a", Tier.FREE), false, "t=0.5  a  refill +0.5 -> 0.5 -> denied");
        clock.advanceSeconds(0.5);
        expect(limiter.allowRequest("a", Tier.FREE), true,  "t=1.0  a  refill +0.5 -> 1.0 -> allowed (floor() version: denied forever)");
        clock.advanceSeconds(59);
        expect(limiter.allowRequest("a", Tier.FREE), true,  "t=60   a  refill +59 -> capped at 2.0 -> 1.0");
        expect(limiter.allowRequest("a", Tier.FREE), true,  "t=60   a  1.0 -> 0.0");
        expect(limiter.allowRequest("a", Tier.FREE), false, "t=60   a  denied: the idle minute was not banked");
        expectThrows(() -> limiter.allowRequest("", Tier.FREE), "empty client id");
        expectThrows(() -> limiter.allowRequest(null, Tier.FREE), "null client id");
    }

    // TRICKY #3: a client polling every 0.5 s on a 1-per-second limit must get every second request.
    static void frequentCallerIsNotStarved() {
        System.out.println("\n=== Polling every 0.5 s on 1 per second ===");
        ManualClock clock = new ManualClock();
        RateLimiter limiter = newLimiter(new Limit(2, 1), clock);
        limiter.allowRequest("poller", Tier.FREE);
        limiter.allowRequest("poller", Tier.FREE); // bucket now empty

        int allowed = 0;
        for (int i = 0; i < 20; i++) {
            clock.advanceSeconds(0.5);
            if (limiter.allowRequest("poller", Tier.FREE)) allowed++;
        }
        check(allowed == 10, "expected 10 of 20 polls allowed, got " + allowed);
        System.out.println("20 polls over 10 s -> " + allowed + " allowed");
    }

    static void badConfigFailsAtStartup() {
        System.out.println("\n=== Bad config fails at startup ===");
        expectThrows(() -> new RateLimiter(Map.of(Tier.FREE, new Limit(10, 1)), new ManualClock()), "PREMIUM has no limit");
        expectThrows(() -> new Limit(0, 1), "capacity 0");
        expectThrows(() -> new Limit(10, 0), "refill rate 0");
    }

    // Req 5: 20 threads hit the same client at the same instant; the limit is 10.
    static void racesAllowExactlyTheLimit(int trials) throws Exception {
        System.out.println("\n=== " + trials + " races: 20 threads, same client, capacity 10, frozen clock ===");
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int t = 0; t < trials; t++) {
                RateLimiter limiter = newLimiter(new Limit(10, 1), new ManualClock());
                CyclicBarrier startTogether = new CyclicBarrier(threads);
                List<Future<Boolean>> answers = new ArrayList<>();
                for (int i = 0; i < threads; i++) {
                    answers.add(pool.submit(() -> {
                        startTogether.await();
                        return limiter.allowRequest("user1", Tier.FREE);
                    }));
                }
                int allowed = 0;
                for (Future<Boolean> answer : answers) {
                    if (answer.get()) allowed++;
                }
                check(allowed == 10, "race " + t + " allowed " + allowed + " of 20");
            }
        } finally {
            pool.shutdown();
        }
        System.out.println(trials + "/" + trials + " races allowed exactly 10 of 20");
    }

    // ---- helpers ----

    static RateLimiter newLimiter(Limit free, Clock clock) {
        return new RateLimiter(Map.of(Tier.FREE, free, Tier.PREMIUM, new Limit(100, 10)), clock);
    }

    static void expect(boolean got, boolean want, String label) {
        check(got == want, label + ": expected " + want + ", got " + got);
        System.out.printf("%-6s %s%n", got ? "TRUE" : "FALSE", label);
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
