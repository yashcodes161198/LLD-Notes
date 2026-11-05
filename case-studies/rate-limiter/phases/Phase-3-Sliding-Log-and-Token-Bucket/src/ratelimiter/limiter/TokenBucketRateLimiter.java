package ratelimiter.limiter;

import ratelimiter.enums.RateLimitType;
import ratelimiter.model.RateLimitConfig;

import java.util.HashMap;
import java.util.Map;

// Single-threaded version. Phase 5 makes it thread-safe.
public class TokenBucketRateLimiter extends RateLimiter {
    private final Map<String, Integer> tokens = new HashMap<>();
    private final Map<String, Long> lastRefillTime = new HashMap<>();

    public TokenBucketRateLimiter(RateLimitConfig config) {
        super(config, RateLimitType.TOKEN_BUCKET);
    }

    @Override
    public boolean allowRequest(String userId) {
        long now = System.currentTimeMillis();

        int currentTokens = refillTokens(userId, now);

        if (currentTokens > 0) {
            tokens.put(userId, currentTokens - 1); // consume 1 token
            return true;
        }

        tokens.put(userId, currentTokens); // remain at 0
        return false;
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
