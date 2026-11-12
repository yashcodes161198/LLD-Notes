package bookmyshow.room;

import java.time.Instant;

// EXT 3: half-open interval [start, end), so 10:00-11:00 and 11:00-12:00 don't clash.
public final class TimeSlot {
    private final Instant start;
    private final Instant end;

    public TimeSlot(Instant start, Instant end) {
        if (start == null || end == null || !start.isBefore(end)) throw new IllegalArgumentException("invalid time slot");
        this.start = start;
        this.end = end;
    }

    public boolean overlaps(TimeSlot other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    public Instant getStart() { return start; }
    public Instant getEnd() { return end; }

    @Override
    public String toString() { return "[" + start + ", " + end + ")"; }
}
