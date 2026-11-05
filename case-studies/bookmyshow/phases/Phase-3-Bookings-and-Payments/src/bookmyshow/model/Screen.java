package bookmyshow.model;

import java.util.*;

public class Screen {
    private final String id;
    private final Map<String, Seat> seats = new LinkedHashMap<>();
    public Screen(String id) { this.id = id; }
    public String getId() { return id; }
    public void addSeat(Seat seat) { seats.put(seat.getId(), seat); }
    public List<Seat> getSeats() { return new ArrayList<>(seats.values()); }
}
