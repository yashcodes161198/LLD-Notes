package splitwise.notifier;

// EXT 4: the seam for telling people about ledger changes. Delivery is out of scope; a failure must not undo the expense.
public interface Notifier {
    void notify(String userId, String message);
}
