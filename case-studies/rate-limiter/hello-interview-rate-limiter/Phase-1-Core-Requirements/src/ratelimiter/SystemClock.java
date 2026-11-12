package ratelimiter;

public class SystemClock implements Clock {
    @Override
    public long nowNanos() {
        return System.nanoTime(); // monotonic: does not jump when NTP adjusts the wall clock
    }
}
