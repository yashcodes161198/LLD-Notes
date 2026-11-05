package bookmyshow.model;

import bookmyshow.enums.*;
import java.util.List;

public class Booking {
    private final String bookingId;
    private final String userId;
    private final String showId;
    private final List<String> seatIds;
    private BookingStatus status;
    private PaymentType paymentType;
    private final double amount;
    public Booking(String bookingId, String userId, String showId,
                   List<String> seatIds, BookingStatus status,
                   PaymentType paymentType, double amount) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.showId = showId;
        this.seatIds = List.copyOf(seatIds);
        this.status = status;
        this.paymentType = paymentType;
        this.amount = amount;
    }
    public String getBookingId() { return bookingId; }
    public String getUserId() { return userId; }
    public String getShowId() { return showId; }
    public List<String> getSeatIds() { return seatIds; }
    public BookingStatus getStatus() { return status; }
    public PaymentType getPaymentType() { return paymentType; }
    public double getAmount() { return amount; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public void setPaymentType(PaymentType type) { paymentType = type; }
}
