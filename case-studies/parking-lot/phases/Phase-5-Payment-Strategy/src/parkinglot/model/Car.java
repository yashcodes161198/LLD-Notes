package parkinglot.model;

import parkinglot.enums.VehicleType;

public class Car extends Vehicle {
    public Car(String number) { super(number, VehicleType.CAR); }
}
