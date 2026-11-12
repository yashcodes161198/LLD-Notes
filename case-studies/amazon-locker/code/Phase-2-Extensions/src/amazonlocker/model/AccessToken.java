package amazonlocker.model;

import java.time.Instant;

// A bearer token: whoever holds the code may open ONE compartment until the expiration.
public final class AccessToken {
    private final String code;
    private final Instant expiration;
    private final Compartment compartment;

    public AccessToken(String code, Instant expiration, Compartment compartment) {
        if (code == null || code.isBlank() || expiration == null || compartment == null) {
            throw new IllegalArgumentException("invalid access token");
        }
        this.code = code;
        this.expiration = expiration;
        this.compartment = compartment;
    }

    public String getCode() { return code; }
    public Instant getExpiration() { return expiration; }
    public Compartment getCompartment() { return compartment; }

    // Req 4. Time is passed in so the Locker's clock is the only source of "now".
    public boolean isExpired(Instant now) { return now.isAfter(expiration); }
}
