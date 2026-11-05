package ratelimiter;

import ratelimiter.clock.ManualClock;
import ratelimiter.enums.RateLimitType;
import ratelimiter.enums.UserTier;
import ratelimiter.factory.RateLimiterFactory;
import ratelimiter.limiter.RateLimiter;
import ratelimiter.model.RateLimitConfig;
import ratelimiter.model.User;
import ratelimiter.service.RateLimiterService;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {
    private static final RateLimitType[] ALGORITHMS = {
            RateLimitType.FIXED_WINDOW, RateLimitType.SLIDING_WINDOW_LOG,
            RateLimitType.TOKEN_BUCKET, RateLimitType.SLIDING_WINDOW_COUNTER};

    public static void main(String[] args) throws Exception {
        videoDemoOnFrozenClock();
        fixedWindowResets();
        tokenBucketKeepsPartialRefill();
        sameTrafficFourAlgorithms();
        raceEveryAlgorithm();
        rejectBadInput();
    }

    // 1. The video's demo again. Time never moves, so no window can end in the middle of it.
    static void videoDemoOnFrozenClock() {
        RateLimiterService service = new RateLimiterService(new ManualClock(0));
        System.out.println("=== The video's demo on a frozen clock ===");
        System.out.println("free user,     15 requests: " + fire(service, new User("user1", UserTier.FREE), 15));
        System.out.println("premium user, 120 requests: " + fire(service, new User("user2", UserTier.PREMIUM), 120));
    }

    // 2. Fix 1: a new fixed window resets the count, including for a user's very first window.
    static void fixedWindowResets() {
        ManualClock clock = new ManualClock(0);
        RateLimiter limiter = RateLimiterFactory.createRateLimiter(
                RateLimitType.FIXED_WINDOW, new RateLimitConfig(10, 60), clock);

        System.out.println("\n=== Fix 1: the fixed window resets (10 per 60 s) ===");
        System.out.println("t=0   12 requests: " + fire(limiter, "user1", 12));
        clock.advanceSeconds(60);
        System.out.println("t=60   1 request:  " + fire(limiter, "user1", 1) + "   (Phase 5 code: BLOCKED forever)");
    }

    // 3. Fix 2: the token bucket keeps partial progress towards the next token.
    static void tokenBucketKeepsPartialRefill() {
        ManualClock clock = new ManualClock(0);
        RateLimiter limiter = RateLimiterFactory.createRateLimiter(
                RateLimitType.TOKEN_BUCKET, new RateLimitConfig(10, 60), clock); // one token every 6 s

        System.out.println("\n=== Fix 2: token bucket refill keeps partial progress (1 token / 6 s) ===");
        System.out.println("t=0   10 requests: " + fire(limiter, "user1", 10) + "   (bucket now empty)");
        clock.advanceSeconds(11);
        System.out.println("t=11   1 request:  " + fire(limiter, "user1", 1) + "   (token earned at t=6)");
        clock.advanceSeconds(1);
        System.out.println("t=12   1 request:  " + fire(limiter, "user1", 1) + "   (token earned at t=12; Phase 5 code: BLOCKED)");
    }

    // 4. The same bursts through all four algorithms: 10 requests at t=50, 60, 90 and 120.
    static void sameTrafficFourAlgorithms() {
        ManualClock clock = new ManualClock(0);
        List<RateLimiter> limiters = new ArrayList<>();
        for (RateLimitType algo : ALGORITHMS) {
            limiters.add(RateLimiterFactory.createRateLimiter(algo, new RateLimitConfig(10, 60), clock));
        }

        System.out.println("\n=== Same traffic, four algorithms (10 per 60 s): allowed per burst of 10 ===");
        System.out.printf("%-6s %-13s %-19s %-13s %s%n", "time", ALGORITHMS[0], ALGORITHMS[1], ALGORITHMS[2], ALGORITHMS[3]);
        int[] totals = new int[ALGORITHMS.length];
        long now = 0;
        for (long t : new long[]{50, 60, 90, 120}) {
            clock.advanceSeconds(t - now);
            now = t;
            int[] allowed = new int[ALGORITHMS.length];
            for (int a = 0; a < ALGORITHMS.length; a++) {
                for (int i = 0; i < 10; i++) {
                    if (limiters.get(a).allowRequest("user1")) allowed[a]++;
                }
                totals[a] += allowed[a];
            }
            System.out.printf("%-6s %-13d %-19d %-13d %d%n", "t=" + t, allowed[0], allowed[1], allowed[2], allowed[3]);
        }
        System.out.printf("%-6s %-13d %-19d %-13d %d%n", "total", totals[0], totals[1], totals[2], totals[3]);
    }

    // 5. The race from Phase 5, now including the new algorithm. The clock is frozen, so only threads vary.
    static void raceEveryAlgorithm() throws Exception {
        System.out.println("\n=== 500 races per algorithm (20 threads, limit 10) ===");
        ExecutorService executor = Executors.newFixedThreadPool(20);
        for (RateLimitType algo : ALGORITHMS) {
            int exact = 0;
            for (int t = 0; t < 500; t++) {
                RateLimiter limiter = RateLimiterFactory.createRateLimiter(
                        algo, new RateLimitConfig(10, 60), new ManualClock(0));
                CyclicBarrier barrier = new CyclicBarrier(20);
                List<Future<Boolean>> answers = new ArrayList<>();
                for (int i = 0; i < 20; i++) {
                    answers.add(executor.submit(() -> {
                        barrier.await();
                        return limiter.allowRequest("user1");
                    }));
                }
                int allowed = 0;
                for (Future<Boolean> answer : answers) {
                    if (answer.get()) allowed++;
                }
                if (allowed == 10) exact++;
            }
            System.out.printf("%-23s %d/500 races allowed exactly 10 of 20%n", algo, exact);
        }
        executor.shutdown();
    }

    // 6. Bad input fails fast with a message instead of dividing by zero later.
    static void rejectBadInput() {
        System.out.println("\n=== Bad input is rejected ===");
        attempt("createRateLimiter(LEAKY_BUCKET, ...)",
                () -> RateLimiterFactory.createRateLimiter(RateLimitType.LEAKY_BUCKET, new RateLimitConfig(10, 60)));
        attempt("new RateLimitConfig(0, 60)", () -> new RateLimitConfig(0, 60));
        attempt("new RateLimitConfig(10, 0)", () -> new RateLimitConfig(10, 0));
    }

    static void attempt(String what, Runnable action) {
        try {
            action.run();
            System.out.println(what + " -> accepted");
        } catch (IllegalArgumentException e) {
            System.out.println(what + " -> " + e.getMessage());
        }
    }

    static String fire(RateLimiterService service, User user, int n) {
        List<Boolean> answers = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            answers.add(service.allowRequest(user));
        }
        return summarize(answers);
    }

    static String fire(RateLimiter limiter, String userId, int n) {
        List<Boolean> answers = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            answers.add(limiter.allowRequest(userId));
        }
        return summarize(answers);
    }

    // [true, true, false] -> "1-2 ALLOWED, 3 BLOCKED"
    static String summarize(List<Boolean> answers) {
        StringJoiner out = new StringJoiner(", ");
        int start = 0;
        for (int i = 1; i <= answers.size(); i++) {
            if (i == answers.size() || !answers.get(i).equals(answers.get(start))) {
                String range = (start + 1 == i) ? String.valueOf(i) : (start + 1) + "-" + i;
                out.add(range + (answers.get(start) ? " ALLOWED" : " BLOCKED"));
                start = i;
            }
        }
        return out.toString();
    }
}
