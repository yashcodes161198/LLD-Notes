package splitwise;

import splitwise.enums.SplitType;
import splitwise.model.*;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Phase 1: Users, Groups & Expenses ===");

        User shubh = new User("u1", "Shubh");
        User bob   = new User("u2", "Bob");
        User tom   = new User("u3", "Tom");

        Group goa = new Group("g1", "Goa Trip");
        List.of(shubh, bob, tom).forEach(goa::addMember);

        // No strategy yet: we work out each share by hand.
        goa.addExpense(new Expense("Lunch Day-1", 100, shubh,
                List.of(new Split(shubh, 50), new Split(bob, 50)), SplitType.EQUAL));
        goa.addExpense(new Expense("Cab", 90, tom,
                List.of(new Split(shubh, 45), new Split(bob, 27), new Split(tom, 18)), SplitType.PERCENTAGE));

        System.out.println("Group: " + goa.getName() + " " + goa.getMembers());
        goa.getExpenses().forEach(e -> System.out.println("  " + e));
    }
}
