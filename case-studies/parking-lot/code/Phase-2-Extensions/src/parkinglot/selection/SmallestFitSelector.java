package parkinglot.selection;

import java.util.Comparator;
import java.util.List;

import parkinglot.model.ParkingSpot;
import parkinglot.model.VehicleType;

// Phase 1's rule: keep big spots for big vehicles. Smallest size, then lowest floor.
public class SmallestFitSelector implements SpotSelector {
    @Override
    public List<ParkingSpot> candidates(List<ParkingSpot> allSpots, VehicleType type) {
        return allSpots.stream()
                .filter(s -> type.fitsIn(s.getSize()))
                .sorted(Comparator.comparing(ParkingSpot::getSize)
                        .thenComparingInt(ParkingSpot::getFloor)
                        .thenComparing(ParkingSpot::getId))
                .toList();
    }
}
