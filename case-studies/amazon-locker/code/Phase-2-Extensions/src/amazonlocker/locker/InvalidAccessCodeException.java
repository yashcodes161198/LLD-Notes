package amazonlocker.locker;

public class InvalidAccessCodeException extends RuntimeException {
    public InvalidAccessCodeException() {
        super("invalid code");
    }
}
