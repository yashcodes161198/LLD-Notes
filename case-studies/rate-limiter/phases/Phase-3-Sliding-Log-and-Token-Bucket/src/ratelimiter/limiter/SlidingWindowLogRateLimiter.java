package ratelimiter.limiter;

import ratelimiter.enums.RateLimitType;
import ratelimiter.model.RateLimitConfig;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

// Single-threaded version. Phase 5 makes it thread-safe.
public class SlidingWindowLogRateLimiter extends RateLimiter {
    private final Map<String, Queue<Long>> requestLog = new HashMap<>();

    public SlidingWindowLogRateLimiter(RateLimitConfig config) {
        super(config, RateLimitType.SLIDING_WINDOW_LOG);
    }

    @Override
    public boolean allowRequest(String userId) {
        long now = System.currentTimeMillis() / 1000;

        Queue<Long> log = requestLog.computeIfAbsent(userId, id -> new ArrayDeque<>());

        // slide the window: drop timestamps that are a full window old
        while (!log.isEmpty() && (now - log.peek()) >= config.getWindowInSeconds()) {
            log.poll();
        }

        if (log.size() < config.getMaxRequests()) {
            log.add(now); // record this request
            return true;
        }

        return false;
    }
}
