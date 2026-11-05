package ratelimiter;

import ratelimiter.enums.UserTier;
import ratelimiter.model.RateLimitConfig;
import ratelimiter.model.User;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        User freeUser = new User("user1", UserTier.FREE);       // 10 req in 60 sec
        User premiumUser = new User("user2", UserTier.PREMIUM); // 100 req in 60 sec

        // Every rate limit is two numbers: how many requests, in how long a window.
        // The limit belongs to the tier, not to the user.
        Map<UserTier, RateLimitConfig> limits = new LinkedHashMap<>();
        limits.put(UserTier.FREE, new RateLimitConfig(10, 60));
        limits.put(UserTier.PREMIUM, new RateLimitConfig(100, 60));

        System.out.println("=== Limit per tier ===");
        for (Map.Entry<UserTier, RateLimitConfig> entry : limits.entrySet()) {
            System.out.println(entry.getKey() + ": " + describe(entry.getValue()));
        }

        System.out.println("\n=== Which limit applies to each user? ===");
        for (User user : List.of(freeUser, premiumUser)) {
            RateLimitConfig limit = limits.get(user.getTier());
            System.out.println(user.getUserId() + " (" + user.getTier() + ") -> " + describe(limit));
        }

        // The LeetCode example from the video: 6 runs per 24-hour day.
        RateLimitConfig leetCode = new RateLimitConfig(6, 24 * 60 * 60);
        System.out.println("\n=== The video's LeetCode example ===");
        System.out.println("6 per day: " + describe(leetCode) + " = one every "
                + leetCode.getWindowInSeconds() / leetCode.getMaxRequests() / 3600 + " hours");
    }

    // "10 requests / 60 s (one every 6.0 s on average)"
    private static String describe(RateLimitConfig config) {
        double gap = (double) config.getWindowInSeconds() / config.getMaxRequests();
        return config.getMaxRequests() + " requests / " + config.getWindowInSeconds() + " s"
                + String.format(" (one every %.1f s on average)", gap);
    }
}
