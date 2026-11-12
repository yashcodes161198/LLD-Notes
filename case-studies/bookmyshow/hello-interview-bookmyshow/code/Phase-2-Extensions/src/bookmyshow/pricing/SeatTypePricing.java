package bookmyshow.pricing;

import java.util.EnumMap;
import java.util.Map;

import bookmyshow.model.Seat;
import bookmyshow.model.SeatType;

// Price per seat type. Fails at construction if a type has no price.
public class SeatTypePricing implements PricingStrategy {
    private final Map<SeatType, Long> prices;

    public SeatTypePricing(Map<SeatType, Long> prices) {
        if (prices == null) throw new IllegalArgumentException("prices required");
        for (SeatType type : SeatType.values()) {
            Long price = prices.get(type);
            if (price == null || price < 0) throw new IllegalArgumentException("no valid price for " + type);
        }
        this.prices = new EnumMap<>(prices);
    }

    @Override
    public long priceOf(Seat seat) { return prices.get(seat.getType()); }
}
