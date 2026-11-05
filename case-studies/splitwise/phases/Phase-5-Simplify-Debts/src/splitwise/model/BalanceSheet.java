package splitwise.model;

import java.util.LinkedHashMap;
import java.util.Map;

// One user's ledger inside one group.
public class BalanceSheet {
    private double totalPaid = 0.0;     // money this user actually paid out
    private double totalExpense = 0.0;  // this user's own share of all expenses
    // other user -> amount. Positive: they owe me. Negative: I owe them.
    // LinkedHashMap (not HashMap) only so the printed order is stable run to run.
    private final Map<User, Double> balances = new LinkedHashMap<>();

    public void addTotalPaid(double amount) {
        this.totalPaid += amount;
    }

    public void addTotalExpense(double amount) {
        this.totalExpense += amount;
    }

    public void addBalance(User other, double amount) {
        balances.put(other, balances.getOrDefault(other, 0.0) + amount);
        if (Math.abs(balances.get(other)) < 1e-6) balances.remove(other);
    }

    public void clearBalances() {
        balances.clear();
    }

    public double getTotalPaid() { return totalPaid; }
    public double getTotalExpense() { return totalExpense; }
    public Map<User, Double> getBalances() { return balances; }

    public void print(User me) {
        double youOwe = 0.0, youGetBack = 0.0;
        for (double amount : balances.values()) {
            if (amount < 0) youOwe += -amount;
            else youGetBack += amount;
        }

        System.out.println("Balance sheet of " + me.getName());
        System.out.printf("  Paid: %.2f  Expense: %.2f  Owe: %.2f  Get back: %.2f%n",
                totalPaid, totalExpense, youOwe, youGetBack);
        for (Map.Entry<User, Double> entry : balances.entrySet()) {
            double amount = entry.getValue();
            if (amount > 0)
                System.out.printf("    gets %.2f from %s%n", amount, entry.getKey().getName());
            else
                System.out.printf("    owes %.2f to %s%n", -amount, entry.getKey().getName());
        }
    }
}
