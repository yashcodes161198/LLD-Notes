package splitwise.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import splitwise.model.Expense;
import splitwise.model.Group;
import splitwise.model.Split;
import splitwise.model.SplitType;
import splitwise.model.Transfer;
import splitwise.notifier.Notifier;
import splitwise.strategy.EqualSplitStrategy;
import splitwise.strategy.ExactSplitStrategy;
import splitwise.strategy.PercentSplitStrategy;
import splitwise.strategy.SharesSplitStrategy;
import splitwise.strategy.SplitStrategy;

// Orchestrator. Money is long cents everywhere: a double would make 3 x 33.33 != 100.00 and the ledger drift.
// Split rules live in the strategy map (EXT 1); the ledger lock lives in Group (EXT 3).
public class SplitwiseService {
    private final Map<String, Group> groups = new ConcurrentHashMap<>(); // EXT 3
    private final Map<SplitType, SplitStrategy> strategies;
    private final List<Notifier> notifiers;

    // Same call as Phase 1: default strategies, no notifications.
    public SplitwiseService() {
        this(defaultStrategies(), List.of());
    }

    public SplitwiseService(Map<SplitType, SplitStrategy> strategies, List<Notifier> notifiers) {
        if (strategies == null || notifiers == null) throw new IllegalArgumentException("strategies and notifiers required");
        for (SplitType type : SplitType.values()) {
            if (strategies.get(type) == null) throw new IllegalArgumentException("no strategy for split type " + type);
        }
        this.strategies = new EnumMap<>(strategies);
        this.notifiers = List.copyOf(notifiers);
    }

    public static Map<SplitType, SplitStrategy> defaultStrategies() {
        Map<SplitType, SplitStrategy> map = new EnumMap<>(SplitType.class);
        map.put(SplitType.EQUAL, new EqualSplitStrategy());
        map.put(SplitType.EXACT, new ExactSplitStrategy());
        map.put(SplitType.PERCENT, new PercentSplitStrategy());
        map.put(SplitType.SHARES, new SharesSplitStrategy());
        return map;
    }

    // Req 1. putIfAbsent so two concurrent creates of the same id can't both win.
    public Group createGroup(String groupId, Collection<String> members) {
        Group group = new Group(groupId, members); // validates id and members
        if (groups.putIfAbsent(groupId, group) != null) throw new IllegalArgumentException("group exists: " + groupId);
        return group;
    }

    // Req 2. Validates everything and builds the Expense before the ledger is touched.
    public Expense addExpense(String groupId, String expenseId, String paidBy, long amountCents,
                              SplitType type, List<String> participants, List<Long> values) {
        Group group = requireGroup(groupId);
        Expense expense = buildExpense(group, expenseId, paidBy, amountCents, type, participants, values);
        if (!group.addExpense(expense)) throw new DuplicateExpenseException(expenseId); // check + add are atomic
        notifyParticipants(expense, expense.getPaidBy() + " added " + expense.getId());
        return expense;
    }

    // EXT 2: build and validate the new version first, so a bad edit leaves the old one in place.
    public Expense editExpense(String groupId, String expenseId, String paidBy, long amountCents,
                               SplitType type, List<String> participants, List<Long> values) {
        Group group = requireGroup(groupId);
        Expense updated = buildExpense(group, expenseId, paidBy, amountCents, type, participants, values);
        if (group.replaceExpense(updated) == null) throw new ExpenseNotFoundException(expenseId);
        notifyParticipants(updated, updated.getPaidBy() + " edited " + updated.getId());
        return updated;
    }

    // EXT 2: reverses the expense's exact splits. A second delete throws instead of reversing twice.
    public Expense deleteExpense(String groupId, String expenseId) {
        Expense old = requireGroup(groupId).removeExpense(expenseId);
        if (old == null) throw new ExpenseNotFoundException(String.valueOf(expenseId));
        notifyParticipants(old, old.getId() + " was deleted");
        return old;
    }

    // Req 3: net per member. Positive = the group owes them, negative = they owe the group.
    public Map<String, Long> getBalances(String groupId) {
        return requireGroup(groupId).getBalances();
    }

