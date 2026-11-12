package parkinglot.payment;

// The out-of-scope seam: charging is someone else's system.
public interface PaymentProcessor {
    // Throws if the charge is declined or the provider is down.
    void charge(String ticketId, long amountCents);
}
