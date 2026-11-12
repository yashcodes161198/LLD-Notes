package parkinglot.model;

// Physical spot. Floor is just an attribute here: a Floor class would hold no behaviour.
public class ParkingSpot {
    private final String id;
    private final int floor;
    private final SpotSize size;
    private boolean occupied;

    public ParkingSpot(String id, int floor, SpotSize size) {
        if (id == null || id.isBlank() || floor < 0 || size == null) {
            throw new IllegalArgumentException("invalid spot");
        }
        this.id = id;
        this.floor = floor;
        this.size = size;
    }

    public boolean isFree() { return !occupied; }

    public void occupy() {
        if (occupied) throw new IllegalStateException("spot " + id + " already occupied");
        occupied = true;
    }

    // Second line of defence: freeing a free spot is a bug, not a no-op.
    public void vacate() {
        if (!occupied) throw new IllegalStateException("spot " + id + " already free");
        occupied = false;
    }

    public String getId() { return id; }
    public int getFloor() { return floor; }
    public SpotSize getSize() { return size; }

    @Override
    public String toString() { return id + "(F" + floor + ", " + size + ")"; }
}
