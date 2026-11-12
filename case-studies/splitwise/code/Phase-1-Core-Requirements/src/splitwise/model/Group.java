package splitwise.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

// Owns its members, its expenses and one net balance per member (positive = is owed, negative = owes).
// Every change adds and subtracts the same cents, so the balances always sum to 0.
public class Group {
    private final String id;
    private final Set<String> members;
    private final Map<String, Expense> expenses = new LinkedHashMap<>();
    private final Map<String, Long> balances = new LinkedHashMap<>();

    public Group(String id, Collection<String> members) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("invalid group id");
        if (members == null) throw new IllegalArgumentException("members required");
        Set<String> unique = new LinkedHashSet<>();
        for (String m : members) {
            if (m == null || m.isBlank()) throw new IllegalArgumentException("invalid member");
            if (!unique.add(m)) throw new IllegalArgumentException("duplicate member " + m);
        }
        if (unique.size() < 2) throw new IllegalArgumentException("a group needs at least 2 members");
        this.id = id;
        this.members = Collections.unmodifiableSet(unique);
        for (String m : unique) balances.put(m, 0L);
    }

    public String getId() { return id; }
    public Set<String> getMembers() { return members; }
    public boolean isMember(String userId) { return members.contains(userId); }
    public boolean hasExpense(String expenseId) { return expenses.containsKey(expenseId); }

    // Req 2: the payer is credited the full amount, each participant is debited their share (payer included).
    public void addExpense(Expense expense) {
        expenses.put(expense.getId(), expense);
        add(expense.getPaidBy(), expense.getAmountCents());
        for (Split split : expense.getSplits()) add(split.getUserId(), -split.getAmountCents());
    }

    // Req 4: paying back moves the debtor up and the creditor down by the same amount.
    public void recordPayment(String from, String to, long amountCents) {
        add(from, amountCents);
        add(to, -amountCents);
    }

    public Map<String, Long> getBalances() { return Collections.unmodifiableMap(new LinkedHashMap<>(balances)); }

    private void add(String userId, long delta) { balances.merge(userId, delta, Long::sum); }
}
