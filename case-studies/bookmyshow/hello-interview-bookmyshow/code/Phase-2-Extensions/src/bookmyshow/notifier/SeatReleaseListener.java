package bookmyshow.notifier;

import java.util.List;

// EXT 2: Observer. Called outside the show's lock, with the seats that actually came back on sale.
public interface SeatReleaseListener {
    void onSeatsReleased(String showId, List<String> seatIds);
}
