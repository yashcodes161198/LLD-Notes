package bookmyshow.strategy.payment;

import bookmyshow.model.Booking;

public interface PaymentStrategy {
    boolean pay(Booking booking);
}
