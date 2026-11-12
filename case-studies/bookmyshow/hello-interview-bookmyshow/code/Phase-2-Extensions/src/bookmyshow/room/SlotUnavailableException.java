package bookmyshow.room;

public class SlotUnavailableException extends RuntimeException {
    public SlotUnavailableException(String roomId, TimeSlot slot) {
        super("room " + roomId + " is taken during " + slot);
    }
}
