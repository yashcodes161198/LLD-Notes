package ratelimiter;

// Req 2: a limit's capacity and average rate, plus (EXT 1) which algorithm enforces it.
// Immutable value object (final fields, no setters): validated once, shared by every limiter built from it.
public class Limit {
    private final Algorithm algorithm;
    private final int capacity;
    private final double refillPerSecond;

    public Limit(Algorithm algorithm, int capacity, double refillPerSecond) {
        if (algorithm == null) {
            throw new IllegalArgumentException("algorithm is required");
        }
        if (capacity <= 0 || !(refillPerSecond > 0) || Double.isInfinite(refillPerSecond)) {
            throw new IllegalArgumentException("capacity and refillPerSecond must be > 0");
        }
        this.algorithm = algorithm;
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
    }

    // Burst up to capacity, then refillPerSecond on average.
    public static Limit tokenBucket(int capacity, double refillPerSecond) {
        return new Limit(Algorithm.TOKEN_BUCKET, capacity, refillPerSecond);
    }

    // At most maxRequests in ANY windowSeconds-long stretch of time.
    public static Limit slidingWindow(int maxRequests, double windowSeconds) {
        if (!(windowSeconds > 0)) {
            throw new IllegalArgumentException("windowSeconds must be > 0");
        }
        return new Limit(Algorithm.SLIDING_WINDOW_LOG, maxRequests, maxRequests / windowSeconds);
    }

    public Algorithm getAlgorithm() {
        return algorithm;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getRefillPerSecond() {
        return refillPerSecond;
    }

    public double getWindowSeconds() {
        return capacity / refillPerSecond;
    }
}
