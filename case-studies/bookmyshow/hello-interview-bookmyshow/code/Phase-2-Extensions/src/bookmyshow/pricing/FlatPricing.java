package bookmyshow.pricing;

import bookmyshow.model.Seat;

// Same price for every seat. Default when no pricing is configured (price 0 = Phase 1 behaviour).
public class FlatPricing implements PricingStrategy {
    private final long price;

    public FlatPricing(long price) {
        if (price < 0) throw new IllegalArgumentException("negative price");
        this.price = price;
    }

    @Override
    public long priceOf(Seat seat) { return price; }
}
