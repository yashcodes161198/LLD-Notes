package parkinglot.payment;

// Charges the card tapped at the exit terminal.
public class CardPaymentProcessor implements PaymentProcessor {
    @Override
    public void charge(String ticketId, long amountCents) {
        // stub: real code would call the card gateway (e.g. Stripe, Adyen) with an idempotency key = ticketId; a decline should throw.
        System.out.println("[CARD] charged " + amountCents + "c for " + ticketId);
    }
}
