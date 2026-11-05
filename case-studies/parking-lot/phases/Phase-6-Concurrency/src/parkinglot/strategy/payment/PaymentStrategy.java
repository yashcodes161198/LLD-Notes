package parkinglot.strategy.payment;

import parkinglot.model.Ticket;

public interface PaymentStrategy {
    boolean processPayment(Ticket ticket, double amount);
}
