package parkinglot.service;

public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String ticketId, Throwable cause) {
        super("payment failed for " + ticketId + ": " + cause.getMessage(), cause);
    }
}
