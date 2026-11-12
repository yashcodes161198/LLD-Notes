package parkinglot.service;

import parkinglot.model.VehicleType;

public class LotFullException extends RuntimeException {
    public LotFullException(VehicleType type) {
        super("no free spot for " + type);
    }
}
