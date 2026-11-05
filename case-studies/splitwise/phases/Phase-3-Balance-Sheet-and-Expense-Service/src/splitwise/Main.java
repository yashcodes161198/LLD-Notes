package splitwise;

import splitwise.enums.SplitType;
import splitwise.model.*;
import splitwise.service.BalanceSheetService;
import splitwise.service.ExpenseService;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Phase 3: Balance Sheet & Expense Service ===");

        User shubh = new User("u1", "Shubh");
        User tom   = new User("u2", "Tom");
        User bob   = new User("u3", "Bob");

        Group goa = new Group("g1", "Goa Trip");
        List.of(shubh, tom, bob).forEach(goa::addMember);

        ExpenseService expenseService = new ExpenseService(new BalanceSheetService());

        // The example from the requirements section of the video.
        expenseService.addExpense(goa, "Dinner", 40, shubh,
                List.of(shubh, tom, bob), SplitType.EQUAL, null);
        expenseService.addExpense(goa, "Cycling", 20, shubh,
                List.of(shubh, bob), SplitType.EQUAL, null);

        goa.getExpenses().forEach(e -> System.out.println("  " + e));
        System.out.println();
        goa.getMembers().forEach(u -> goa.getBalanceSheet(u).print(u));
    }
}
