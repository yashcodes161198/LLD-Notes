package parkinglot.model;

import parkinglot.enums.VehicleType;

public class ParkingSpot {
    private final String id;
    private final VehicleType allowedType;
    private boolean occupied = false;

    public ParkingSpot(String id, VehicleType allowedType) {
        this.id = id;
        this.allowedType = allowedType;
    }

    // Check-then-act: correct for one thread, NOT thread-safe (fixed in Phase 6).
    public boolean tryOccupy() {
        if (occupied) return false;
        occupied = true;
        return true;
    }

    public void vacate() { occupied = false; }

    public boolean isOccupied() { return occupied; }
    public String getId() { return id; }
    public VehicleType getAllowedType() { return allowedType; }
}
