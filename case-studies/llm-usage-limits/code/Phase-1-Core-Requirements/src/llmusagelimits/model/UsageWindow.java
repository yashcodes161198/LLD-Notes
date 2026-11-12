package llmusagelimits.model;

import java.time.Duration;
import java.time.Instant;

// One limit of one user (Req 4). Owns its counters and its reset rule (Information Expert).
// Not thread-safe on its own: only touched under its UsageAccount's lock.
public class UsageWindow {
    private final LimitType type;
    private final Duration duration;
    private final long limit;
    private long used;          // Req 2: actual charges in this window
    private long reserved;      // Req 5: estimates of in-flight requests
    private Instant resetAt;    // null = no active window

    public UsageWindow(LimitType type, Duration duration, long limit) {
        if (type == null || duration == null || duration.isNegative() || duration.isZero() || limit <= 0) {
            throw new IllegalArgumentException("invalid window");
        }
        this.type = type;
        this.duration = duration;
        this.limit = limit;
    }

    // Reset clears what was spent, not what is still running (TRICKY #3).
    public void rollIfExpired(Instant now) {
        if (resetAt != null && !now.isBefore(resetAt)) {
            used = 0;
            resetAt = null;
        }
    }

    public void startIfIdle(Instant now) {
        if (resetAt == null) resetAt = now.plus(duration);
    }

    // Req 5: blocked only when used + reserved has reached the limit.
    public boolean hasRoom() { return used + reserved < limit; }

    public void reserve(long units) { reserved += units; }
    public void release(long units) { reserved -= units; }
    public void charge(long units) { used += units; }

    // Req 6: an overshoot shows as 0, not negative.
    public long remaining() { return Math.max(0, limit - used); }

    public LimitType getType() { return type; }
    public Instant getResetAt() { return resetAt; }
    public long getReserved() { return reserved; }
}
