package splitwise;

import splitwise.enums.SplitType;
import splitwise.model.Group;
import splitwise.model.User;
import splitwise.repository.InMemoryGroupRepository;
import splitwise.service.*;

import java.util.List;

public class Main {
    private static final InMemoryGroupRepository repo = new InMemoryGroupRepository();
    private static final GroupService groupService = new GroupService(
            repo, new ExpenseService(new BalanceSheetService()), new DebtSimplificationService());

    public static void main(String[] args) {
        System.out.println("=== Phase 5: Simplify Debts ===");

        // 1. The demo from the video's code walkthrough.
        User shubh = new User("u1", "Shubh"), bob = new User("u2", "Bob"), tom = new User("u3", "Tom");
        String goa = groupService.createGroup("Goa Trip", List.of(shubh, bob, tom));
        groupService.addExpense(goa, "Lunch Day-1", 100, shubh, List.of(shubh, bob), SplitType.EQUAL, null);
        groupService.addExpense(goa, "Lunch Day-2", 100, bob, List.of(bob, tom), SplitType.EQUAL, null);
        simplifyAndShow(goa);
        groupService.printBalances(goa);

        // 2. The Splitwise app demo: flight $120 (Shubh, Jay), cycling $18 (all three, paid by Parth).
        User jay = new User("u4", "Jay"), parth = new User("u5", "Parth");
        String app = groupService.createGroup("App demo", List.of(shubh, jay, parth));
        groupService.addExpense(app, "Flight", 120, shubh, List.of(shubh, jay), SplitType.EQUAL, null);
        groupService.addExpense(app, "Cycling", 18, parth, List.of(shubh, jay, parth), SplitType.EQUAL, null);
        simplifyAndShow(app);

        // 3. The 5-node graph from the video's whiteboard (A..E, 7 edges).
        User a = new User("a", "A"), b = new User("b", "B"), c = new User("c", "C"),
             d = new User("d", "D"), e = new User("e", "E");
        String graph = groupService.createGroup("Graph", List.of(a, b, c, d, e));
        owes(graph, a, b, 300); owes(graph, b, c, 400); owes(graph, c, a, 50); owes(graph, e, a, 50);
        owes(graph, d, b, 100); owes(graph, c, d, 150); owes(graph, d, e, 100);
        simplifyAndShow(graph);

        // 4. Where greedy is not optimal: nets P+40, Q+30, R-30, S-20, T-20.
        User p = new User("p", "P"), q = new User("q", "Q"), r = new User("r", "R"),
             s = new User("s", "S"), t = new User("t", "T");
        String tricky = groupService.createGroup("Greedy edge case", List.of(p, q, r, s, t));
        owes(tricky, r, p, 30); owes(tricky, s, q, 20); owes(tricky, t, q, 10); owes(tricky, t, p, 10);
        simplifyAndShow(tricky);
        System.out.println("  Optimal is 3: R -> Q 30, S -> P 20, T -> P 20 (greedy cannot see the {Q,R} pair)");
    }

    // "debtor owes creditor amount" is just an expense the creditor paid for the debtor alone.
    private static void owes(String groupId, User debtor, User creditor, double amount) {
        groupService.addExpense(groupId, debtor.getName() + " owes " + creditor.getName(),
                amount, creditor, List.of(debtor), SplitType.EQUAL, null);
    }

    private static void simplifyAndShow(String groupId) {
        Group g = repo.findById(groupId).orElseThrow();
        System.out.println("\n[" + g.getName() + "] before: " + debts(g));
        groupService.simplifyDebts(groupId);
        System.out.println("[" + g.getName() + "] after:  " + debts(g));
    }

    // Each debt appears twice (once per side); list it from the debtor's side only.
    private static String debts(Group g) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (User u : g.getMembers()) {
            for (var entry : g.getBalanceSheet(u).getBalances().entrySet()) {
                if (entry.getValue() < 0) {
                    sb.append(String.format("%s->%s %.0f, ", u.getName(), entry.getKey().getName(), -entry.getValue()));
                    count++;
                }
            }
        }
        return count + (count == 1 ? " txn   " : " txns  ") + (count == 0 ? "" : sb.substring(0, sb.length() - 2));
    }
}
