package ratelimiter;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

// The orchestrator: finds the client's bucket and asks it for a decision.
public class RateLimiter {
    private final Map<Tier, Limit> limits;                                       // Req 2
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();  // Req 4: one per client
    private final Clock clock;

    public RateLimiter(Map<Tier, Limit> limits, Clock clock) {
        // Validate the config once at startup, so a lookup can never miss at request time.
        for (Tier tier : Tier.values()) {
            if (limits.get(tier) == null) {
                throw new IllegalArgumentException("missing limit for tier " + tier);
            }
        }
        this.limits = new EnumMap<>(limits); // a copy: the caller can't change limits later
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    // Req 1. Returns false when the client is over its limit: that's a normal answer, not an error.
    public boolean allowRequest(String clientId, Tier tier) {
        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalArgumentException("invalid client id");
        }
        Objects.requireNonNull(tier, "tier");

        long now = clock.nowNanos(); // read the clock once per request and pass it down
        TokenBucket bucket = getOrCreateBucket(clientId, tier, now);
        return bucket.tryConsume(now);
    }

    private TokenBucket getOrCreateBucket(String clientId, Tier tier, long now) {
        Limit limit = limits.get(tier);
        // computeIfAbsent is atomic per key: two threads that see a new client at the same
        // moment both get the SAME bucket, so nobody consumes from a bucket that gets lost.
        // shortcut: buckets are never removed (~100 bytes each, ~100 MB per million clients);
        // if memory grows, sweep buckets idle longer than capacity / refillPerSecond (they're full again).
        return buckets.computeIfAbsent(clientId, id -> new TokenBucket(limit, now));
    }
}
