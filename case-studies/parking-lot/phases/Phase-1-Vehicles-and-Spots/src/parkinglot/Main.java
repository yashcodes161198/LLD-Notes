package parkinglot;

import parkinglot.enums.VehicleType;
import parkinglot.model.*;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Phase 1: Vehicles & Parking Spots ===");

        List<ParkingSpot> spots = List.of(
                new ParkingSpot("S1", VehicleType.BIKE),
                new ParkingSpot("S2", VehicleType.CAR),
                new ParkingSpot("S3", VehicleType.TRUCK));

        List<Vehicle> arrivals = List.of(
                new Car("KA01AB1234"), new Bike("KA01XY0001"), new Car("KA05ZZ9999"));

        for (Vehicle v : arrivals) {
            ParkingSpot assigned = null;
            for (ParkingSpot s : spots) {
                if (s.getAllowedType() == v.getType() && s.tryOccupy()) { assigned = s; break; }
            }
            System.out.println("  " + v + " -> " + (assigned == null ? "NO SPOT" : assigned.getId()));
        }

        spots.forEach(s -> System.out.println("  Spot " + s.getId() + " [" + s.getAllowedType() + "] "
                + (s.isOccupied() ? "Occupied" : "Free")));
    }
}
