package bookmyshow;

import bookmyshow.model.*;
import bookmyshow.enums.*;
import bookmyshow.service.BookingService;
import bookmyshow.repository.BookingRepository;
import bookmyshow.strategy.locking.InMemoryLockProvider;
import bookmyshow.exception.SeatNotAvailableException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public final class SafetyChecks {
    private static int passed;
    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        passed++;
        System.out.println("PASS " + name);
    }
    private static void rejected(Runnable action, String name) {
        try { action.run(); }
        catch (IllegalArgumentException | IllegalStateException
               | SeatNotAvailableException expected) {
            check(true, name);
            return;
        }
        throw new AssertionError(name);
    }
    public static void run(Show show) throws Exception {
        AtomicLong time = new AtomicLong(1_000);
        AtomicInteger charges = new AtomicInteger();
        try (InMemoryLockProvider locks = new InMemoryLockProvider(time::get)) {
            BookingRepository repo = new BookingRepository();
            BookingService service = new BookingService(locks, repo,
                type -> booking -> { charges.incrementAndGet(); return true; });
            Booking b = service.createBooking("u", show, List.of("s1-1"));
            rejected(() -> service.confirmBooking(b.getBookingId(), "other",
                PaymentType.CARD), "wrong caller rejected");
            service.confirmBooking(b, PaymentType.CARD);
            service.confirmBooking(b, PaymentType.CARD);
            check(charges.get() == 1, "confirm retry does not recharge");
            rejected(() -> service.createBooking("v", show, List.of("s1-1")),
                "confirmed seat cannot be sold again");
            rejected(() -> service.createBooking("u", show, List.of()),
                "empty selection rejected");
            rejected(() -> service.createBooking("u", show,
                List.of("s1-2", "s1-2")), "duplicate selection rejected");
            rejected(() -> service.createBooking("u", show, List.of("missing")),
                "unknown seat rejected");
            Booking blocker = service.createBooking("v", show, List.of("s1-3"));
            rejected(() -> service.createBooking("u", show,
                List.of("s1-2", "s1-3")), "overlapping group rejected");
            Booking free = service.createBooking("w", show, List.of("s1-2"));
            check(free.getStatus() == BookingStatus.CREATED,
                "partial acquisition rolled back");
            service.cancelBooking(free.getBookingId(), "w");
            check(free.getStatus() == BookingStatus.CANCELLED,
                "cancellation releases hold");
            Booking old = service.createBooking("same", show, List.of("s1-4"));
            time.addAndGet(5_000);
            Booking fresh = service.createBooking("same", show, List.of("s1-4"));
            rejected(() -> service.confirmBooking(old, PaymentType.CARD),
                "old attempt rejected even for same user");
            check(locks.isLockedBy("show1:s1-4", fresh.getBookingId()),
                "old attempt cannot release the new token");
            service.confirmBooking(fresh, PaymentType.CARD);
            BookingService declines = new BookingService(locks, repo,
                type -> booking -> false);
            Booking fail = declines.createBooking("x", show, List.of("s1-5"));
            declines.confirmBooking(fail, PaymentType.UPI);
            check(fail.getStatus() == BookingStatus.FAILED,
                "payment false produces FAILED");
            Booking retry = service.createBooking("y", show, List.of("s1-5"));
            check(retry.getStatus() == BookingStatus.CREATED,
                "failed payment frees the seat");
            BookingService throwsError = new BookingService(locks, repo,
                type -> booking -> { throw new IllegalStateException("declined"); });
            Booking err = throwsError.createBooking("z", show, List.of("s1-6"));
            rejected(() -> throwsError.confirmBooking(err, PaymentType.CARD),
                "payment exception propagated");
            check(err.getStatus() == BookingStatus.FAILED
                && locks.isLockExpired("show1:s1-6"),
                "payment exception fails booking and releases hold");
        }
        concurrentConfirmation(show);
        expiryDuringPayment(show);
        System.out.println("Safety checks passed: " + passed);
    }
    private static void concurrentConfirmation(Show show) throws Exception {
        AtomicInteger charges = new AtomicInteger();
        try (InMemoryLockProvider locks = new InMemoryLockProvider()) {
            BookingRepository repo = new BookingRepository();
            BookingService a = new BookingService(locks, repo,
                type -> booking -> { charges.incrementAndGet(); return true; });
            BookingService b = new BookingService(locks, repo,
                type -> booking -> { charges.incrementAndGet(); return true; });
            Booking booking = a.createBooking("u", show, List.of("s1-8"));
            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch gate = new CountDownLatch(1);
            try {
                Future<?> one = pool.submit(() -> {
                    await(gate); a.confirmBooking(booking, PaymentType.CARD);
                });
                Future<?> two = pool.submit(() -> {
                    await(gate); b.confirmBooking(booking, PaymentType.CARD);
                });
                gate.countDown(); one.get(); two.get();
                check(charges.get() == 1, "two service instances charge once");
            } finally { pool.shutdownNow(); }
        }
    }
    private static void expiryDuringPayment(Show show) throws Exception {
        AtomicLong time = new AtomicLong(1_000);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        try (InMemoryLockProvider locks = new InMemoryLockProvider(time::get)) {
            BookingRepository repo = new BookingRepository();
            BookingService service = new BookingService(locks, repo,
                type -> booking -> {
                    entered.countDown(); await(finish); return true;
                });
            Booking booking = service.createBooking("u", show, List.of("s1-9"));
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                Future<?> payment = pool.submit(() ->
                    service.confirmBooking(booking, PaymentType.CARD));
                if (!entered.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("Payment did not start");
                }
                time.addAndGet(6_000);
                CountDownLatch attempted = new CountDownLatch(1);
                Future<Boolean> competitor = pool.submit(() -> {
                    attempted.countDown();
                    try {
                        service.createBooking("v", show, List.of("s1-9"));
                        return true;
                    } catch (SeatNotAvailableException expected) {
                        return false;
                    }
                });
                attempted.await(); finish.countDown(); payment.get();
                check(!competitor.get()
                    && booking.getStatus() == BookingStatus.CONFIRMED,
                    "expiry during payment cannot permit a second sale");
            } finally { finish.countDown(); pool.shutdownNow(); }
        }
    }
    private static void await(CountDownLatch gate) {
        try { gate.await(); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
