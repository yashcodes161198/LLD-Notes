package amazonlocker.locker;

public class AccessCodeExpiredException extends RuntimeException {
    public AccessCodeExpiredException() {
        super("access code expired");
    }
}
