package ratelimiter.factory;

import ratelimiter.enums.RateLimitType;
import ratelimiter.limiter.FixedWindowRateLimiter;
import ratelimiter.limiter.RateLimiter;
import ratelimiter.limiter.SlidingWindowCounterRateLimiter;
import ratelimiter.limiter.SlidingWindowLogRateLimiter;
import ratelimiter.limiter.TokenBucketRateLimiter;
import ratelimiter.model.RateLimitConfig;

import java.time.Clock;

public class RateLimiterFactory {
    public static RateLimiter createRateLimiter(RateLimitType algo, RateLimitConfig config) {
        return createRateLimiter(algo, config, Clock.systemUTC());
    }

    public static RateLimiter createRateLimiter(RateLimitType algo, RateLimitConfig config, Clock clock) {
        return switch (algo) {
            case TOKEN_BUCKET -> new TokenBucketRateLimiter(config, clock);
            case FIXED_WINDOW -> new FixedWindowRateLimiter(config, clock);
            case SLIDING_WINDOW_LOG -> new SlidingWindowLogRateLimiter(config, clock);
            case SLIDING_WINDOW_COUNTER -> new SlidingWindowCounterRateLimiter(config, clock);
            default -> throw new IllegalArgumentException("Unknown algorithm: " + algo);
        };
    }
}
