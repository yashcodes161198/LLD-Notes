package bookmyshow.service;

import bookmyshow.model.*;
import bookmyshow.enums.*;
import bookmyshow.repository.BookingRepository;
import bookmyshow.strategy.locking.LockProvider;
import bookmyshow.strategy.payment.PaymentStrategy;
import bookmyshow.factory.PaymentStrategyFactory;
import bookmyshow.exception.SeatNotAvailableException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BookingService {
    private static final long TTL = 5_000;
    private static final AtomicInteger ids = new AtomicInteger();
    private final LockProvider lockProvider;
    private final BookingRepository bookingRepo;
    private final java.util.function.Function<PaymentType, PaymentStrategy>
        payments;
    public BookingService(LockProvider locks, BookingRepository repo) {
        this(locks, repo, PaymentStrategyFactory::getStrategy);
    }
    public BookingService(LockProvider locks, BookingRepository repo,
            java.util.function.Function<PaymentType, PaymentStrategy> payments) {
        this.lockProvider = Objects.requireNonNull(locks);
        this.bookingRepo = Objects.requireNonNull(repo);
        this.payments = Objects.requireNonNull(payments);
    }
    public Booking createBooking(String userId, Show show,
                                 List<String> seatIds) {
        if (userId == null || userId.isBlank() || show == null
            || seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("User, show and seats required");
        }
        List<String> selected = List.copyOf(seatIds);
        if (new HashSet<>(selected).size() != selected.size()) {
            throw new IllegalArgumentException("Duplicate seat IDs");
        }
        Map<String, Seat> available = new HashMap<>();
        for (Seat seat : show.getSeats()) available.put(seat.getId(), seat);
        double amount = 0;
        List<String> keys = new ArrayList<>();
        for (String id : selected) {
            Seat seat = available.get(id);
            if (seat == null) throw new IllegalArgumentException("Unknown seat");
            if (!Double.isFinite(seat.getPrice()) || seat.getPrice() < 0) {
                throw new IllegalArgumentException("Invalid seat price");
            }
            keys.add(key(show.getId(), id));
            amount += seat.getPrice();
        }
        if (!Double.isFinite(amount)) {
            throw new IllegalArgumentException("Amount overflow");
        }
        String token = "B" + ids.incrementAndGet();
        synchronized (bookingRepo) {
            List<String> acquired = new ArrayList<>();
            try {
                for (String key : keys) {
                    if (bookingRepo.isBooked(key)
                        || !lockProvider.tryLock(key, token, TTL)) {
                        throw new SeatNotAvailableException("Seat unavailable");
                    }
                    acquired.add(key);
                }
                Booking booking = new Booking(token, userId, show.getId(),
                    selected, BookingStatus.CREATED, null, amount);
                bookingRepo.save(booking);
                return booking;
            } catch (RuntimeException e) {
                for (String key : acquired) lockProvider.unlock(key, token);
                throw e;
            }
        }
    }
    public void confirmBooking(Booking booking, PaymentType paymentType) {
        confirmBooking(booking.getBookingId(), booking.getUserId(), paymentType);
    }
    public void confirmBooking(String bookingId, String userId,
                               PaymentType paymentType) {
        Objects.requireNonNull(paymentType, "Payment type required");
        synchronized (bookingRepo) {
            Booking booking = requireOwner(bookingId, userId);
            if (booking.getStatus() == BookingStatus.CONFIRMED) return;
            if (booking.getStatus() != BookingStatus.CREATED) {
                throw new IllegalStateException("Booking is not CREATED");
            }
            List<String> keys = keys(booking);
            for (String key : keys) {
                if (bookingRepo.isBooked(key)
                    || !lockProvider.isLockedBy(key, bookingId)) {
                    booking.setStatus(BookingStatus.FAILED);
                    release(booking);
                    throw new SeatNotAvailableException("Seat unavailable");
                }
            }
            booking.setPaymentType(paymentType);
            try {
                boolean paid = payments.apply(paymentType).pay(booking);
                if (!paid) {
                    booking.setStatus(BookingStatus.FAILED);
                    return;
                }
                bookingRepo.markBooked(keys);
                booking.setStatus(BookingStatus.CONFIRMED);
            } catch (RuntimeException e) {
                booking.setStatus(BookingStatus.FAILED);
                throw e;
            } finally {
                release(booking);
            }
        }
    }
    public void cancelBooking(String bookingId, String userId) {
        synchronized (bookingRepo) {
            Booking booking = requireOwner(bookingId, userId);
            if (booking.getStatus() != BookingStatus.CREATED) {
                throw new IllegalStateException("Only CREATED can be cancelled");
            }
            booking.setStatus(BookingStatus.CANCELLED);
            release(booking);
        }
    }
    private Booking requireOwner(String id, String userId) {
        Booking booking = bookingRepo.get(id);
        if (booking == null || !booking.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unknown booking or wrong user");
        }
        return booking;
    }
    private List<String> keys(Booking booking) {
        return booking.getSeatIds().stream()
            .map(id -> key(booking.getShowId(), id)).toList();
    }
    private void release(Booking booking) {
        for (String key : keys(booking)) {
            lockProvider.unlock(key, booking.getBookingId());
        }
    }
    private String key(String showId, String seatId) {
        if (showId == null || showId.isBlank() || showId.contains(":")) {
            throw new IllegalArgumentException("Invalid show ID");
        }
        return showId + ":" + seatId;
    }
}
