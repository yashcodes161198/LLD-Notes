package splitwise.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import splitwise.model.Expense;
import splitwise.model.Group;
import splitwise.model.Split;
import splitwise.model.SplitType;
import splitwise.model.Transfer;

// Orchestrator. Money is long cents everywhere: a double would make 3 x 33.33 != 100.00 and the ledger drift.
public class SplitwiseService {
    private static final long FULL_PERCENT_BP = 10_000; // 100% in basis points, so 33.33% is exact

    private final Map<String, Group> groups = new HashMap<>();

    // Req 1
    public Group createGroup(String groupId, Collection<String> members) {
        if (groupId != null && groups.containsKey(groupId)) throw new IllegalArgumentException("group exists: " + groupId);
        Group group = new Group(groupId, members); // validates id and members
        groups.put(groupId, group);
        return group;
    }

    // Req 2. Validates everything, builds the Expense, and only then touches the ledger.
    public Expense addExpense(String groupId, String expenseId, String paidBy, long amountCents,
                              SplitType type, List<String> participants, List<Long> values) {
        Group group = requireGroup(groupId);
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
        if (group.hasExpense(expenseId)) throw new DuplicateExpenseException(expenseId);

        List<Split> splits = computeSplits(type, amountCents, participants, values == null ? List.of() : values);
        Expense expense = new Expense(expenseId, paidBy, amountCents, type, splits);
        group.addExpense(expense);
        return expense;
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

    // ---- Req 2 split rules ----

    private List<Split> computeSplits(SplitType type, long amountCents, List<String> participants, List<Long> values) {
        switch (type) {
            case EQUAL:
                if (!values.isEmpty()) throw new InvalidSplitException("EQUAL takes no values");
                return proportional(amountCents, participants, nCopies(participants.size()));
            case EXACT:
                requireValues(participants, values);
                if (sum(values) != amountCents) {
                    throw new InvalidSplitException("EXACT shares sum to " + sum(values) + ", not " + amountCents);
                }
                List<Split> splits = new ArrayList<>();
                for (int i = 0; i < participants.size(); i++) splits.add(new Split(participants.get(i), values.get(i)));
                return splits;
            case PERCENT:
                requireValues(participants, values);
                if (sum(values) != FULL_PERCENT_BP) {
                    throw new InvalidSplitException("PERCENT must total 10000 bp, got " + sum(values));
                }
                return proportional(amountCents, participants, values);
            default:
                throw new IllegalArgumentException("unsupported split type " + type);
        }
    }

    // Floor each share, then hand the leftover cents (fewer than n) one each to the first participants.
    // Without this, 100 cents / 3 = 33+33+33 and one cent vanishes from the ledger.
    private static List<Split> proportional(long amountCents, List<String> participants, List<Long> weights) {
        long total = sum(weights);
        long[] shares = new long[participants.size()];
        long assigned = 0;
        for (int i = 0; i < shares.length; i++) {
            shares[i] = amountCents * weights.get(i) / total;
            assigned += shares[i];
        }
        for (int i = 0; assigned < amountCents; i++, assigned++) shares[i]++;
        List<Split> splits = new ArrayList<>();
        for (int i = 0; i < shares.length; i++) splits.add(new Split(participants.get(i), shares[i]));
        return splits;
    }

    private static void requireValues(List<String> participants, List<Long> values) {
        if (values.size() != participants.size()) {
            throw new InvalidSplitException(values.size() + " values for " + participants.size() + " participants");
        }
        for (Long v : values) {
            if (v == null || v < 0) throw new InvalidSplitException("split values must be >= 0");
        }
    }

    private static List<Long> nCopies(int n) { return Collections.nCopies(n, 1L); }

    private static long sum(List<Long> values) {
        long s = 0;
        for (long v : values) s += v;
        return s;
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
