package amazonlocker.model;

import amazonlocker.hardware.DoorActuator;

// A physical slot. Occupancy is a physical fact, so the compartment owns it (Information Expert).
public class Compartment {
    private final String id;
    private final Size size;
    private final DoorActuator door;
    private boolean occupied;

    public Compartment(String id, Size size, DoorActuator door) {
        if (id == null || id.isBlank() || size == null || door == null) {
            throw new IllegalArgumentException("invalid compartment");
        }
        this.id = id;
        this.size = size;
        this.door = door;
    }

    public String getId() { return id; }
    public Size getSize() { return size; }
    public boolean isOccupied() { return occupied; }

    public void markOccupied() { occupied = true; }
    public void markFree() { occupied = false; }

    public void open() { door.open(id); }

    @Override
    public String toString() { return id + "(" + size + (occupied ? ", occupied" : ", free") + ")"; }
}
