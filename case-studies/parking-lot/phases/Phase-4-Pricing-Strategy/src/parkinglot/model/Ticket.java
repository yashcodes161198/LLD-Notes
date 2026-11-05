package parkinglot.model;

import java.time.LocalDateTime;

public class Ticket {
    private final String ticketId;
    private final LocalDateTime entryTime;
    private final Vehicle vehicle;
    private final String floorId;
    private final String spotId;

    private Ticket(Builder b) {
        this.ticketId = b.ticketId;
        this.entryTime = b.entryTime;
        this.vehicle = b.vehicle;
        this.floorId = b.floorId;
        this.spotId = b.spotId;
    }

    public static Builder builder() { return new Builder(); }

    public String getTicketId() { return ticketId; }
    public LocalDateTime getEntryTime() { return entryTime; }
    public Vehicle getVehicle() { return vehicle; }
    public String getFloorId() { return floorId; }
    public String getSpotId() { return spotId; }

    public static class Builder {
        private String ticketId;
        private LocalDateTime entryTime;
        private Vehicle vehicle;
        private String floorId;
        private String spotId;

        public Builder ticketId(String v) { this.ticketId = v; return this; }
        public Builder entryTime(LocalDateTime v) { this.entryTime = v; return this; }
        public Builder vehicle(Vehicle v) { this.vehicle = v; return this; }
        public Builder floorId(String v) { this.floorId = v; return this; }
        public Builder spotId(String v) { this.spotId = v; return this; }

        public Ticket build() {
            if (ticketId == null || vehicle == null || entryTime == null) {
                throw new IllegalStateException("ticketId, vehicle and entryTime are required");
            }
            return new Ticket(this);
        }
    }
}
