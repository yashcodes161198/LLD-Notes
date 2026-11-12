package llmusagelimits.model;

import java.time.Instant;

// One limit of one user (Req 4). Owns its counters and its reset rule (Information Expert).
// Not thread-safe on its own: only touched under its UsageAccount's lock.
public class UsageWindow {
    private final WindowSpec spec;
    private long limit;         // EXT 1: base limit x plan multiplier
    private long used;          // Req 2: actual charges in this window
    private long reserved;      // Req 5: estimates of in-flight requests
    private Instant resetAt;      // null = no active window

    public UsageWindow(WindowSpec spec) {
        if (spec == null) throw new IllegalArgumentException("invalid window");
        this.spec = spec;
        this.limit = spec.getBaseLimit();
    }

    // EXT 3: an OPUS-only window ignores other models.
    public boolean appliesTo(Model model) { return spec.getModels().contains(model); }

    // EXT 1: a plan change moves the limit and keeps used/reserved (no free refill).
    public void applyMultiplier(int multiplier) { limit = spec.getBaseLimit() * multiplier; }

    // Reset clears what was spent, not what is still running (TRICKY #3).
    public void rollIfExpired(Instant now) {
        if (resetAt != null && !now.isBefore(resetAt)) {
            used = 0;
            resetAt = null;
        }
    }

    public void startIfIdle(Instant now) {
        if (resetAt == null) resetAt = now.plus(spec.getDuration());
    }

    // Req 5: blocked only when used + reserved has reached the limit.
    public boolean hasRoom() { return used + reserved < limit; }

    public void reserve(long units) { reserved += units; }
    public void release(long units) { reserved -= units; }
    public void charge(long units) { used += units; }

    // Req 6: an overshoot shows as 0, not negative.
    public long remaining() { return Math.max(0, limit - used); }

    public LimitType getType() { return spec.getType(); }
    public Instant getResetAt() { return resetAt; }
    public long getReserved() { return reserved; }
}
