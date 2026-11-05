package parkinglot.model;

import parkinglot.enums.VehicleType;

public class Truck extends Vehicle {
    public Truck(String number) { super(number, VehicleType.TRUCK); }
}
