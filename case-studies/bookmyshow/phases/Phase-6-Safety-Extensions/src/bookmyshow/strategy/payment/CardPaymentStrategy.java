package bookmyshow.strategy.payment;

import bookmyshow.model.Booking;
import java.util.Locale;

public class CardPaymentStrategy implements PaymentStrategy {
    @Override
    public boolean pay(Booking booking) {
        System.out.printf(Locale.ROOT, "Paid INR %.2f via Card%n",
            booking.getAmount());
        return true;
    }
}
