package bookmyshow.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import bookmyshow.model.Booking;
import bookmyshow.model.BookingStatus;
import bookmyshow.model.Seat;
import bookmyshow.model.Show;

// Orchestrator. Every read-then-write on a show runs under that show's monitor, with the clock read inside it.
public class BookingSystem {
    public static final Duration HOLD_DURATION = Duration.ofMinutes(10); // Req 4




    private final Map<String, Show> shows;
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();
    private final Clock clock;
    private final AtomicLong nextId = new AtomicLong(1);

    public BookingSystem(List<Show> shows, Clock clock) {
        if (shows == null || clock == null) throw new IllegalArgumentException("shows and clock required");
        Map<String, Show> byId = new HashMap<>();
        for (Show show : shows) {
            if (show == null || byId.putIfAbsent(show.getId(), show) != null) {
                throw new IllegalArgumentException("null or duplicate show"); // fail fast at startup
            }
        }
        this.shows = Map.copyOf(byId);
        this.clock = clock;
    }

    // Req 1
    public List<Seat> getAvailableSeats(String showId) {
        Show show = findShow(showId);
        synchronized (show) {
            return show.getAvailableSeats(clock.instant());
        }
    }

    // Req 2. Returns the booking id. The booking is stored only after the hold succeeded.
    public String holdSeats(String userId, String showId, List<String> seatIds) {
        Set<String> unique = seatIds == null ? Set.of() : new HashSet<>(seatIds);
        if (userId == null || userId.isBlank() || unique.isEmpty()
                || unique.contains(null) || unique.size() != seatIds.size()) {
            throw new IllegalArgumentException("invalid request");
        }
        Show show = findShow(showId);
        Booking booking;
        synchronized (show) {
            Instant now = clock.instant(); // inside the lock (TRICKY #3)
            booking = new Booking("B" + nextId.getAndIncrement(), userId, show, seatIds, now.plus(HOLD_DURATION));
            show.hold(booking, now);
        }
        bookings.put(booking.getId(), booking);
        return booking.getId();
    }

    // Req 3. Same lock as hold, so "not expired" and CONFIRMED are one atomic step.
    public void confirmBooking(String bookingId, String paymentRef) {
        if (paymentRef == null || paymentRef.isBlank()) throw new IllegalArgumentException("invalid request");
        Booking booking = findBooking(bookingId);
        synchronized (booking.getShow()) {
            Instant now = clock.instant();
            if (booking.getStatus() == BookingStatus.CONFIRMED) return; // idempotent: payment callbacks retry
            if (booking.getStatus() == BookingStatus.CANCELLED) throw new IllegalStateException("booking cancelled: " + bookingId);
            if (booking.isExpired(now)) throw new HoldExpiredException(bookingId);
            booking.confirm(paymentRef);
        }
    }

    // Req 5. Idempotent. Allowed on an expired hold: release only frees seats it still holds.
    public void cancelBooking(String bookingId) {
        Booking booking = findBooking(bookingId);
        Show show = booking.getShow();
        synchronized (show) {
            if (booking.getStatus() == BookingStatus.CANCELLED) return;
            show.release(booking);
            booking.cancel();
        }
    }


    public Booking getBooking(String bookingId) {
        return findBooking(bookingId);
    }

    private Show findShow(String showId) {
        Show show = showId == null ? null : shows.get(showId);
        if (show == null) throw new ShowNotFoundException(showId);
        return show;
    }

    private Booking findBooking(String bookingId) {
        Booking booking = bookingId == null ? null : bookings.get(bookingId);
        if (booking == null) throw new BookingNotFoundException(bookingId);
        return booking;
    }
}
