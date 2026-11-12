package bookmyshow.service;

public class HoldExpiredException extends RuntimeException {
    public HoldExpiredException(String bookingId) {
        super("hold expired: " + bookingId);
    }
}
