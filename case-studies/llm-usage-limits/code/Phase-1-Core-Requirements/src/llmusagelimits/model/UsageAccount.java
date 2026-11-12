package llmusagelimits.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

// One user's windows, and the lock unit: parallel chats of one user serialize only here.
public class UsageAccount {
    private final List<UsageWindow> windows;

    public UsageAccount(long fiveHourLimit, long weeklyLimit) {
        this.windows = List.of(
                new UsageWindow(LimitType.FIVE_HOUR, Duration.ofHours(5), fiveHourLimit),
                new UsageWindow(LimitType.WEEKLY, Duration.ofDays(7), weeklyLimit));
    }

    // Req 3, 5: atomic check + reserve. Returns the blocking window, or null if reserved.
    public synchronized UsageWindow tryReserve(long units, Instant now) {
        for (UsageWindow w : windows) {
            w.rollIfExpired(now);
            w.startIfIdle(now);
        }
        // Check ALL before reserving ANY, or a full weekly window leaks a 5h reservation (TRICKY #5).
        UsageWindow blocking = null;
        for (UsageWindow w : windows) {
            if (!w.hasRoom() && (blocking == null || w.getResetAt().isAfter(blocking.getResetAt()))) {
                blocking = w; // the latest reset is when the user can really come back (TRICKY #4)
            }
        }
        if (blocking != null) return blocking;
        for (UsageWindow w : windows) w.reserve(units);
        return null;
    }

    // Req 3: release the estimate, charge the actual (0 when inference failed).
    public synchronized void settle(long reservedUnits, long actualUnits, Instant now) {
        for (UsageWindow w : windows) {
            w.rollIfExpired(now);
            w.release(reservedUnits);
            if (actualUnits > 0) {
                w.startIfIdle(now);
                w.charge(actualUnits);
            }
        }
    }

    public synchronized long remaining(LimitType type, Instant now) {
        UsageWindow w = window(type);
        w.rollIfExpired(now);
        return w.remaining();
    }

    public synchronized long reserved(LimitType type) {
        return window(type).getReserved();
    }

    private UsageWindow window(LimitType type) {
        for (UsageWindow w : windows) {
            if (w.getType() == type) return w;
        }
        throw new IllegalArgumentException("no window " + type);
    }
}
