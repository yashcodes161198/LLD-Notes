package ratelimiter.limiter;

import ratelimiter.enums.RateLimitType;
import ratelimiter.model.RateLimitConfig;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class SlidingWindowLogRateLimiter extends RateLimiter {
    private final Map<String, Queue<Long>> requestLog = new ConcurrentHashMap<>();

    public SlidingWindowLogRateLimiter(RateLimitConfig config) {
        this(config, Clock.systemUTC());
    }

    public SlidingWindowLogRateLimiter(RateLimitConfig config, Clock clock) {
        super(config, RateLimitType.SLIDING_WINDOW_LOG, clock);
    }

    @Override
    public boolean allowRequest(String userId) {
        AtomicBoolean allowed = new AtomicBoolean(false);
        long now = clock.millis();
        long windowMillis = config.getWindowInSeconds() * 1000L;

        requestLog.compute(userId, (id, log) -> {
            if (log == null) log = new ArrayDeque<>();

            while (!log.isEmpty() && (now - log.peek()) >= windowMillis) {
                log.poll();
            }

            if (log.size() < config.getMaxRequests()) {
                log.add(now);      // record this request
                allowed.set(true); // mark allowed
            }

            return log;
        });

        return allowed.get();
    }
}
