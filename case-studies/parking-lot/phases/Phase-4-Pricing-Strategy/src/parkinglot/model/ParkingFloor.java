package parkinglot.model;

import parkinglot.enums.VehicleType;

import java.util.LinkedHashMap;
import java.util.Map;

public class ParkingFloor {
    private final String id;
    private final Map<String, ParkingSpot> spots = new LinkedHashMap<>();

    public ParkingFloor(String id) { this.id = id; }

    public void addSpot(ParkingSpot spot) {
        spots.put(spot.getId(), spot);
    }

    // Finds AND reserves a spot in one step, so callers never hold an unreserved spot.
    public ParkingSpot findAvailableSpot(VehicleType vehicleType) {
        for (ParkingSpot spot : spots.values()) {
            if (spot.getAllowedType() == vehicleType && spot.tryOccupy()) {
                return spot;
            }
        }
        return null;
    }

    public String getId() { return id; }
    public Map<String, ParkingSpot> getSpots() { return spots; }
}
