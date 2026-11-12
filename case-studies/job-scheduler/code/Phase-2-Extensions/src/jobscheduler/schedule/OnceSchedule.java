package jobscheduler.schedule;

import java.time.Instant;

public final class OnceSchedule implements Schedule {
    private final Instant runAt;

    public OnceSchedule(Instant runAt) {
        if (runAt == null) throw new IllegalArgumentException("invalid job");
        this.runAt = runAt;
    }

    @Override public Instant firstSlot() { return runAt; }
    @Override public Instant nextSlotAfter(Instant slot, Instant now) { return null; }
}
