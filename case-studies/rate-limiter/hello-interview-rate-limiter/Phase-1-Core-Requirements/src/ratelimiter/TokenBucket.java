package ratelimiter;

// One client's state and rules: how many tokens it has, and when they were last topped up.
// tokens and lastRefillNanos always change together, so they live in one object,
// which also gives exactly one thing to lock per client.
public class TokenBucket {
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private final Limit limit;
    private double tokens;          // a decimal on purpose: partial tokens carry over between calls
    private long lastRefillNanos;

    public TokenBucket(Limit limit, long nowNanos) {
        this.limit = limit;
        this.tokens = limit.getCapacity(); // Req 4: a new client starts with its full burst
        this.lastRefillNanos = nowNanos;
    }

    // Req 5: synchronized locks THIS bucket, so refill + check + take are one step and two
    // threads can never both take the last token. Other clients have other buckets,
    // so they never wait on each other.
    public synchronized boolean tryConsume(long nowNanos) {
        refill(nowNanos);
        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }
        return false; // Req 3: a denied request consumes nothing
    }

    // Lazy refill: work out what the bucket earned since last time. O(1), no background thread.
    private void refill(long nowNanos) {
        long elapsed = nowNanos - lastRefillNanos;
        if (elapsed <= 0) {
            return; // same instant, or the clock went backwards: add nothing
        }
        double earned = elapsed / NANOS_PER_SECOND * limit.getRefillPerSecond();
        tokens = Math.min(limit.getCapacity(), tokens + earned); // the cap: idle time is not banked
        lastRefillNanos = nowNanos;
    }
}
