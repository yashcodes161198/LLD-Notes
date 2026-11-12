package amazonlocker.model;

import amazonlocker.hardware.DoorActuator;

// A physical slot. It owns its status and refuses illegal transitions (Information Expert).
public class Compartment {
    private final String id;
    private final Size size;
    private final DoorActuator door;
    private CompartmentStatus status = CompartmentStatus.AVAILABLE;

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
    public CompartmentStatus getStatus() { return status; }

    public boolean isAvailable() { return status == CompartmentStatus.AVAILABLE; }
    public boolean isOccupied() { return status == CompartmentStatus.OCCUPIED; }

    public void markReserved() { move(CompartmentStatus.AVAILABLE, CompartmentStatus.RESERVED); }      // EXT 3
    public void markOccupied() { move(CompartmentStatus.RESERVED, CompartmentStatus.OCCUPIED); }       // EXT 3: only after confirm
    public void returnToService() { move(CompartmentStatus.OUT_OF_SERVICE, CompartmentStatus.AVAILABLE); } // EXT 2

    // Pickup, staff clear-out, or a cancelled reservation.
    public void markFree() {
        if (status != CompartmentStatus.OCCUPIED && status != CompartmentStatus.RESERVED) throw illegal("free");
        status = CompartmentStatus.AVAILABLE;
    }

    // EXT 2: only an empty compartment can be taken out of service, so no live token is ever orphaned.
    public void markOutOfService() {
        if (status != CompartmentStatus.AVAILABLE) throw illegal("take out of service");
        status = CompartmentStatus.OUT_OF_SERVICE;
    }

    public void open() { door.open(id); }

    private void move(CompartmentStatus from, CompartmentStatus to) {
        if (status != from) throw illegal("move to " + to);
        status = to;
    }

    private IllegalStateException illegal(String action) {
        return new IllegalStateException("cannot " + action + " compartment " + id + " while " + status);
    }

    @Override
    public String toString() { return id + "(" + size + ", " + status + ")"; }
}
