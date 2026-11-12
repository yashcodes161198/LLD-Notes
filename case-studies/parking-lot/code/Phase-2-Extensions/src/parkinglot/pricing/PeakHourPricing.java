package parkinglot.pricing;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;

import parkinglot.model.VehicleType;

// Each billed hour is priced by the local time it starts at: peak or off-peak.
public class PeakHourPricing implements PricingStrategy {
    private final Map<VehicleType, Long> offPeakCents;
    private final Map<VehicleType, Long> peakCents;
    private final LocalTime peakStart;
    private final LocalTime peakEnd;
    private final ZoneId zone;

    public PeakHourPricing(Map<VehicleType, Long> offPeakCents, Map<VehicleType, Long> peakCents,
                           LocalTime peakStart, LocalTime peakEnd, ZoneId zone) {
        PricingStrategy.requireAllTypes(offPeakCents, "off-peak rate");
        PricingStrategy.requireAllTypes(peakCents, "peak rate");
        if (peakStart == null || peakEnd == null || zone == null || !peakStart.isBefore(peakEnd)) {
            throw new IllegalArgumentException("invalid peak window");
        }
        this.offPeakCents = new EnumMap<>(offPeakCents);
        this.peakCents = new EnumMap<>(peakCents);
        this.peakStart = peakStart;
        this.peakEnd = peakEnd;
        this.zone = zone;
    }

    @Override
    public long feeCents(VehicleType type, Instant entry, Instant exit) {
        long total = 0;
        long hours = PricingStrategy.billableHours(entry, exit);
        for (long i = 0; i < hours; i++) {
            LocalTime start = entry.plusSeconds(i * 3600).atZone(zone).toLocalTime();
            boolean peak = !start.isBefore(peakStart) && start.isBefore(peakEnd);
            total += (peak ? peakCents : offPeakCents).get(type);
        }
        return total;
    }
}
