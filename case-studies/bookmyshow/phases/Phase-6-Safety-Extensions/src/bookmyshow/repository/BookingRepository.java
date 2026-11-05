package bookmyshow.repository;

import bookmyshow.model.Booking;
import java.util.*;

public class BookingRepository {
    private final Map<String, Booking> map = new LinkedHashMap<>();
    private final Set<String> bookedSeats = new HashSet<>();
    public synchronized void save(Booking booking) {
        map.put(booking.getBookingId(), booking);
    }
    public synchronized Booking get(String id) { return map.get(id); }
    public synchronized boolean isBooked(String key) {
        return bookedSeats.contains(key);
    }
    public synchronized void markBooked(List<String> keys) {
        bookedSeats.addAll(keys);
    }
}
