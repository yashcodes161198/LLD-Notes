package parkinglot.strategy.payment;

import parkinglot.model.Ticket;

public class CardPayment implements PaymentStrategy {
    @Override
    public boolean processPayment(Ticket ticket, double amount) {
        System.out.println("  Paid Rs." + amount + " for ticket " + ticket.getTicketId() + " via Card.");
        return true;
    }
}
