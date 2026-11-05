package ratelimiter.factory;

import ratelimiter.enums.RateLimitType;
import ratelimiter.limiter.FixedWindowRateLimiter;
import ratelimiter.limiter.RateLimiter;
import ratelimiter.limiter.SlidingWindowLogRateLimiter;
import ratelimiter.limiter.TokenBucketRateLimiter;
import ratelimiter.model.RateLimitConfig;

public class RateLimiterFactory {
    public static RateLimiter createRateLimiter(RateLimitType algo, RateLimitConfig config) {
        return switch (algo) {
            case TOKEN_BUCKET -> new TokenBucketRateLimiter(config);
            case FIXED_WINDOW -> new FixedWindowRateLimiter(config);
            case SLIDING_WINDOW_LOG -> new SlidingWindowLogRateLimiter(config);
            default -> throw new IllegalArgumentException("Unknown algorithm: " + algo);
        };
    }
}
