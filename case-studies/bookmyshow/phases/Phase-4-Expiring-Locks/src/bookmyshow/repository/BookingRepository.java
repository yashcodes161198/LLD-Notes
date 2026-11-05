package bookmyshow.repository;

import bookmyshow.model.Booking;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BookingRepository {
    private final Map<String, Booking> map = new ConcurrentHashMap<>();
    public void save(Booking booking) {
        map.put(booking.getBookingId(), booking);
    }
    public Booking get(String id) { return map.get(id); }
}
