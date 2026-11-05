package parkinglot.service;

import parkinglot.enums.PaymentStatus;
import parkinglot.model.Ticket;
import parkinglot.strategy.payment.PaymentStrategy;

public class PaymentProcessor {
    private final PaymentStrategy strategy;

    public PaymentProcessor(PaymentStrategy strategy) {
        this.strategy = strategy;
    }

    public boolean pay(Ticket ticket, double amount) {
        boolean success = strategy.processPayment(ticket, amount);
        ticket.setPaymentStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        if (!success) System.out.println("  Payment failed for ticket: " + ticket.getTicketId());
        return success;
    }
}
