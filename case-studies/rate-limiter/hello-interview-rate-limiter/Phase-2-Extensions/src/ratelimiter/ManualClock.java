package ratelimiter;

// A clock that only moves when told to. Only the test's main thread advances it.
public class ManualClock implements Clock {
    private volatile long nanos;

    @Override
    public long nowNanos() {
        return nanos;
    }

    public void advanceSeconds(double seconds) {
        nanos += (long) (seconds * 1_000_000_000L);
    }
}
