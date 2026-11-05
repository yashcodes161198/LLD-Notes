package bookmyshow;

import bookmyshow.model.*;
import bookmyshow.enums.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Main {
    public static void main(String[] args) throws Exception {
        Theatre theatre = new Theatre("t1", "PVR Phoenix");
        Screen screen = new Screen("s1");
        screen.addSeat(new RegularSeat("s1-1", 150));
        screen.addSeat(new ReclinerSeat("s1-9", 300));
        theatre.addScreen(screen);
        System.out.println(theatre.getName() + " / " + screen.getId());
        for (Seat seat : screen.getSeats()) {
            System.out.printf(Locale.ROOT, "%s %s INR %.2f%n",
                seat.getId(), seat.getType(), seat.getPrice());
        }
        System.out.println("Physical seats have no show-specific status.");
    }

}
