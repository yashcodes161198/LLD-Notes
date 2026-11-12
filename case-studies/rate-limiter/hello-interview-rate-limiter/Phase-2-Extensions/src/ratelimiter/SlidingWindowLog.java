package ratelimiter;

import java.util.ArrayDeque;
import java.util.Deque;

// EXT 1: at most capacity requests in any window. Exact, but costs memory per REQUEST
// (up to capacity timestamps per client), where a token bucket is O(1) per client.
public class SlidingWindowLog implements ClientLimiter {
    private final Limit limit;
    private final long windowNanos;
    private final Deque<Long> timestamps = new ArrayDeque<>(); // allowed requests, oldest first

    public SlidingWindowLog(Limit limit) {
        this.limit = limit;
        this.windowNanos = (long) (limit.getWindowSeconds() * 1_000_000_000L);
    }

    // ArrayDeque isn't thread-safe; synchronized makes forget + check + record one step (Req 5).
    @Override
    public synchronized Decision tryConsume(long nowNanos) {
        while (!timestamps.isEmpty() && timestamps.peekFirst() <= nowNanos - windowNanos) {
            timestamps.pollFirst(); // forget requests that have left the window
        }
        if (timestamps.size() < limit.getCapacity()) {
            timestamps.addLast(nowNanos);
            return Decision.allow();
        }
        // EXT 3: a slot frees up when the oldest request leaves the window.
        long oldestLeavesAt = timestamps.peekFirst() + windowNanos;
        return Decision.deny((oldestLeavesAt - nowNanos) / 1_000_000_000.0);
    }
}
