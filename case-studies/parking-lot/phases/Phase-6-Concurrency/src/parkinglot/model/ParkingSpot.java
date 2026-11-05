package parkinglot.model;

import parkinglot.enums.VehicleType;

import java.util.concurrent.atomic.AtomicBoolean;

public class ParkingSpot {
    private final String id;
    private final VehicleType allowedType;
    private final AtomicBoolean occupied = new AtomicBoolean(false);

    public ParkingSpot(String id, VehicleType allowedType) {
        this.id = id;
        this.allowedType = allowedType;
    }

    // CAS: only one thread can flip false -> true, so a spot is never double-booked.
    public boolean tryOccupy() {
        return occupied.compareAndSet(false, true);
    }

    public void vacate() { occupied.set(false); }

    public boolean isOccupied() { return occupied.get(); }
    public String getId() { return id; }
    public VehicleType getAllowedType() { return allowedType; }
}
