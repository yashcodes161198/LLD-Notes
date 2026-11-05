package ratelimiter.clock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

// A clock that only moves when told to, so time-based behaviour can be replayed exactly.
// Only one thread advances it; limiters on other threads just read it.
public class ManualClock extends Clock {
    private volatile long millis;

    public ManualClock(long startMillis) {
        this.millis = startMillis;
    }

    public void advanceSeconds(long seconds) {
        millis += seconds * 1000;
    }

    @Override
    public long millis() {
        return millis;
    }

    @Override
    public Instant instant() {
        return Instant.ofEpochMilli(millis);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
