package bookmyshow.model;

import bookmyshow.enums.SeatType;

public class RegularSeat extends Seat {
    public RegularSeat(String id, double price) { super(id, price); }
    @Override
    public SeatType getType() { return SeatType.REGULAR; }
}
