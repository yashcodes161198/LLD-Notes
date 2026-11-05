package ratelimiter;

import ratelimiter.enums.UserTier;
import ratelimiter.limiter.FixedWindowRateLimiter;
import ratelimiter.limiter.RateLimiter;
import ratelimiter.limiter.SlidingWindowLogRateLimiter;
import ratelimiter.limiter.TokenBucketRateLimiter;
import ratelimiter.model.RateLimitConfig;
import ratelimiter.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        User freeUser = new User("user1", UserTier.FREE);

        // A small limit, so the differences show up within a few seconds.
        RateLimitConfig config = new RateLimitConfig(3, 3); // 3 requests per 3 s
        List<RateLimiter> limiters = List.of(
                new FixedWindowRateLimiter(config),
                new SlidingWindowLogRateLimiter(config),
                new TokenBucketRateLimiter(config));

        System.out.println("=== Same config (3 requests / 3 s), three algorithms ===");
        round("t = 0.0 s, 5 requests each", limiters, freeUser, 5);
        Thread.sleep(1200);
        round("t = 1.2 s, 2 requests each", limiters, freeUser, 2);
        Thread.sleep(2400);
        round("t = 3.6 s, 3 requests each", limiters, freeUser, 3);
    }

    // The caller only knows the RateLimiter type; each subclass applies its own algorithm.
    static void round(String title, List<RateLimiter> limiters, User user, int n) {
        System.out.println(title);
        for (RateLimiter limiter : limiters) {
            System.out.printf("  %-28s %s%n", limiter.getClass().getSimpleName(), fire(limiter, user, n));
        }
    }

    // Sends n requests back to back and groups the answers: "1-10 ALLOWED, 11-15 BLOCKED".
    static String fire(RateLimiter limiter, User user, int n) {
        List<Boolean> answers = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            answers.add(limiter.allowRequest(user.getUserId()));
        }
        return summarize(answers);
    }

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
