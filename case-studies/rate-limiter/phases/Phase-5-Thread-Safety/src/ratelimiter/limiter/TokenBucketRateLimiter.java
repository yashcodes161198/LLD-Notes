package ratelimiter.limiter;

import ratelimiter.enums.RateLimitType;
import ratelimiter.model.RateLimitConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class TokenBucketRateLimiter extends RateLimiter {
    private final Map<String, Integer> tokens = new ConcurrentHashMap<>();
    private final Map<String, Long> lastRefillTime = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(RateLimitConfig config) {
        super(config, RateLimitType.TOKEN_BUCKET);
    }

    @Override
    public boolean allowRequest(String userId) {
        AtomicBoolean allowed = new AtomicBoolean(false);
        long now = System.currentTimeMillis();

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

    // free user refill rate = 60 / 10 = 6
    private int refillTokens(String userId, long now) {
        double refillRate = (double) config.getWindowInSeconds() / config.getMaxRequests();

        lastRefillTime.putIfAbsent(userId, now);
        long lastRefill = lastRefillTime.get(userId);

        long elapsedSeconds = (now - lastRefill) / 1000;
        int refillTokens = (int) (elapsedSeconds / refillRate);
        int currentTokens = tokens.getOrDefault(userId, config.getMaxRequests());
        currentTokens = Math.min(config.getMaxRequests(), currentTokens + refillTokens);

        if (refillTokens > 0) lastRefillTime.put(userId, now);
        return currentTokens;
    }
}
