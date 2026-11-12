package jobscheduler.schedule;

import java.time.Instant;

// EXT 1: the only thing that varies between one-off, fixed-rate and cron is "what's the next slot".
public interface Schedule {
    Instant firstSlot();

    // The first slot after both `slot` and `now` (missed slots are skipped), or null when there are no more runs.
    Instant nextSlotAfter(Instant slot, Instant now);
}
