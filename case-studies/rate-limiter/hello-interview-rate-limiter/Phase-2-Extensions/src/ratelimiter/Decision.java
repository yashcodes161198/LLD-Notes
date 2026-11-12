package ratelimiter;

// EXT 3: the answer plus WHEN to retry, so the gateway can set a Retry-After header.
public class Decision {
    private final boolean allowed;
    private final double retryAfterSeconds; // 0 when allowed

    private Decision(boolean allowed, double retryAfterSeconds) {
        this.allowed = allowed;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static Decision allow() {
        return new Decision(true, 0);
    }

    public static Decision deny(double retryAfterSeconds) {
        return new Decision(false, retryAfterSeconds);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public double getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    @Override
    public String toString() {
        return "Decision{allowed=" + allowed + ", retryAfterSeconds=" + retryAfterSeconds + "}";
    }
}
