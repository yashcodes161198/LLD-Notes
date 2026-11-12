package splitwise.service;

// An expense with this id is already in the group (a retried request must not count twice).
public class DuplicateExpenseException extends RuntimeException {
    public DuplicateExpenseException(String message) { super(message); }
}