    // Req 4
    public void settleUp(String groupId, String from, String to, long amountCents) {
        Group group = requireGroup(groupId);
        requireMember(group, from);
        requireMember(group, to);
        if (from.equals(to)) throw new IllegalArgumentException("cannot pay yourself");
        if (amountCents <= 0) throw new IllegalArgumentException("amount must be positive");
        group.recordPayment(from, to, amountCents);
    }

    // Req 5: greedy, biggest debtor pays biggest creditor. At most (members - 1) transfers.
    // Not always the true minimum (that is NP-hard), but it is what Splitwise-style apps ship.
    // Works on one consistent snapshot of the balances (EXT 3).
    public List<Transfer> simplifyDebts(String groupId) {
        // (user, |balance|), largest first; ties by user id so the plan is deterministic.
        Comparator<Map.Entry<String, Long>> biggestFirst =
                Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey());
        PriorityQueue<Map.Entry<String, Long>> creditors = new PriorityQueue<>(biggestFirst);
        PriorityQueue<Map.Entry<String, Long>> debtors = new PriorityQueue<>(biggestFirst);
        for (Map.Entry<String, Long> e : getBalances(groupId).entrySet()) {
            if (e.getValue() > 0) creditors.add(Map.entry(e.getKey(), e.getValue()));
            else if (e.getValue() < 0) debtors.add(Map.entry(e.getKey(), -e.getValue()));
        }
        List<Transfer> transfers = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) { // both empty together: balances sum to 0
            Map.Entry<String, Long> creditor = creditors.poll();
            Map.Entry<String, Long> debtor = debtors.poll();
            long paid = Math.min(creditor.getValue(), debtor.getValue());
            transfers.add(new Transfer(debtor.getKey(), creditor.getKey(), paid));
            if (creditor.getValue() > paid) creditors.add(Map.entry(creditor.getKey(), creditor.getValue() - paid));
            if (debtor.getValue() > paid) debtors.add(Map.entry(debtor.getKey(), debtor.getValue() - paid));
        }
        return transfers;
    }

    private Expense buildExpense(Group group, String expenseId, String paidBy, long amountCents,
                                 SplitType type, List<String> participants, List<Long> values) {
        if (amountCents <= 0) throw new IllegalArgumentException("amount must be positive");
        if (type == null) throw new IllegalArgumentException("split type required");
        requireMember(group, paidBy);
        if (participants == null || participants.isEmpty()) throw new IllegalArgumentException("no participants");
        Set<String> seen = new HashSet<>();
        for (String p : participants) {
            requireMember(group, p);
            if (!seen.add(p)) throw new IllegalArgumentException("duplicate participant " + p);
        }
        if (expenseId == null || expenseId.isBlank()) throw new IllegalArgumentException("invalid expense id");
        List<Split> splits = strategies.get(type).split(amountCents, List.copyOf(participants),
                values == null ? List.of() : List.copyOf(values));
        return new Expense(expenseId, paidBy, amountCents, type, splits); // re-checks the sum, even for a custom strategy
    }

    // EXT 4: after the ledger change, so a notifier failure is logged, never rolled back or thrown.
    private void notifyParticipants(Expense expense, String what) {
        for (Split split : expense.getSplits()) {
            if (split.getUserId().equals(expense.getPaidBy())) continue;
            String message = what + ": your share is " + split.getAmountCents();
            for (Notifier notifier : notifiers) {
                try {
                    notifier.notify(split.getUserId(), message);
                } catch (RuntimeException e) {
                    System.err.println("[warn] notify " + split.getUserId() + " failed: " + e.getMessage());
                }
            }
        }
    }

    private Group requireGroup(String groupId) {
        Group group = groupId == null ? null : groups.get(groupId);
        if (group == null) throw new GroupNotFoundException(String.valueOf(groupId));
        return group;
    }

    private static void requireMember(Group group, String userId) {
        if (userId == null || !group.isMember(userId)) {
            throw new NotAGroupMemberException(userId + " is not in group " + group.getId());
        }
    }
}
