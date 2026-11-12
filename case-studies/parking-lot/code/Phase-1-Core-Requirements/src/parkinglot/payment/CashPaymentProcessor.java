package parkinglot.payment;

// Accepts cash at the exit kiosk.
public class CashPaymentProcessor implements PaymentProcessor {
    @Override
    public void charge(String ticketId, long amountCents) {
        // stub: real code would ask the kiosk's cash unit to collect the amount and dispense change; a timeout should throw.
        System.out.println("[CASH] collected " + amountCents + "c for " + ticketId);
    }
}
