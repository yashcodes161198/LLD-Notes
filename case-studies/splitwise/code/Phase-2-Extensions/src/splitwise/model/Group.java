package splitwise.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

// Owns its members, its expenses and one net balance per member (positive = is owed, negative = owes).
// Every change adds and subtracts the same cents, so the balances always sum to 0.
// EXT 3: every ledger method is synchronized on the group, so the duplicate check and the update are one step
// and readers never see half an expense. Different groups never block each other.
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
    public Set<String> getMembers() { return members; } // immutable, safe without the lock
    public boolean isMember(String userId) { return members.contains(userId); }

    // Req 2. Returns false (and changes nothing) if the id is already there.
    public synchronized boolean addExpense(Expense expense) {
        if (expenses.containsKey(expense.getId())) return false;
        expenses.put(expense.getId(), expense);
        apply(expense, 1);
        return true;
    }

    // EXT 2: undo exactly what addExpense did. Returns null if there is nothing to delete.
    public synchronized Expense removeExpense(String expenseId) {
        Expense old = expenses.remove(expenseId);
        if (old != null) apply(old, -1);
        return old;
    }

    // EXT 2: reverse the old version and apply the new one in one step. Returns null if there is no old version.
    public synchronized Expense replaceExpense(Expense updated) {
        Expense old = expenses.get(updated.getId());
        if (old == null) return null;
        apply(old, -1);
        expenses.put(updated.getId(), updated);
        apply(updated, 1);
        return old;
    }

    public synchronized Expense getExpense(String expenseId) { return expenses.get(expenseId); }

    // Req 4: paying back moves the debtor up and the creditor down by the same amount.
    public synchronized void recordPayment(String from, String to, long amountCents) {
        add(from, amountCents);
        add(to, -amountCents);
    }

    public synchronized Map<String, Long> getBalances() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(balances));
    }

    // The payer is credited the full amount, each participant is debited their share (payer included).
    private void apply(Expense expense, int sign) {
        add(expense.getPaidBy(), sign * expense.getAmountCents());
        for (Split split : expense.getSplits()) add(split.getUserId(), -sign * split.getAmountCents());
    }

    private void add(String userId, long delta) { balances.merge(userId, delta, Long::sum); }
}
