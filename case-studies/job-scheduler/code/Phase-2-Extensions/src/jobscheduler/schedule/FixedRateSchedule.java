package jobscheduler.schedule;

import java.time.Duration;
import java.time.Instant;

// Phase 1's interval arithmetic, moved out of Job. Anchored on the slot, so retries never cause drift.
public final class FixedRateSchedule implements Schedule {
    private final Instant startAt;
    private final Duration interval;

    public FixedRateSchedule(Instant startAt, Duration interval) {
        if (startAt == null) throw new IllegalArgumentException("invalid job");
        if (interval == null || interval.isZero() || interval.isNegative()) throw new IllegalArgumentException("invalid interval");
        this.startAt = startAt;
        this.interval = interval;
    }

    @Override public Instant firstSlot() { return startAt; }

    @Override
    public Instant nextSlotAfter(Instant slot, Instant now) {
        Instant next = slot.plus(interval);
        if (!next.isAfter(now)) {
            long missed = Duration.between(slot, now).toNanos() / interval.toNanos();
            next = slot.plus(interval.multipliedBy(missed + 1));
        }
        return next;
    }
}
