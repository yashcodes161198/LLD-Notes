package bookmyshow.factory;

import bookmyshow.enums.PaymentType;
import bookmyshow.strategy.payment.*;

public class PaymentStrategyFactory {
    public static PaymentStrategy getStrategy(PaymentType type) {
        return switch (type) {
            case CARD -> new CardPaymentStrategy();
            case UPI -> new UpiPaymentStrategy();
        };
    }
}
