package splitwise.service;

import splitwise.enums.SplitType;
import splitwise.factory.SplitStrategyFactory;
import splitwise.model.*;
import splitwise.strategy.SplitStrategy;

import java.util.List;
import java.util.Map;

public class ExpenseService {
    private final BalanceSheetService balanceSheetService;

    public ExpenseService(BalanceSheetService balanceSheetService) {
        this.balanceSheetService = balanceSheetService;
    }

    public void addExpense(Group group, String description, double amount, User paidBy,
                           List<User> participants, SplitType splitType, Map<User, Double> metadata) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (participants.isEmpty()) throw new IllegalArgumentException("An expense needs participants");
        requireMember(group, paidBy);
        participants.forEach(u -> requireMember(group, u));

        SplitStrategy strategy = SplitStrategyFactory.getStrategy(splitType);
        List<Split> splits = strategy.split(amount, participants, metadata);

        // One guard for every strategy, present and future: the shares must cover the bill.
        double covered = splits.stream().mapToDouble(Split::getAmount).sum();
        if (Math.abs(covered - amount) > 0.005)
            throw new IllegalStateException(splitType + " split covers " + covered + " of " + amount);

        Expense expense = new Expense(description, amount, paidBy, splits, splitType);
        group.addExpense(expense);

        balanceSheetService.updateBalances(group, paidBy, splits);
    }

    private void requireMember(Group group, User user) {
        if (group.getBalanceSheet(user) == null)
            throw new IllegalArgumentException(user.getName() + " is not in " + group.getName());
    }
}
