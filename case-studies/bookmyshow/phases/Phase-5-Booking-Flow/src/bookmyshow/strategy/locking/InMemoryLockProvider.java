package bookmyshow.strategy.locking;

import java.util.concurrent.*;
import java.util.function.LongSupplier;

public class InMemoryLockProvider implements LockProvider, AutoCloseable {
    private static class Expiry {
        final long deadline;
        final String owner;
        Expiry(long deadline, String owner) {
            this.deadline = deadline;
            this.owner = owner;
        }
    }
    private final ConcurrentHashMap<String, Expiry> locks =
        new ConcurrentHashMap<>();
    private final ScheduledExecutorService sweeper =
        Executors.newSingleThreadScheduledExecutor();
    private final LongSupplier clock;
    public InMemoryLockProvider() { this(System::currentTimeMillis); }
    public InMemoryLockProvider(LongSupplier clock) {
        this.clock = clock;
        sweeper.scheduleAtFixedRate(this::sweep, 1, 1, TimeUnit.MINUTES);
    }
    private void sweep() {
        long now = clock.getAsLong();
        locks.entrySet().removeIf(e -> e.getValue().deadline <= now);
    }
    public boolean tryLock(String key, String userId, long ttlMs) {
        long now = clock.getAsLong();
        Expiry expiry = new Expiry(now + ttlMs, userId);
        return locks.compute(key, (k, old) ->
            old == null || old.deadline <= now ? expiry : old) == expiry;
    }
    public void unlock(String key) { locks.remove(key); }
    public boolean isLockExpired(String key) {
        Expiry expiry = locks.get(key);
        return expiry == null || expiry.deadline <= clock.getAsLong();
    }
    public boolean isLockedBy(String key, String userId) {
        Expiry expiry = locks.get(key);
        return expiry != null && expiry.owner.equals(userId);
    }
    public void close() { sweeper.shutdownNow(); }
}
