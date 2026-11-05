package ratelimiter.model;

public class RateLimitConfig {
    private final int maxRequests;
    private final int windowInSeconds;

    public RateLimitConfig(int maxRequests, int windowInSeconds) {
        if (maxRequests <= 0) {
            throw new IllegalArgumentException("maxRequests must be positive, got " + maxRequests);
        }
        if (windowInSeconds <= 0) {
            throw new IllegalArgumentException("windowInSeconds must be positive, got " + windowInSeconds);
        }
        this.maxRequests = maxRequests;
        this.windowInSeconds = windowInSeconds;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public int getWindowInSeconds() {
        return windowInSeconds;
    }
}
