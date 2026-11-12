package amazonlocker.model;

import java.time.Instant;

// EXT 3: a compartment held for a driver between "door opened" and "package confirmed".
public final class Reservation {
    private final String id;
    private final Compartment compartment;
    private final Instant expiresAt;

    public Reservation(String id, Compartment compartment, Instant expiresAt) {
        if (id == null || id.isBlank() || compartment == null || expiresAt == null) {
            throw new IllegalArgumentException("invalid reservation");
        }
        this.id = id;
        this.compartment = compartment;
        this.expiresAt = expiresAt;
    }

    public String getId() { return id; }
    public Compartment getCompartment() { return compartment; }
    public boolean isExpired(Instant now) { return now.isAfter(expiresAt); }
}
