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
    public BookingService(LockProvider lockProvider,
                          BookingRepository bookingRepo) {
        this.lockProvider = lockProvider;
        this.bookingRepo = bookingRepo;
    }
    public Booking createBooking(String userId, Show show,
                                 List<String> seatIds) {
        for (String seatId : seatIds) {
            String key = show.getId() + ":" + seatId;
            if (!lockProvider.tryLock(key, userId, TTL)) {
                throw new SeatNotAvailableException(
                    "Seat " + seatId + " is temporarily unavailable");
            }
        }
        double totalPrice = 0;
        for (Seat seat : show.getSeats()) {
            if (seatIds.contains(seat.getId())) {
                totalPrice += seat.getPrice();
            }
        }
        Booking booking = new Booking("B" + ids.incrementAndGet(),
            userId, show.getId(), seatIds, BookingStatus.CREATED,
            null, totalPrice);
        bookingRepo.save(booking);
        return booking;
    }
    public void confirmBooking(Booking booking, PaymentType paymentType) {
        if (booking.getStatus() != BookingStatus.CREATED) {
            throw new IllegalStateException("Booking is not CREATED");
        }
        for (String seatId : booking.getSeatIds()) {
            String key = booking.getShowId() + ":" + seatId;
            if (lockProvider.isLockExpired(key)
                || !lockProvider.isLockedBy(key, booking.getUserId())) {
                throw new SeatNotAvailableException(
                    "Seat " + seatId + " is temporarily unavailable");
            }
        }
        booking.setPaymentType(paymentType);
        PaymentStrategy strategy =
            PaymentStrategyFactory.getStrategy(paymentType);
        strategy.pay(booking);
        for (String seatId : booking.getSeatIds()) {
            lockProvider.unlock(booking.getShowId() + ":" + seatId);
        }
        booking.setStatus(BookingStatus.CONFIRMED);
    }
}
