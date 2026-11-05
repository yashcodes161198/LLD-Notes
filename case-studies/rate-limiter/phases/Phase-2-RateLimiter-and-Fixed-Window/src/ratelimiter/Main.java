package ratelimiter;

import ratelimiter.enums.UserTier;
import ratelimiter.limiter.FixedWindowRateLimiter;
import ratelimiter.limiter.RateLimiter;
import ratelimiter.model.RateLimitConfig;
import ratelimiter.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        User freeUser = new User("user1", UserTier.FREE);
        User otherFreeUser = new User("user3", UserTier.FREE);

        RateLimiter limiter = new FixedWindowRateLimiter(new RateLimitConfig(10, 60)); // 10 req/min

        System.out.println("=== FixedWindowRateLimiter, 10 requests per 60 s ===");
        System.out.println("user1 sends 15: " + fire(limiter, freeUser, 15));
        System.out.println("user3 sends 3:  " + fire(limiter, otherFreeUser, 3) + "   (own counter)");

        // To see a window reset without waiting a minute, use 2 requests per 1-second window.
        RateLimiter perSecond = new FixedWindowRateLimiter(new RateLimitConfig(2, 1));

        System.out.println("\n=== FixedWindowRateLimiter, 2 requests per 1 s ===");
        System.out.println("user1 sends 3:  " + fire(perSecond, freeUser, 3));
        Thread.sleep(1100); // now we are in a later 1-second window
        boolean fourth = perSecond.allowRequest(freeUser.getUserId());
        System.out.println("1.1 s later, request 4: " + (fourth ? "ALLOWED" : "BLOCKED")
                + "   <- a new window has started. Expected ALLOWED: spot the bug!");
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
