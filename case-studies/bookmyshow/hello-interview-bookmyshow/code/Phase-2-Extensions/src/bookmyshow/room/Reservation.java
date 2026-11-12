package bookmyshow.room;

public final class Reservation {
    private final String id;
    private final String userId;
    private final TimeSlot slot;

    public Reservation(String id, String userId, TimeSlot slot) {
        if (id == null || userId == null || userId.isBlank() || slot == null) {
            throw new IllegalArgumentException("invalid reservation");
        }
        this.id = id;
        this.userId = userId;
        this.slot = slot;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public TimeSlot getSlot() { return slot; }
}
