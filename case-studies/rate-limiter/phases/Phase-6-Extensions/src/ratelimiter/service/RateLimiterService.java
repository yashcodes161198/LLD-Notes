package ratelimiter.service;

import ratelimiter.enums.RateLimitType;
import ratelimiter.enums.UserTier;
import ratelimiter.factory.RateLimiterFactory;
import ratelimiter.limiter.RateLimiter;
import ratelimiter.model.RateLimitConfig;
import ratelimiter.model.User;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

public class RateLimiterService {
    private final Map<UserTier, RateLimiter> rateLimiters = new HashMap<>();

    public RateLimiterService() {
        this(Clock.systemUTC());
    }

    public RateLimiterService(Clock clock) {
        // Configure per-tier limits + algorithms
        rateLimiters.put(
                UserTier.FREE,
                RateLimiterFactory.createRateLimiter(
                        RateLimitType.TOKEN_BUCKET,
                        new RateLimitConfig(10, 60), // 10 req/min
                        clock
                )
        );

        rateLimiters.put(
                UserTier.PREMIUM,
                RateLimiterFactory.createRateLimiter(
                        RateLimitType.FIXED_WINDOW,
                        new RateLimitConfig(100, 60), // 100 req/min
                        clock
                )
        );
    }

    public boolean allowRequest(User user) {
        RateLimiter limiter = rateLimiters.get(user.getTier());
        if (limiter == null) {
            throw new IllegalArgumentException("No limiter configured for tier: " + user.getTier());
        }
        return limiter.allowRequest(user.getUserId());
    }
}
