package bookmyshow;

import bookmyshow.model.*;
import bookmyshow.enums.*;
import bookmyshow.repository.*;
import bookmyshow.service.*;
import bookmyshow.strategy.payment.*;
import bookmyshow.factory.*;
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

        Booking booking = new Booking("preview", "user1", show.getId(),
            List.of("s1-1", "s1-9"), BookingStatus.CREATED, null, 450);
        BookingRepository bookings = new BookingRepository();
        bookings.save(booking);
        System.out.println("Stored booking: " + bookings.get("preview").getStatus());
        boolean paid = PaymentStrategyFactory.getStrategy(PaymentType.CARD)
            .pay(booking);
        System.out.println("Payment result: " + paid);
        System.out.println("Status stays CREATED until orchestration exists.");
    }

}
