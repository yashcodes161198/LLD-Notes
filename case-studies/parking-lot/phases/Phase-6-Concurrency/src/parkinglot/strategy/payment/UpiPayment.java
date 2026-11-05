package parkinglot.strategy.payment;

import parkinglot.model.Ticket;

public class UpiPayment implements PaymentStrategy {
    @Override
    public boolean processPayment(Ticket ticket, double amount) {
        System.out.println("  Paid Rs." + amount + " for ticket " + ticket.getTicketId() + " via UPI.");
        return true;
    }
}
