package parkinglot.pricing;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

import parkinglot.model.VehicleType;

// Phase 1's rule, now one strategy among several.
public class HourlyPricing implements PricingStrategy {
    private final Map<VehicleType, Long> hourlyCents;

    public HourlyPricing(Map<VehicleType, Long> hourlyCents) {
        PricingStrategy.requireAllTypes(hourlyCents, "hourly rate");
        this.hourlyCents = new EnumMap<>(hourlyCents);
    }

    @Override
    public long feeCents(VehicleType type, Instant entry, Instant exit) {
        return PricingStrategy.billableHours(entry, exit) * hourlyCents.get(type);
    }
}
