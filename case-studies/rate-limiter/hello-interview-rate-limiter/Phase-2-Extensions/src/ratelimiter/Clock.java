package ratelimiter;

// Monotonic time in nanoseconds. Injected so the checks in Main can control time exactly.
public interface Clock {
    long nowNanos();
}
