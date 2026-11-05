package splitwise.model;

import java.util.ArrayList;
import java.util.List;

public class Group {
    private final String id;
    private final String name;
    private final List<User> members = new ArrayList<>();
    private final List<Expense> expenses = new ArrayList<>();

    public Group(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public void addMember(User user) {
        members.add(user);
    }

    public void addExpense(Expense expense) {
        expenses.add(expense);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<User> getMembers() { return members; }
    public List<Expense> getExpenses() { return expenses; }
}
