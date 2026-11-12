package parkinglot.selection;

import java.util.Comparator;
import java.util.List;

import parkinglot.model.ParkingSpot;
import parkinglot.model.VehicleType;

// Nearest to a ground-level entrance: lowest floor first, size only breaks ties.
public class LowestFloorSelector implements SpotSelector {
    @Override
    public List<ParkingSpot> candidates(List<ParkingSpot> allSpots, VehicleType type) {
        return allSpots.stream()
                .filter(s -> type.fitsIn(s.getSize()))
                .sorted(Comparator.comparingInt(ParkingSpot::getFloor)
                        .thenComparing(ParkingSpot::getSize)
                        .thenComparing(ParkingSpot::getId))
                .toList();
    }
}
