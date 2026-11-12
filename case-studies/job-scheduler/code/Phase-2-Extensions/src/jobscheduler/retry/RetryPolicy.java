package jobscheduler.retry;

import java.time.Duration;

// EXT 2: Phase 1's two scheduler constants, promoted to a per-job value class with exponential backoff.
// shortcut: no jitter (keeps the checks deterministic); add +-20% random jitter before production.
public final class RetryPolicy {
    private final int maxAttempts;
    private final Duration initialDelay;
    private final double multiplier;
    private final Duration maxDelay;

    private RetryPolicy(int maxAttempts, Duration initialDelay, double multiplier, Duration maxDelay) {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
        if (initialDelay == null || initialDelay.isNegative()) throw new IllegalArgumentException("invalid retryDelay");
        if (multiplier < 1.0) throw new IllegalArgumentException("multiplier must be >= 1");
        if (maxDelay == null || maxDelay.compareTo(initialDelay) < 0) throw new IllegalArgumentException("maxDelay must be >= initialDelay");
        this.maxAttempts = maxAttempts;
        this.initialDelay = initialDelay;
        this.multiplier = multiplier;
        this.maxDelay = maxDelay;
    }

    public static RetryPolicy fixed(int maxAttempts, Duration delay) {
        return new RetryPolicy(maxAttempts, delay, 1.0, delay);
    }

    public static RetryPolicy exponential(int maxAttempts, Duration initialDelay, double multiplier, Duration maxDelay) {
        return new RetryPolicy(maxAttempts, initialDelay, multiplier, maxDelay);
    }

    public boolean canRetry(int attempts) { return attempts < maxAttempts; }

    // Delay before the next try, after `attempt` failed tries: initial * multiplier^(attempt-1), capped.
    public Duration delayAfter(int attempt) {
        double millis = initialDelay.toMillis() * Math.pow(multiplier, attempt - 1);
        return Duration.ofMillis((long) Math.min(millis, maxDelay.toMillis()));
    }

    public int getMaxAttempts() { return maxAttempts; }
}
