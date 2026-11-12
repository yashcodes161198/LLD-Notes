package bookmyshow.model;

// Physical seat, shared by every show on the screen. No booked flag: that's per show (lives on Show).
public final class Seat {
    private final String id;
    private final SeatType type;

    public Seat(String id, SeatType type) {
        if (id == null || id.isBlank() || type == null) throw new IllegalArgumentException("invalid seat");
        this.id = id;
        this.type = type;
    }

    public String getId() { return id; }
    public SeatType getType() { return type; }

    @Override
    public String toString() { return id; }
}
