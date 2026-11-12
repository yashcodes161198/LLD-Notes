package parkinglot.pricing;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import parkinglot.model.VehicleType;

// EXT 1: how a stay is priced. Implementations must be stateless (shared across gates).
public interface PricingStrategy {
    long feeCents(VehicleType type, Instant entry, Instant exit);

    // Every started hour counts; the first hour is always charged.
    static long billableHours(Instant entry, Instant exit) {
        long seconds = Duration.between(entry, exit).getSeconds();
        return Math.max(1, (seconds + 3599) / 3600);
    }

    static void requireAllTypes(Map<VehicleType, Long> cents, String what) {
        if (cents == null) throw new IllegalArgumentException(what + " required");
        for (VehicleType type : VehicleType.values()) {
            Long c = cents.get(type);
            if (c == null || c < 0) throw new IllegalArgumentException("no valid " + what + " for " + type);
        }
    }
}
