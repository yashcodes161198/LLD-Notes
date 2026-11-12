package notificationrouter.router;

public class NoPreferredChannelException extends RuntimeException {
    public NoPreferredChannelException(String userId) {
        super("no preferred channel for user " + userId);
    }
}
