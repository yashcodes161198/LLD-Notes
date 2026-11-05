package ratelimiter.limiter;

import ratelimiter.enums.RateLimitType;
import ratelimiter.model.RateLimitConfig;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class TokenBucketRateLimiter extends RateLimiter {
    private final Map<String, Integer> tokens = new ConcurrentHashMap<>();
    private final Map<String, Long> lastRefillTime = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(RateLimitConfig config) {
        this(config, Clock.systemUTC());
    }

    public TokenBucketRateLimiter(RateLimitConfig config, Clock clock) {
        super(config, RateLimitType.TOKEN_BUCKET, clock);
    }

    @Override
    public boolean allowRequest(String userId) {
        AtomicBoolean allowed = new AtomicBoolean(false);
        long now = clock.millis();

        tokens.compute(userId, (id, availableTokens) -> {
            int currentTokens = refillTokens(userId, now);

            if (currentTokens > 0) {
                allowed.set(true);           // mark allowed BEFORE we decrement
                return currentTokens - 1;    // consume 1 token
            } else {
                return currentTokens;        // remain at 0
            }
        });

        return allowed.get();
    }

    // free user: one token every 60 / 10 = 6 s
    private int refillTokens(String userId, long now) {
        double refillIntervalMillis = config.getWindowInSeconds() * 1000.0 / config.getMaxRequests();

        lastRefillTime.putIfAbsent(userId, now);
        long lastRefill = lastRefillTime.get(userId);

        int refillTokens = (int) ((now - lastRefill) / refillIntervalMillis);
        int currentTokens = tokens.getOrDefault(userId, config.getMaxRequests());

        if (currentTokens + refillTokens >= config.getMaxRequests()) {
            // bucket is full: time spent full earns nothing, so the next token is a full interval away
            lastRefillTime.put(userId, now);
            return config.getMaxRequests();
        }

        if (refillTokens > 0) {
            // move on only by the time those tokens took, keeping progress towards the next one
            lastRefillTime.put(userId, lastRefill + (long) (refillTokens * refillIntervalMillis));
        }
        return currentTokens + refillTokens;
    }
}
