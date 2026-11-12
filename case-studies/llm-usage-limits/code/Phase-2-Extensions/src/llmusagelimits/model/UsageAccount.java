package llmusagelimits.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// One user's windows, and the lock unit: parallel chats of one user serialize only here.
public class UsageAccount {
    private final List<UsageWindow> windows = new ArrayList<>();
    private int inFlight; // EXT 4

    public UsageAccount(List<WindowSpec> specs, int multiplier) {
        for (WindowSpec spec : specs) {
            UsageWindow w = new UsageWindow(spec);
            w.applyMultiplier(multiplier);
            windows.add(w);
        }
    }

    // EXT 4: per-user concurrency cap. Separate from usage: it bounds load, not spend.
    public synchronized boolean tryEnter(int maxInFlight) {
        if (inFlight >= maxInFlight) return false;
        inFlight++;
        return true;
    }

    public synchronized void leave() { inFlight--; }

    // Req 3, 5: atomic check + reserve over the windows this model counts against (EXT 3).
    // Returns the blocking window, or null if reserved.
    public synchronized UsageWindow tryReserve(Model model, long units, Instant now) {
        List<UsageWindow> relevant = relevant(model);
        for (UsageWindow w : relevant) {
            w.rollIfExpired(now);
            w.startIfIdle(now);
        }
        // Check ALL before reserving ANY (TRICKY #5); report the latest reset (TRICKY #4).
        UsageWindow blocking = null;
        for (UsageWindow w : relevant) {
            if (!w.hasRoom() && (blocking == null || w.getResetAt().isAfter(blocking.getResetAt()))) {
                blocking = w;
            }
        }
        if (blocking != null) return blocking;
        for (UsageWindow w : relevant) w.reserve(units);
        return null;
    }

    // Req 3: same model filter as tryReserve, or we release from a window we never reserved in.
    public synchronized void settle(Model model, long reservedUnits, long actualUnits, Instant now) {
        for (UsageWindow w : relevant(model)) {
            w.rollIfExpired(now);
            w.release(reservedUnits);
            if (actualUnits > 0) {
                w.startIfIdle(now);
                w.charge(actualUnits);
            }
        }
    }

    // EXT 1
    public synchronized void applyPlan(int multiplier) {
        for (UsageWindow w : windows) w.applyMultiplier(multiplier);
    }

    public synchronized long remaining(LimitType type, Instant now) {
        UsageWindow w = window(type);
        w.rollIfExpired(now);
        return w.remaining();
    }

    public synchronized long reserved(LimitType type) {
        return window(type).getReserved();
    }

    private List<UsageWindow> relevant(Model model) {
        List<UsageWindow> out = new ArrayList<>();
        for (UsageWindow w : windows) {
            if (w.appliesTo(model)) out.add(w);
        }
        return out;
    }

    private UsageWindow window(LimitType type) {
        for (UsageWindow w : windows) {
            if (w.getType() == type) return w;
        }
        throw new IllegalArgumentException("no window " + type);
    }
}
