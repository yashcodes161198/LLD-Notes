package ratelimiter.limiter;

import ratelimiter.enums.RateLimitType;
import ratelimiter.model.RateLimitConfig;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class FixedWindowRateLimiter extends RateLimiter {
    private final Map<String, Integer> requestCount = new ConcurrentHashMap<>();
    private final Map<String, Long> windowStart = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(RateLimitConfig config) {
        this(config, Clock.systemUTC());
    }

    public FixedWindowRateLimiter(RateLimitConfig config, Clock clock) {
        super(config, RateLimitType.FIXED_WINDOW, clock);
    }

    @Override
    public boolean allowRequest(String userId) {
        AtomicBoolean allowed = new AtomicBoolean(false);

        long currentReqWindow = clock.millis() / 1000 / config.getWindowInSeconds();

        requestCount.compute(userId, (id, count) -> {
            long lastReqWindow = windowStart.getOrDefault(id, -1L); // -1 = no request yet

            if (lastReqWindow != currentReqWindow) {
                // first request, or window expired -> reset counter and window of last req
                windowStart.put(id, currentReqWindow);
                allowed.set(true);
                return 1; // first request in new window
            }

            if (count < config.getMaxRequests()) {
                allowed.set(true);
                return count + 1; // increment count
            }

            return count; // remain same
        });

        return allowed.get();
    }
}
