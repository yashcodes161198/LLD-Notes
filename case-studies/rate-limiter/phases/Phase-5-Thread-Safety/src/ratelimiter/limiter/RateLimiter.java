package ratelimiter.limiter;

import ratelimiter.enums.RateLimitType;
import ratelimiter.model.RateLimitConfig;

public abstract class RateLimiter {
    protected final RateLimitConfig config;
    protected final RateLimitType type;

    public RateLimiter(RateLimitConfig config, RateLimitType type) {
        this.config = config;
        this.type = type;
    }

    public abstract boolean allowRequest(String userId);
}
