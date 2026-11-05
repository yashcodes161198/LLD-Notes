package splitwise;

import splitwise.enums.SplitType;
import splitwise.model.User;
import splitwise.repository.InMemoryGroupRepository;
import splitwise.service.*;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Phase 4: Repository & Group Service ===");

        User shubh = new User("u1", "Shubh");
        User bob   = new User("u2", "Bob");
        User tom   = new User("u3", "Tom");
        User jay   = new User("u4", "Jay");

        // Wiring: build the object graph once, inject dependencies through constructors.
        InMemoryGroupRepository repo = new InMemoryGroupRepository();
        BalanceSheetService balanceSheetService = new BalanceSheetService();
        ExpenseService expenseService = new ExpenseService(balanceSheetService);
        GroupService groupService = new GroupService(repo, expenseService);

        // From here on the client only talks to GroupService.
        String goa = groupService.createGroup("Goa Trip", List.of(shubh, bob, tom));
        groupService.addExpense(goa, "Lunch Day-1", 100, shubh, List.of(shubh, bob), SplitType.EQUAL, null);
        groupService.addExpense(goa, "Lunch Day-2", 100, bob, List.of(bob, tom), SplitType.EQUAL, null);
        groupService.printBalances(goa);

        // 1-on-1 expenses live in a default "Non-Group Expenses" group.
        String nonGroup = groupService.createGroup("Non-Group Expenses", List.of(shubh, jay));
        groupService.addExpense(nonGroup, "Dinner", 20, jay, List.of(shubh, jay), SplitType.EQUAL, null);
        groupService.addExpense(nonGroup, "Ice cream", 10, shubh, List.of(shubh, jay), SplitType.EQUAL, null);
        System.out.println();
        groupService.printBalances(nonGroup);

        try {
            groupService.addExpense("no-such-id", "Taxi", 30, tom, List.of(tom), SplitType.EQUAL, null);
        } catch (IllegalArgumentException ex) {
            System.out.println("\nRejected: " + ex.getMessage());
        }
    }
}
