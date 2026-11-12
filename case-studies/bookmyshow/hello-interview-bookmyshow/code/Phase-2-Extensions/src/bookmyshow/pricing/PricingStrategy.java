package bookmyshow.pricing;

import bookmyshow.model.Seat;

// EXT 1: Strategy. Amounts are in minor units (paise/cents), never floating point.
public interface PricingStrategy {
    long priceOf(Seat seat);
}
