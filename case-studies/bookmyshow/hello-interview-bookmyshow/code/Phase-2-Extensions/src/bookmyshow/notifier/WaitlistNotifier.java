package bookmyshow.notifier;

import java.util.List;

// Tells users on a show's waitlist that seats freed up.
public class WaitlistNotifier implements SeatReleaseListener {
    @Override
    public void onSeatsReleased(String showId, List<String> seatIds) {
        // stub: real code would load the show's waitlist and enqueue a push/email per user; a provider error should not reach the caller.
        System.out.println("[WAITLIST] show " + showId + ": seats " + seatIds + " are available again");
    }
}
