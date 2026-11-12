package parkinglot.service;

public class VehicleAlreadyParkedException extends RuntimeException {
    public VehicleAlreadyParkedException(String licensePlate) {
        super(licensePlate + " is already parked");
    }
}
