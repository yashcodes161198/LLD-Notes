package bookmyshow;

import bookmyshow.model.*;
import bookmyshow.enums.*;
import bookmyshow.repository.*;
import bookmyshow.service.*;
import bookmyshow.strategy.payment.*;
import bookmyshow.factory.*;
import bookmyshow.strategy.locking.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Main {
    public static void main(String[] args) throws Exception {
        MovieRepository movieRepo = new MovieRepository();
        TheatreRepository theatreRepo = new TheatreRepository();
        ShowRepository showRepo = new ShowRepository();
        MovieService movies = new MovieService(movieRepo);
        TheatreService theatres = new TheatreService(theatreRepo);
        ShowService shows = new ShowService(showRepo, theatreRepo, movieRepo);
        theatres.createTheatre("t1", "PVR Phoenix");
        theatres.addScreen("t1", new Screen("s1"));
        for (int i = 1; i <= 10; i++) {
            Seat seat = i <= 8 ? new RegularSeat("s1-" + i, 150)
                : new ReclinerSeat("s1-" + i, 300);
            theatres.addSeat("t1", "s1", seat);
        }
        movies.createMovie("m1", "Interstellar", 120);
        Date start = Date.from(java.time.Instant.parse("2025-07-17T13:00:00Z"));
        Show show = shows.createShow("show1", "m1", "t1", "s1", start);
        System.out.println("Search: interstellar");
        for (Show found : shows.getShowsByMovieTitle("interstellar")) {
            System.out.println(found.getId() + " | " + found.getMovie().getTitle()
                + " | " + found.getTheatre().getName() + " | "
                + found.getScreen().getId() + " | 18:30-20:30 IST");
        }

        AtomicLong clock = new AtomicLong(1_000);
        try (InMemoryLockProvider locks = new InMemoryLockProvider(clock::get)) {
            BookingRepository bookings = new BookingRepository();
            BookingService service = new BookingService(locks, bookings);
            System.out.println("Demo 2: user1 selects s1-1 and s1-2");
            Booking first = service.createBooking("user1", show,
                List.of("s1-1", "s1-2"));
            service.confirmBooking(first, PaymentType.CARD);
            System.out.println("user1: " + first.getStatus());
            System.out.println("Demo 3: overlapping [3,4] and [4,5]");
            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch startGate = new CountDownLatch(1);
            List<Future<Booking>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                final int n = i;
                results.add(pool.submit(() -> {
                    startGate.await();
                    try {
                        return service.createBooking("user" + (n + 2), show,
                            n == 0 ? List.of("s1-3", "s1-4")
                                   : List.of("s1-4", "s1-5"));
                    } catch (bookmyshow.exception.SeatNotAvailableException e) {
                        return null;
                    }
                }));
            }
            startGate.countDown();
            int winners = 0;
            Booking winner = null;
            for (Future<Booking> result : results) {
                Booking candidate = result.get();
                if (candidate != null) { winners++; winner = candidate; }
            }
            pool.shutdown();
            System.out.println("Winners: " + winners + "; rejected: " + (2 - winners));
            if (winners != 1) throw new AssertionError("Expected one winner");
            service.confirmBooking(winner, PaymentType.CARD);
            System.out.println("Winning booking: " + winner.getStatus());
            System.out.println("Demo 4: user4 waits 6s with a 5s TTL");
            Booking old = service.createBooking("user4", show,
                List.of("s1-6", "s1-7"));
            clock.addAndGet(6_000);
            Booking fresh = service.createBooking("user5", show,
                List.of("s1-6", "s1-7"));
            try {
                service.confirmBooking(old, PaymentType.UPI);
                throw new AssertionError("Expired booking accepted");
            } catch (bookmyshow.exception.SeatNotAvailableException e) {
                System.out.println("user4 rejected: " + e.getMessage());
            }
            service.confirmBooking(fresh, PaymentType.UPI);
            System.out.println("user5: " + fresh.getStatus());
        }
    }

}
