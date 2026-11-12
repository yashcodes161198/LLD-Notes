package bookmyshow.service;

public class BookingNotFoundException extends RuntimeException {
    public BookingNotFoundException(String bookingId) {
        super("booking not found: " + bookingId);
    }
}
