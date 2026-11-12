package parkinglot.pricing;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

import parkinglot.model.VehicleType;

// Event days: one price per vehicle, however long it stays.
public class FlatRatePricing implements PricingStrategy {
    private final Map<VehicleType, Long> flatCents;

    public FlatRatePricing(Map<VehicleType, Long> flatCents) {
        PricingStrategy.requireAllTypes(flatCents, "flat rate");
        this.flatCents = new EnumMap<>(flatCents);
    }

    @Override
    public long feeCents(VehicleType type, Instant entry, Instant exit) {
        return flatCents.get(type);
    }
}
