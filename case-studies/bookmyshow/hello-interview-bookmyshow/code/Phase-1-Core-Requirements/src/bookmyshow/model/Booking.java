package bookmyshow.model;

import java.time.Instant;
import java.util.List;

// A hold (PENDING) and later the ticket (CONFIRMED). Status changes happen under the show's lock.
public class Booking {
    private final String id;
    private final String userId;
    private final Show show;
    private final List<String> seatIds; // what this booking once held; Show.seatHolders is the truth
    private final Instant expiresAt;
    private BookingStatus status = BookingStatus.PENDING;
    private String paymentRef;

    public Booking(String id, String userId, Show show, List<String> seatIds, Instant expiresAt) {
        if (id == null || userId == null || show == null || seatIds == null || expiresAt == null) {
            throw new IllegalArgumentException("invalid booking");
        }
        this.id = id;
        this.userId = userId;
        this.show = show;
        this.seatIds = List.copyOf(seatIds);
        this.expiresAt = expiresAt;
    }

    // Req 4: a seat is taken iff its holder is active.
    public boolean isActive(Instant now) {
        return status == BookingStatus.CONFIRMED || (status == BookingStatus.PENDING && now.isBefore(expiresAt));
    }

    public boolean isExpired(Instant now) {
        return status == BookingStatus.PENDING && !now.isBefore(expiresAt);
    }

    public void confirm(String paymentRef) {
        this.status = BookingStatus.CONFIRMED;
        this.paymentRef = paymentRef;
    }

    public void cancel() { this.status = BookingStatus.CANCELLED; }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public Show getShow() { return show; }
    public List<String> getSeatIds() { return seatIds; }
    public Instant getExpiresAt() { return expiresAt; }
    public BookingStatus getStatus() { return status; }
    public String getPaymentRef() { return paymentRef; }

    @Override
    public String toString() { return id + "(" + status + " " + seatIds + ")"; }
}
