package splitwise.model;

import splitwise.enums.SplitType;

import java.util.List;

public class Expense {
    private final String description;
    private final double amount;
    private final User paidBy;
    private final List<Split> splits;
    private final SplitType splitType;

    public Expense(String description, double amount, User paidBy, List<Split> splits, SplitType splitType) {
        this.description = description;
        this.amount = amount;
        this.paidBy = paidBy;
        this.splits = splits;
        this.splitType = splitType;
    }

    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public User getPaidBy() { return paidBy; }
    public List<Split> getSplits() { return splits; }
    public SplitType getSplitType() { return splitType; }

    @Override
    public String toString() {
        return String.format("%s: %.2f paid by %s, %s split %s", description, amount, paidBy.getName(), splitType, splits);
    }
}
