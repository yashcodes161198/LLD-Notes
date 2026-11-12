package parkinglot.model;

import java.time.Instant;

// Immutable record of one stay. Only the lot's active map decides whether it is still valid.
public final class Ticket {
    private final String id;
    private final String licensePlate;
    private final VehicleType vehicleType;
    private final ParkingSpot spot;
    private final Instant entryTime;

    public Ticket(String id, String licensePlate, VehicleType vehicleType, ParkingSpot spot, Instant entryTime) {
        if (id == null || licensePlate == null || vehicleType == null || spot == null || entryTime == null) {
            throw new IllegalArgumentException("invalid ticket");
        }
        this.id = id;
        this.licensePlate = licensePlate;
        this.vehicleType = vehicleType;
        this.spot = spot;
        this.entryTime = entryTime;
    }

    public String getId() { return id; }
    public String getLicensePlate() { return licensePlate; }
    public VehicleType getVehicleType() { return vehicleType; }
    public ParkingSpot getSpot() { return spot; }
    public Instant getEntryTime() { return entryTime; }

    @Override
    public String toString() { return id + "[" + licensePlate + " @ " + spot.getId() + "]"; }
}
