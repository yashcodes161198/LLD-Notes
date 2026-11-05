package bookmyshow.strategy.locking;

public interface LockProvider {
    boolean tryLock(String key, String ownerToken, long ttlMs);
    void unlock(String key, String ownerToken);
    boolean isLockExpired(String key);
    boolean isLockedBy(String key, String ownerToken);
}
