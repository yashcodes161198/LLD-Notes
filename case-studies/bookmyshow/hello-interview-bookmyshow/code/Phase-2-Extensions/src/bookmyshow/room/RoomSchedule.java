package bookmyshow.room;

import java.util.ArrayList;
import java.util.List;

// EXT 3: the seat booker's skeleton with a different conflict check.
// Key (show, seat) -> (room, interval); "seat free?" -> "no reservation overlaps". Lock per room.
public class RoomSchedule {
    private final String roomId;
    private final List<Reservation> reservations = new ArrayList<>(); // shortcut: linear scan, fine for one room's day; TreeMap by start if n grows
    private long nextId = 1;

    public RoomSchedule(String roomId) {
        if (roomId == null || roomId.isBlank()) throw new IllegalArgumentException("invalid room id");
        this.roomId = roomId;
    }

    // Check, then write, under the lock: same atomicity as Show.hold.
    public synchronized String reserve(String userId, TimeSlot slot) {
        if (userId == null || userId.isBlank() || slot == null) throw new IllegalArgumentException("invalid request");
        for (Reservation r : reservations) {
            if (r.getSlot().overlaps(slot)) throw new SlotUnavailableException(roomId, slot);
        }
        Reservation reservation = new Reservation(roomId + "-R" + nextId++, userId, slot);
        reservations.add(reservation);
        return reservation.getId();
    }

    // Removes by reservation id, so it can only ever drop its own reservation. Idempotent.
    public synchronized void cancel(String reservationId) {
        reservations.removeIf(r -> r.getId().equals(reservationId));
    }

    public synchronized List<TimeSlot> getReservedSlots() {
        List<TimeSlot> slots = new ArrayList<>();
        for (Reservation r : reservations) slots.add(r.getSlot());
        return slots;
    }
}
