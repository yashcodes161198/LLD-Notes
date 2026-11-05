package ratelimiter.limiter;

import ratelimiter.enums.RateLimitType;
import ratelimiter.model.RateLimitConfig;

import java.time.Clock;

public abstract class RateLimiter {
    protected final RateLimitConfig config;
    protected final RateLimitType type;
    protected final Clock clock;

    public RateLimiter(RateLimitConfig config, RateLimitType type, Clock clock) {
        this.config = config;
        this.type = type;
        this.clock = clock;
    }

    public abstract boolean allowRequest(String userId);
}
