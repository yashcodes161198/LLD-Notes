package ratelimiter.limiter;

import ratelimiter.enums.RateLimitType;
import ratelimiter.model.RateLimitConfig;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

// Estimates a sliding window from two fixed-window counters:
// estimate = previous window's count * (share of it still inside the sliding window) + current window's count
public class SlidingWindowCounterRateLimiter extends RateLimiter {
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public SlidingWindowCounterRateLimiter(RateLimitConfig config) {
        this(config, Clock.systemUTC());
    }

    public SlidingWindowCounterRateLimiter(RateLimitConfig config, Clock clock) {
        super(config, RateLimitType.SLIDING_WINDOW_COUNTER, clock);
    }

    @Override
    public boolean allowRequest(String userId) {
        AtomicBoolean allowed = new AtomicBoolean(false);
        long windowMillis = config.getWindowInSeconds() * 1000L;
        long now = clock.millis();
        long currentWindow = now / windowMillis;
        double elapsedShare = (double) (now % windowMillis) / windowMillis; // how far into the current window

        counters.compute(userId, (id, counter) -> {
            if (counter == null) counter = new Counter(currentWindow);

            if (counter.window != currentWindow) {
                // slide: current becomes previous (or 0 if a whole window passed with no requests)
                counter.previousCount = (counter.window == currentWindow - 1) ? counter.currentCount : 0;
                counter.currentCount = 0;
                counter.window = currentWindow;
            }

            double estimate = counter.previousCount * (1 - elapsedShare) + counter.currentCount;
            if (estimate < config.getMaxRequests()) {
                counter.currentCount++;
                allowed.set(true);
            }

            return counter;
        });

        return allowed.get();
    }

    // Only ever read or changed inside compute(), which holds this user's lock.
    private static class Counter {
        long window;
        int currentCount;
        int previousCount;

        Counter(long window) {
            this.window = window;
        }
    }
}
