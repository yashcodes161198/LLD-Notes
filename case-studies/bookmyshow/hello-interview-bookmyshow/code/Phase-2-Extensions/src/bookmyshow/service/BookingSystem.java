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
import bookmyshow.notifier.SeatReleaseListener;
import bookmyshow.pricing.FlatPricing;
import bookmyshow.pricing.PricingStrategy;

// Orchestrator. Every read-then-write on a show runs under that show's monitor, with the clock read inside it.
// Listeners are always called after the lock is released.
public class BookingSystem {
    public static final Duration HOLD_DURATION = Duration.ofMinutes(10); // Req 4

    private final Map<String, Show> shows;
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();
    private final Clock clock;
    private final PricingStrategy pricing;                 // EXT 1
    private final List<SeatReleaseListener> listeners;     // EXT 2
    private final AtomicLong nextId = new AtomicLong(1);

    // Phase 1 constructor still works: everything free, nobody listening.
    public BookingSystem(List<Show> shows, Clock clock) {
        this(shows, clock, new FlatPricing(0), List.of());
    }

    public BookingSystem(List<Show> shows, Clock clock, PricingStrategy pricing, List<SeatReleaseListener> listeners) {
        if (shows == null || clock == null || pricing == null || listeners == null || listeners.stream().anyMatch(l -> l == null)) {
            throw new IllegalArgumentException("shows, clock, pricing and listeners required");
        }
        Map<String, Show> byId = new HashMap<>();
        for (Show show : shows) {
            if (show == null || byId.putIfAbsent(show.getId(), show) != null) {
                throw new IllegalArgumentException("null or duplicate show"); // fail fast at startup
            }
        }
        this.shows = Map.copyOf(byId);
        this.clock = clock;
        this.pricing = pricing;
        this.listeners = List.copyOf(listeners);
    }

    // Req 1
    public List<Seat> getAvailableSeats(String showId) {
        Show show = findShow(showId);
        synchronized (show) {
            return show.getAvailableSeats(clock.instant());
        }
    }

    // Req 2 + EXT 1. Returns the booking id. The booking is stored only after the hold succeeded.
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
            long amount = 0;
            for (String seatId : seatIds) amount += pricing.priceOf(show.getSeat(seatId)); // EXT 1: priced at hold time
            booking = new Booking("B" + nextId.getAndIncrement(), userId, show, seatIds, now.plus(HOLD_DURATION), amount);
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

    // Req 5 + EXT 2. Idempotent. Release only frees seats this booking still holds.
    public void cancelBooking(String bookingId) {
        Booking booking = findBooking(bookingId);
        Show show = booking.getShow();
        List<String> freed;
        synchronized (show) {
            if (booking.getStatus() == BookingStatus.CANCELLED) return;
            freed = show.release(booking);
            booking.cancel();
        }
        notifyReleased(show.getId(), freed);
    }

    // EXT 2: run by a scheduler every few seconds. Lazy expiry has no event, so this creates one.
    public int releaseExpiredHolds() {
        int total = 0;
        for (Show show : shows.values()) {
            List<String> freed;
            synchronized (show) {
                freed = show.removeExpiredHolders(clock.instant());
            }
            notifyReleased(show.getId(), freed);
            total += freed.size();
        }
        return total;
    }

    public Booking getBooking(String bookingId) {
        return findBooking(bookingId);
    }

    // The state change already happened, so a failing listener is logged, never thrown.
    private void notifyReleased(String showId, List<String> seatIds) {
        if (seatIds.isEmpty()) return;
        for (SeatReleaseListener listener : listeners) {
            try {
                listener.onSeatsReleased(showId, List.copyOf(seatIds));
            } catch (RuntimeException e) {
                System.err.println("[warn] listener failed for show " + showId + ": " + e.getMessage());
            }
        }
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
