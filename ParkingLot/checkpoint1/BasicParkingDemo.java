import java.util.*;

public class BasicParkingDemo {
    enum VehicleType { BIKE, CAR }

    record Vehicle(String plate, VehicleType type) {
        Vehicle {
            if (plate == null || plate.isBlank()) throw new IllegalArgumentException("Plate required");
            plate = plate.strip().toUpperCase(Locale.ROOT);
            Objects.requireNonNull(type);
        }
    }

    static final class ParkingSpot {
        final int id;
        final VehicleType type;
        private Vehicle occupant;

        ParkingSpot(int id, VehicleType type) {
            this.id = id;
            this.type = Objects.requireNonNull(type);
        }

        boolean accepts(Vehicle vehicle) { return occupant == null && type == vehicle.type(); }
        void park(Vehicle vehicle) {
            if (!accepts(vehicle)) throw new IllegalStateException("Unavailable or incompatible spot");
            occupant = vehicle;
        }
        void release() { occupant = null; }
    }

    // shortcut: single-threaded, replace plate-based exit with tickets in checkpoint 2.
    static final class ParkingLot {
        private final List<ParkingSpot> spots = List.of(
                new ParkingSpot(1, VehicleType.BIKE), new ParkingSpot(2, VehicleType.CAR));

        int park(Vehicle vehicle) {
            if (spots.stream().anyMatch(s -> s.occupant != null && s.occupant.plate().equals(vehicle.plate())))
                throw new IllegalStateException("Vehicle already parked");
            ParkingSpot spot = spots.stream().filter(s -> s.accepts(vehicle)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("No compatible spot"));
            spot.park(vehicle);
            return spot.id;
        }

        void leave(String plate) {
            if (plate == null || plate.isBlank()) throw new IllegalArgumentException("Plate required");
            String normalized = plate.strip().toUpperCase(Locale.ROOT);
            ParkingSpot spot = spots.stream()
                    .filter(s -> s.occupant != null && s.occupant.plate().equals(normalized))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Vehicle not parked"));
            spot.release();
        }
    }

    public static void main(String[] args) {
        ParkingLot lot = new ParkingLot();
        Vehicle car = new Vehicle(" ka-01-a-100 ", VehicleType.CAR);
        if (lot.park(car) != 2) throw new AssertionError("Wrong spot type");
        lot.leave(car.plate());
        if (lot.park(car) != 2) throw new AssertionError("Spot was not reusable");
        System.out.println("Checkpoint 1 passed: compatible allocation and reuse.");
    }
}
