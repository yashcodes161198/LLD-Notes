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

    public Seat getSeat(String seatId) {
        Seat seat = seats.get(seatId);
        if (seat == null) throw new IllegalArgumentException("invalid seat: " + seatId);
        return seat;
    }

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
    // EXT 2: returns the seats actually freed, for the listeners.
    public synchronized List<String> release(Booking booking) {
        List<String> freed = new ArrayList<>();
        for (String seatId : booking.getSeatIds()) {
            if (seatHolders.remove(seatId, booking)) freed.add(seatId);
        }
        return freed;
    }

    // EXT 2: the sweeper walks the SEAT MAP (the truth), never a booking's seat list.
    public synchronized List<String> removeExpiredHolders(Instant now) {
        List<String> freed = new ArrayList<>();
        seatHolders.entrySet().removeIf(e -> {
            boolean expired = e.getValue().isExpired(now);
            if (expired) freed.add(e.getKey());
            return expired;
        });
        return freed;
    }

    private boolean isFree(String seatId, Instant now) {
        Booking holder = seatHolders.get(seatId);
        return holder == null || !holder.isActive(now);
    }
}
