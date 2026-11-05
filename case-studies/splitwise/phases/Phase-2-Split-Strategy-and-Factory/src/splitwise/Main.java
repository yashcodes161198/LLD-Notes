package splitwise;

import splitwise.enums.SplitType;
import splitwise.factory.SplitStrategyFactory;
import splitwise.model.*;
import splitwise.strategy.SplitStrategy;

import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Phase 2: Split Strategy & Factory ===");

        User shubh = new User("u1", "Shubh");
        User bob   = new User("u2", "Bob");
        User tom   = new User("u3", "Tom");

        Group goa = new Group("g1", "Goa Trip");
        List.of(shubh, bob, tom).forEach(goa::addMember);

        // The caller names a SplitType; the factory picks the algorithm.
        goa.addExpense(createExpense("Lunch Day-1", 100, shubh,
                List.of(shubh, bob), SplitType.EQUAL, null));
        goa.addExpense(createExpense("Cab", 90, tom,
                List.of(shubh, bob, tom), SplitType.PERCENTAGE, Map.of(shubh, 50.0, bob, 30.0, tom, 20.0)));
        goa.addExpense(createExpense("Cycling", 18, shubh,
                List.of(shubh, bob, tom), SplitType.EQUAL, null));

        System.out.println("Group: " + goa.getName() + " " + goa.getMembers());
        goa.getExpenses().forEach(e -> System.out.println("  " + e));

        try {
            createExpense("Bad split", 100, bob, List.of(bob, tom), SplitType.PERCENTAGE, Map.of(bob, 60.0, tom, 30.0));
        } catch (IllegalArgumentException ex) {
            System.out.println("  Rejected 60% + 30%: " + ex.getMessage());
        }
    }

    private static Expense createExpense(String description, double amount, User paidBy,
                                         List<User> participants, SplitType type, Map<User, Double> metadata) {
        SplitStrategy strategy = SplitStrategyFactory.getStrategy(type);
        List<Split> splits = strategy.split(amount, participants, metadata);
        return new Expense(description, amount, paidBy, splits, type);
    }
}
