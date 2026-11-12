package ratelimiter;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

// The orchestrator: finds the client's limiter and asks it for a decision.
public class RateLimiter {
    // EXT 2: endpoints without an override share ONE default budget per client, as in Phase 1.
    // (The alternative, a full default budget on EVERY endpoint, is a product decision: ask.)
    private static final String DEFAULT_ENDPOINT = "*";

    private final Map<Tier, Limit> limits;                       // Req 2: defaults per tier
    private final Map<String, Map<Tier, Limit>> endpointLimits;  // EXT 2: endpoint -> tier -> override
    private final Map<BucketKey, ClientLimiter> limiters = new ConcurrentHashMap<>(); // Req 4
    private final Clock clock;

    // EXT 2: one limiter per (client, endpoint). Two fields, not one "client|endpoint" string,
    // so ids containing "|" can never collide.
    // It's a map key, so it needs equals + hashCode: two keys with the same fields are the same key.
    private static class BucketKey {
        private final String clientId;
        private final String endpoint;

        BucketKey(String clientId, String endpoint) {
            this.clientId = clientId;
            this.endpoint = endpoint;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof BucketKey)) return false;
            BucketKey other = (BucketKey) o;
            return clientId.equals(other.clientId) && endpoint.equals(other.endpoint);
        }

        @Override
        public int hashCode() {
            return Objects.hash(clientId, endpoint);
        }
    }

    public RateLimiter(Map<Tier, Limit> limits, Clock clock) {
        this(limits, Map.of(), clock);
    }

    public RateLimiter(Map<Tier, Limit> limits, Map<String, Map<Tier, Limit>> endpointLimits, Clock clock) {
        // Validate the config once at startup, so a lookup can never miss at request time.
        for (Tier tier : Tier.values()) {
            if (limits.get(tier) == null) {
                throw new IllegalArgumentException("missing limit for tier " + tier);
            }
        }
        this.limits = new EnumMap<>(limits); // copies: the caller can't change limits later

        Map<String, Map<Tier, Limit>> overrides = new HashMap<>();
        for (Map.Entry<String, Map<Tier, Limit>> entry : endpointLimits.entrySet()) {
            overrides.put(entry.getKey(), Map.copyOf(entry.getValue())); // copyOf rejects null limits
        }
        this.endpointLimits = overrides;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    // Callers that don't know the endpoint use the client's default budget.
    public Decision allowRequest(String clientId, Tier tier) {
        return allowRequest(clientId, tier, DEFAULT_ENDPOINT);
    }

    // Req 1 + EXT 2 + EXT 3. A denial is a normal answer, not an error.
    public Decision allowRequest(String clientId, Tier tier, String endpoint) {
        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalArgumentException("invalid client id");
        }
        Objects.requireNonNull(tier, "tier");
        if (endpoint == null || endpoint.isEmpty()) {
            throw new IllegalArgumentException("invalid endpoint");
        }

        Limit override = endpointLimits.getOrDefault(endpoint, Map.of()).get(tier);
        Limit limit = (override != null) ? override : limits.get(tier);
        BucketKey key = new BucketKey(clientId, (override != null) ? endpoint : DEFAULT_ENDPOINT);

        long now = clock.nowNanos(); // read the clock once per request and pass it down
        ClientLimiter limiter = getOrCreateLimiter(key, limit, now);
        return limiter.tryConsume(now);
    }

    private ClientLimiter getOrCreateLimiter(BucketKey key, Limit limit, long now) {
        // computeIfAbsent is atomic per key: two threads that see a new client at the same
        // moment both get the SAME limiter.
        // shortcut: limiters are never removed; if memory grows, sweep ones idle longer than
        // capacity / refillPerSecond (a token bucket is full again by then).
        return limiters.computeIfAbsent(key, k -> createLimiter(limit, now));
    }

    // EXT 1: the only place that knows there is more than one algorithm (a simple factory).
    private static ClientLimiter createLimiter(Limit limit, long now) {
        return switch (limit.getAlgorithm()) {
            case TOKEN_BUCKET -> new TokenBucket(limit, now);
            case SLIDING_WINDOW_LOG -> new SlidingWindowLog(limit);
        };
    }
}
