package ratelimiter;

import ratelimiter.enums.RateLimitType;
import ratelimiter.enums.UserTier;
import ratelimiter.factory.RateLimiterFactory;
import ratelimiter.model.RateLimitConfig;
import ratelimiter.model.User;
import ratelimiter.service.RateLimiterService;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class Main {
    public static void main(String[] args) {
        RateLimiterService rateLimiterService = new RateLimiterService();

        User freeUser = new User("user1", UserTier.FREE);       // 10 req in 60 sec
        User premiumUser = new User("user2", UserTier.PREMIUM); // 100 req in 60 sec

        System.out.println("=== Free User Requests ===");
        for (int i = 1; i <= 15; i++) {
            boolean allowed = rateLimiterService.allowRequest(freeUser);
            System.out.println("Request " + i + " for Free User: " + (allowed ? "ALLOWED" : "BLOCKED"));
        }

        System.out.println("\n=== Premium User Requests ===");
        System.out.println("120 requests: " + fire(rateLimiterService, premiumUser, 120));

        // One limiter per tier, but one bucket per user inside it.
        User anotherFreeUser = new User("user3", UserTier.FREE);
        System.out.println("\n=== Another free user shares the limiter, not the bucket ===");
        System.out.println("user3 sends 12: " + fire(rateLimiterService, anotherFreeUser, 12));

        System.out.println("\n=== The factory only builds what exists ===");
        try {
            RateLimiterFactory.createRateLimiter(RateLimitType.LEAKY_BUCKET, new RateLimitConfig(10, 60));
        } catch (IllegalArgumentException e) {
            System.out.println("LEAKY_BUCKET -> " + e.getMessage());
        }
    }

    // Sends n requests back to back and groups the answers: "1-100 ALLOWED, 101-120 BLOCKED".
    static String fire(RateLimiterService service, User user, int n) {
        List<Boolean> answers = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            answers.add(service.allowRequest(user));
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
