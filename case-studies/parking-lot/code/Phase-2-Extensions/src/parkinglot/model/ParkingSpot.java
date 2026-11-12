package parkinglot.model;

import java.util.concurrent.atomic.AtomicBoolean;

// Physical spot. Floor is just an attribute here: a Floor class would hold no behaviour.
public class ParkingSpot {
    private final String id;
    private final int floor;
    private final SpotSize size;
    private final AtomicBoolean occupied = new AtomicBoolean(false); // EXT 4: many gates at once

    public ParkingSpot(String id, int floor, SpotSize size) {
        if (id == null || id.isBlank() || floor < 0 || size == null) {
            throw new IllegalArgumentException("invalid spot");
        }
        this.id = id;
        this.floor = floor;
        this.size = size;
    }

    public boolean isFree() { return !occupied.get(); }

    // EXT 4: check-and-take in one CAS, so two gates can never both get this spot.
    public boolean tryOccupy() { return occupied.compareAndSet(false, true); }

    // Second line of defence: freeing a free spot is a bug, not a no-op.
    public void vacate() {
        if (!occupied.compareAndSet(true, false)) throw new IllegalStateException("spot " + id + " already free");
    }

    public String getId() { return id; }
    public int getFloor() { return floor; }
    public SpotSize getSize() { return size; }

    @Override
    public String toString() { return id + "(F" + floor + ", " + size + ")"; }
}
