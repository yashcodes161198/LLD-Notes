package parkinglot.model;

import parkinglot.enums.VehicleType;

public abstract class Vehicle {
    private final String number;
    private final VehicleType type;

    protected Vehicle(String number, VehicleType type) {
        this.number = number;
        this.type = type;
    }

    public String getNumber() { return number; }
    public VehicleType getType() { return type; }

    @Override
    public String toString() { return type + "(" + number + ")"; }
}
