package parkinglot.model;

import java.time.Instant;

// Req 2: what the driver gets at the exit gate.
public final class Receipt {
    private final String ticketId;
    private final String licensePlate;
    private final String spotId;
    private final Instant entryTime;
    private final Instant exitTime;
    private final long amountCents;
    private final PaymentMethod method;

    public Receipt(Ticket ticket, Instant exitTime, long amountCents, PaymentMethod method) {
        this.ticketId = ticket.getId();
        this.licensePlate = ticket.getLicensePlate();
        this.spotId = ticket.getSpot().getId();
        this.entryTime = ticket.getEntryTime();
        this.exitTime = exitTime;
        this.amountCents = amountCents;
        this.method = method;
    }

    public String getTicketId() { return ticketId; }
    public String getLicensePlate() { return licensePlate; }
    public String getSpotId() { return spotId; }
    public Instant getEntryTime() { return entryTime; }
    public Instant getExitTime() { return exitTime; }
    public long getAmountCents() { return amountCents; }
    public PaymentMethod getMethod() { return method; }

    @Override
    public String toString() {
        return ticketId + " " + licensePlate + " " + spotId + " paid " + amountCents + "c by " + method;
    }
}
