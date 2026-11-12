package llmusagelimits.limiter;

// EXT 4: a load limit, not a usage limit, so it's a different error.
public class TooManyConcurrentRequestsException extends RuntimeException {
    public TooManyConcurrentRequestsException(String userId, int maxInFlight) {
        super("user " + userId + " already has " + maxInFlight + " requests in flight");
    }
}
