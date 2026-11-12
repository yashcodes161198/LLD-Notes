package amazonlocker.locker;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import amazonlocker.model.AccessToken;
import amazonlocker.model.Compartment;
import amazonlocker.model.Size;

// Orchestrator: finds compartments, issues codes, validates pickups.
public class Locker {
    static final Duration TOKEN_TTL = Duration.ofDays(7);   // Req 4
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O, 1/I
    private static final int CODE_LENGTH = 6;

    private final List<Compartment> compartments;                          // Req 1
    private final Map<String, AccessToken> accessTokensMapping = new HashMap<>(); // Req 3: code -> live token
    private final Set<String> expiredCodes = new HashSet<>();              // only for the "expired" message (TRICKY #6)
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public Locker(List<Compartment> compartments) {
        this(compartments, Clock.systemUTC());
    }

    public Locker(List<Compartment> compartments, Clock clock) {
        if (compartments == null || compartments.isEmpty()) throw new IllegalArgumentException("compartments required");
        if (clock == null) throw new IllegalArgumentException("clock required");
        Set<String> ids = new HashSet<>();
        for (Compartment c : compartments) {
            if (c == null) throw new IllegalArgumentException("null compartment");
            if (!ids.add(c.getId())) throw new IllegalArgumentException("duplicate compartment id " + c.getId());
        }
        this.compartments = List.copyOf(compartments);
        this.clock = clock;
    }

    // Req 1 + 2: returns the code; the parent service sends it to the customer.
    public String depositPackage(Size size) {
        if (size == null) throw new IllegalArgumentException("size required");
        Compartment compartment = getAvailableCompartment(size);
        if (compartment == null) throw new NoAvailableCompartmentException(size);
        compartment.open();
        compartment.markOccupied();
        AccessToken accessToken = generateAccessToken(compartment);
        String code = accessToken.getCode();
        accessTokensMapping.put(code, accessToken);
        return code;
    }

    // Req 3. Every check runs before any state changes.
    public void pickup(String code) {
        if (code == null || code.isBlank()) throw new InvalidAccessCodeException();
        AccessToken accessToken = accessTokensMapping.get(code);
        if (accessToken == null) {
            if (expiredCodes.contains(code)) throw new AccessCodeExpiredException();
            throw new InvalidAccessCodeException();
        }
        // Req 4: do NOT free the compartment, the package is still inside waiting for staff.
        if (accessToken.isExpired(clock.instant())) throw new AccessCodeExpiredException();

        Compartment compartment = accessToken.getCompartment();
        compartment.open();
        compartment.markFree();          // assumption: door auto-closes and the customer took the package
        accessTokensMapping.remove(code);
    }

    // Req 5. Returns the ids it opened. Expired tokens leave the live map, so a later run
    // can never reopen a compartment that was reused for a new package (TRICKY #6).
    public List<String> openExpiredCompartments() {
        List<String> opened = new ArrayList<>();
        Iterator<Map.Entry<String, AccessToken>> it = accessTokensMapping.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, AccessToken> entry = it.next();
            AccessToken accessToken = entry.getValue();
            if (!accessToken.isExpired(clock.instant())) continue;
            Compartment compartment = accessToken.getCompartment();
            compartment.open();
            compartment.markFree();      // staff physically remove the package
            it.remove();
            expiredCodes.add(entry.getKey());
            opened.add(compartment.getId());
        }
        // shortcut: expiredCodes grows forever; drop codes older than ~3 months with a periodic cleanup.
        return opened;
    }

    private Compartment getAvailableCompartment(Size size) {
        for (Compartment c : compartments) {
            if (c.getSize() == size && !c.isOccupied()) return c;
        }
        return null;
    }

    private AccessToken generateAccessToken(Compartment compartment) {
        String code;
        do {
            code = randomCode();
        } while (accessTokensMapping.containsKey(code) || expiredCodes.contains(code)); // unique among known codes
        return new AccessToken(code, clock.instant().plus(TOKEN_TTL), compartment);
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        return sb.toString();
    }
}
