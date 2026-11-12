package splitwise.service;

// EXT 2: edit or delete of an expense id the group doesn't have (or already deleted).
public class ExpenseNotFoundException extends RuntimeException {
    public ExpenseNotFoundException(String message) { super(message); }
}
