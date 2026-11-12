package parkinglot.model;

// Req 1: a vehicle fits any spot at least as big as its own size.
public enum VehicleType {
    MOTORCYCLE(SpotSize.SMALL),
    CAR(SpotSize.MEDIUM),
    TRUCK(SpotSize.LARGE);

    private final SpotSize size;

    VehicleType(SpotSize size) { this.size = size; }

    public SpotSize getSize() { return size; }

    public boolean fitsIn(SpotSize spot) { return spot.compareTo(size) >= 0; }
}
