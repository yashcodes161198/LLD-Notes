package bookmyshow.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import bookmyshow.service.SeatUnavailableException;

// Owns who holds which seat for THIS show, and the lock (its monitor) that guards it.
public class Show {
    private final String id;
    private final Map<String, Seat> seats = new LinkedHashMap<>();       // the layout, in display order
    private final Map<String, Booking> seatHolders = new HashMap<>();    // Req 2, 4: seatId -> holder

    public Show(String id, List<Seat> seats) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("invalid show id");
        if (seats == null || seats.isEmpty()) throw new IllegalArgumentException("show " + id + " has no seats");
        for (Seat seat : seats) {
            if (seat == null || this.seats.putIfAbsent(seat.getId(), seat) != null) {
                throw new IllegalArgumentException("show " + id + " has a null or duplicate seat");
            }
        }
        this.id = id;
    }

    public String getId() { return id; }

    // Req 1: a snapshot; hold() is the real check.
    public synchronized List<Seat> getAvailableSeats(Instant now) {
        List<Seat> free = new ArrayList<>();
        for (Seat seat : seats.values()) {
            if (isFree(seat.getId(), now)) free.add(seat);
        }
        return free;
    }

    // Req 2: check ALL, then write ALL, under the lock, so it's atomic and all-or-nothing.
    public synchronized void hold(Booking booking, Instant now) {
        for (String seatId : booking.getSeatIds()) {
            if (!seats.containsKey(seatId)) throw new IllegalArgumentException("invalid seat: " + seatId);
        }
        for (String seatId : booking.getSeatIds()) {
            if (!isFree(seatId, now)) throw new SeatUnavailableException(seatId);
        }
        for (String seatId : booking.getSeatIds()) {
            seatHolders.put(seatId, booking);
        }
    }

    // Req 5. Release BY OWNER: a seat re-held by a newer booking is left alone (TRICKY #4).
    public synchronized void release(Booking booking) {
        for (String seatId : booking.getSeatIds()) {
            seatHolders.remove(seatId, booking);
        }
    }

    private boolean isFree(String seatId, Instant now) {
        Booking holder = seatHolders.get(seatId);
        return holder == null || !holder.isActive(now);
    }
}
