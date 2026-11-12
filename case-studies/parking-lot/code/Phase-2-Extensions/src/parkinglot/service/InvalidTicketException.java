package parkinglot.service;

public class InvalidTicketException extends RuntimeException {
    public InvalidTicketException(String ticketId) {
        super("unknown or already used ticket " + ticketId);
    }
}
