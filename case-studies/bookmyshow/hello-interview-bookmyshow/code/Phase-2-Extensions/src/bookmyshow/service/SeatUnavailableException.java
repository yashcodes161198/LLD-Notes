package bookmyshow.service;

public class SeatUnavailableException extends RuntimeException {
    public SeatUnavailableException(String seatId) {
        super("seat unavailable: " + seatId);
    }
}
