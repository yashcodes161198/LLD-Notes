package splitwise.model;

import java.util.*;

public class Group {
    private final String id;
    private final String name;
    private final List<User> members = new ArrayList<>();
    private final List<Expense> expenses = new ArrayList<>();
    private final Map<User, BalanceSheet> balanceSheets = new HashMap<>();

    public Group(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public void addMember(User user) {
        members.add(user);
        balanceSheets.putIfAbsent(user, new BalanceSheet());
    }

    public void addExpense(Expense expense) {
        expenses.add(expense);
    }

    public BalanceSheet getBalanceSheet(User user) {
        return balanceSheets.get(user);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<User> getMembers() { return members; }
    public List<Expense> getExpenses() { return expenses; }
    public Map<User, BalanceSheet> getBalanceSheets() { return balanceSheets; }
}
