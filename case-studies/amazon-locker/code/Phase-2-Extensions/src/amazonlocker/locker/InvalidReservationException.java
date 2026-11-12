package amazonlocker.locker;

public class InvalidReservationException extends RuntimeException {
    public InvalidReservationException(String reason) {
        super("invalid reservation: " + reason);
    }
}
