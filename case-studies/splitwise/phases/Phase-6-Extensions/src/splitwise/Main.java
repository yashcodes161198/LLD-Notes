package splitwise;

import splitwise.enums.SplitType;
import splitwise.model.User;
import splitwise.repository.InMemoryGroupRepository;
import splitwise.service.*;

import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Phase 6: Extensions (exact split, settle up, money rounding, validation) ===");

        BalanceSheetService balanceSheetService = new BalanceSheetService();
        GroupService groupService = new GroupService(
                new InMemoryGroupRepository(),
                new ExpenseService(balanceSheetService),
                new DebtSimplificationService(),
                balanceSheetService);

        User shubh = new User("u1", "Shubh"), bob = new User("u2", "Bob"), tom = new User("u3", "Tom");
        User jay = new User("u4", "Jay");
        String goa = groupService.createGroup("Goa Trip", List.of(shubh, bob, tom));

        // 1. Cent-exact equal split: 100 / 3 -> 33.34 + 33.33 + 33.33 = 100.00
        groupService.addExpense(goa, "Lunch", 100, shubh, List.of(shubh, bob, tom), SplitType.EQUAL, null);

        // 2. The new EXACT split type: one new class, one enum constant, one factory case.
        groupService.addExpense(goa, "Hotel", 250, bob, List.of(shubh, bob, tom), SplitType.EXACT,
                Map.of(shubh, 100.0, bob, 80.0, tom, 70.0));

        // 3. Percentage split still works as before.
        groupService.addExpense(goa, "Scuba", 120, tom, List.of(bob, tom), SplitType.PERCENTAGE,
                Map.of(bob, 25.0, tom, 75.0));

        groupService.printBalances(goa);

        // 4. Settle up: Tom pays Shubh back in cash. Balances move; totals do not.
        System.out.println("\nTom pays Shubh 33.33 (settle up)");
        groupService.settleUp(goa, tom, shubh, 33.33);
        groupService.printBalances(goa);

        // 5. Bad input is rejected before it can corrupt a balance sheet.
        System.out.println();
        tryIt("Jay is not in the group", () -> groupService.addExpense(goa, "Taxi", 30, jay,
                List.of(jay, bob), SplitType.EQUAL, null));
        tryIt("Exact amounts do not add up", () -> groupService.addExpense(goa, "Shopping", 90, bob,
                List.of(bob, tom), SplitType.EXACT, Map.of(bob, 50.0, tom, 30.0)));
        tryIt("Percent given for a non-participant", () -> groupService.addExpense(goa, "Boat", 60, bob,
                List.of(bob, tom), SplitType.PERCENTAGE, Map.of(bob, 50.0, shubh, 50.0)));
        tryIt("Overpaying a settle up", () -> groupService.settleUp(goa, tom, bob, 500));
        tryIt("Settling a debt that does not exist", () -> groupService.settleUp(goa, bob, shubh, 10));
    }

    private static void tryIt(String label, Runnable action) {
        try {
            action.run();
            System.out.println("  " + label + ": accepted?!");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("  " + label + " -> rejected: " + ex.getMessage());
        }
    }
}
