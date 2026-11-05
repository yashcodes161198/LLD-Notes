package ratelimiter.model;

public class RateLimitConfig {
    private final int maxRequests;
    private final int windowInSeconds;

    public RateLimitConfig(int maxRequests, int windowInSeconds) {
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
