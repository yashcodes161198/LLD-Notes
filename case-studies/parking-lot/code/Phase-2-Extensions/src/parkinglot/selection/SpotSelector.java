package parkinglot.selection;

import java.util.List;

import parkinglot.model.ParkingSpot;
import parkinglot.model.VehicleType;

// EXT 2: which spots to try, best first. Pure: the lot does the claiming, so a selector can't break atomicity.
public interface SpotSelector {
    List<ParkingSpot> candidates(List<ParkingSpot> allSpots, VehicleType type);
}
