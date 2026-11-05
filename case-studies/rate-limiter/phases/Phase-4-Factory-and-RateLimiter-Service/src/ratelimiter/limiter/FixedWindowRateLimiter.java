package ratelimiter.limiter;

import ratelimiter.enums.RateLimitType;
import ratelimiter.model.RateLimitConfig;

import java.util.HashMap;
import java.util.Map;

// Single-threaded version. Phase 5 makes it thread-safe.
public class FixedWindowRateLimiter extends RateLimiter {
    private final Map<String, Integer> requestCount = new HashMap<>();
    private final Map<String, Long> windowStart = new HashMap<>();

    public FixedWindowRateLimiter(RateLimitConfig config) {
        super(config, RateLimitType.FIXED_WINDOW);
    }

    @Override
    public boolean allowRequest(String userId) {
        long currentReqWindow = System.currentTimeMillis() / 1000 / config.getWindowInSeconds();

        long lastReqWindow = windowStart.getOrDefault(userId, currentReqWindow);

        if (lastReqWindow != currentReqWindow) {
            // window expired -> reset counter and window of last req
            windowStart.put(userId, currentReqWindow);
            requestCount.put(userId, 1); // first request in new window
            return true;
        }

        int count = requestCount.getOrDefault(userId, 0);

        if (count < config.getMaxRequests()) {
            requestCount.put(userId, count + 1); // increment count
            return true;
        }

        return false; // count remains same
    }
}
