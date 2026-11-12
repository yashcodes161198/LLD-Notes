package ratelimiter;

// Req 2: a tier's burst capacity and average refill rate.
// Immutable value object (final fields, no setters): validated once, shared by every bucket of that tier.
public class Limit {
    private final int capacity;
    private final double refillPerSecond;

    public Limit(int capacity, double refillPerSecond) {
        if (capacity <= 0 || !(refillPerSecond > 0)) {
            throw new IllegalArgumentException("capacity and refillPerSecond must be > 0");
        }
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getRefillPerSecond() {
        return refillPerSecond;
    }
}
