package splitwise.service;

import splitwise.model.Group;
import splitwise.model.Split;
import splitwise.model.User;

import java.util.List;

public class BalanceSheetService {

    public void updateBalances(Group group, User paidBy, List<Split> splits) {
        double totalAmount = splits.stream().mapToDouble(Split::getAmount).sum();
        group.getBalanceSheet(paidBy).addTotalPaid(totalAmount);

        for (Split split : splits) {
            User user = split.getUser();
            double amt = split.getAmount();
            group.getBalanceSheet(user).addTotalExpense(amt);
            if (!user.equals(paidBy)) {
                // Two-sided entry: the participant owes the payer, the payer is owed by the participant.
                group.getBalanceSheet(user).addBalance(paidBy, -amt);
                group.getBalanceSheet(paidBy).addBalance(user, amt);
            }
        }
    }
}
