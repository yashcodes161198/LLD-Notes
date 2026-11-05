package bookmyshow.model;

import bookmyshow.enums.SeatType;

public abstract class Seat {
    private final String id;
    private final double price;
    protected Seat(String id, double price) {
        this.id = id;
        this.price = price;
    }
    public String getId() { return id; }
    public double getPrice() { return price; }
    public abstract SeatType getType();
}
