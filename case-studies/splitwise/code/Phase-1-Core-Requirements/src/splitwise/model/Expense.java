package splitwise.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Immutable record of who paid and who owes what. The constructor enforces "shares add up to the amount",
// so an expense that would unbalance the ledger can't exist.
public final class Expense {
    private final String id;
    private final String paidBy;
    private final long amountCents;
    private final SplitType type;
    private final List<Split> splits;

    public Expense(String id, String paidBy, long amountCents, SplitType type, List<Split> splits) {
        if (id == null || id.isBlank() || paidBy == null || paidBy.isBlank() || type == null) {
            throw new IllegalArgumentException("invalid expense");
        }
        if (amountCents <= 0) throw new IllegalArgumentException("amount must be positive");
        if (splits == null || splits.isEmpty()) throw new IllegalArgumentException("no splits");
        long sum = 0;
        Set<String> seen = new HashSet<>();
        for (Split split : splits) {
            if (!seen.add(split.getUserId())) throw new IllegalArgumentException("duplicate split for " + split.getUserId());
            sum += split.getAmountCents();
        }
        if (sum != amountCents) throw new IllegalArgumentException("splits sum to " + sum + ", not " + amountCents);
        this.id = id;
        this.paidBy = paidBy;
        this.amountCents = amountCents;
        this.type = type;
        this.splits = List.copyOf(splits);
    }

    public String getId() { return id; }
    public String getPaidBy() { return paidBy; }
    public long getAmountCents() { return amountCents; }
    public SplitType getType() { return type; }
    public List<Split> getSplits() { return splits; }

    @Override
    public String toString() { return id + "(" + paidBy + " paid " + amountCents + ", " + type + " " + splits + ")"; }
}
