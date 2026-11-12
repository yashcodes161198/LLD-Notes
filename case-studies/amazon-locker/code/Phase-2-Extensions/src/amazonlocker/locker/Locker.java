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
import amazonlocker.model.Reservation;
import amazonlocker.model.Size;

// Orchestrator: finds compartments, issues codes, validates pickups.
// Public methods are synchronized so "find + mark" is atomic when two drivers use the kiosk at once.
public class Locker {
    static final Duration TOKEN_TTL = Duration.ofDays(7);            // Req 4
    static final Duration RESERVATION_TTL = Duration.ofMinutes(3);   // EXT 3: abandoned reservations come back
    private static final List<Size> SIZES_IN_ORDER = List.of(Size.SMALL, Size.MEDIUM, Size.LARGE); // EXT 1: explicit, not enum order
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O, 1/I
    private static final int CODE_LENGTH = 6;

    private final List<Compartment> compartments;                          // Req 1
    private final Map<String, AccessToken> accessTokensMapping = new HashMap<>(); // Req 3: code -> live token
    private final Set<String> expiredCodes = new HashSet<>();              // only for the "expired" message (TRICKY #6)
    private final Map<String, Reservation> reservations = new HashMap<>(); // EXT 3: reservationId -> reservation
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

    // Phase 1 API kept: reserve + confirm in one call, for callers without a deposit sensor.
    public synchronized String depositPackage(Size size) {
        return confirmDeposit(reserveCompartment(size));
    }

    // EXT 3, phase 1 of 2: hold a compartment and open it. No code exists yet.
    public synchronized String reserveCompartment(Size size) {
        if (size == null) throw new IllegalArgumentException("size required");
        cancelStaleReservations();
        Compartment compartment = getAvailableCompartment(size);
        if (compartment == null) throw new NoAvailableCompartmentException(size);
        compartment.markReserved();
        compartment.open();
        String reservationId = newUniqueCode(reservations.keySet(), Set.of());
        reservations.put(reservationId, new Reservation(reservationId, compartment, clock.instant().plus(RESERVATION_TTL)));
        return reservationId;
    }

    // EXT 3, phase 2 of 2: the package is in (sensor or driver tap), so now issue the token.
    public synchronized String confirmDeposit(String reservationId) {
        Reservation reservation = reservations.get(reservationId);
        if (reservation == null) throw new InvalidReservationException("unknown id");
        if (reservation.isExpired(clock.instant())) {
            release(reservation);
            throw new InvalidReservationException("timed out, compartment released");
        }
        Compartment compartment = reservation.getCompartment();
        compartment.markOccupied();
        AccessToken accessToken = generateAccessToken(compartment);
        String code = accessToken.getCode();
        accessTokensMapping.put(code, accessToken);
        reservations.remove(reservationId);
        return code;
    }

    // EXT 3: the driver backs out.
    public synchronized void cancelReservation(String reservationId) {
        Reservation reservation = reservations.get(reservationId);
        if (reservation == null) throw new InvalidReservationException("unknown id");
        release(reservation);
    }

    // EXT 3: without this, abandoned reservations leak compartments forever. Also run lazily on every reserve.
    public synchronized List<String> cancelStaleReservations() {
        List<String> released = new ArrayList<>();
        for (Reservation r : List.copyOf(reservations.values())) {
            if (r.isExpired(clock.instant())) {
                release(r);
                released.add(r.getCompartment().getId());
            }
        }
        return released;
    }

    // Req 3. Every check runs before any state changes.
    public synchronized void pickup(String code) {
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

    // Req 5. Expired tokens leave the live map, so a reused compartment is never reopened (TRICKY #6).
    public synchronized List<String> openExpiredCompartments() {
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

    // EXT 2: staff flag a broken compartment. Compartment refuses if it holds a package or a reservation.
    public synchronized void markOutOfService(String compartmentId) {
        find(compartmentId).markOutOfService();
    }

    public synchronized void returnToService(String compartmentId) {
        find(compartmentId).returnToService();
    }

    // EXT 1: exact size first, then larger sizes, never smaller.
    private Compartment getAvailableCompartment(Size requestedSize) {
        for (int i = SIZES_IN_ORDER.indexOf(requestedSize); i < SIZES_IN_ORDER.size(); i++) {
            Size size = SIZES_IN_ORDER.get(i);
            for (Compartment c : compartments) {
                if (c.getSize() == size && c.isAvailable()) return c;   // EXT 2: isAvailable skips OUT_OF_SERVICE
            }
        }
        return null;
    }

    private void release(Reservation reservation) {
        reservation.getCompartment().markFree();
        reservations.remove(reservation.getId());
    }

    private Compartment find(String compartmentId) {
        for (Compartment c : compartments) {
            if (c.getId().equals(compartmentId)) return c;
        }
        throw new IllegalArgumentException("unknown compartment " + compartmentId);
    }

    private AccessToken generateAccessToken(Compartment compartment) {
        String code = newUniqueCode(accessTokensMapping.keySet(), expiredCodes);
        return new AccessToken(code, clock.instant().plus(TOKEN_TTL), compartment);
    }

    private String newUniqueCode(Set<String> taken, Set<String> alsoTaken) {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            code = sb.toString();
        } while (taken.contains(code) || alsoTaken.contains(code));
        return code;
    }
}
