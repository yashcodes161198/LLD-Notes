package splitwise;

import java.util.List;
import java.util.Map;

import splitwise.model.Expense;
import splitwise.model.Transfer;
import splitwise.service.DuplicateExpenseException;
import splitwise.service.GroupNotFoundException;
import splitwise.service.InvalidSplitException;
import splitwise.service.NotAGroupMemberException;
import splitwise.service.SplitwiseService;

import static splitwise.model.SplitType.*;

// Self-checks: each one throws if the service misbehaves. Amounts are cents.
public class Main {
    public static void main(String[] args) {
        verificationTrace();
        roundingTrapDoesNotHappen();
        invalidInputChangesNothing();
        badGroupSetupFails();
        demoGoaTrip();
        System.out.println("\nAll checks passed.");
    }

    // A, B, C share a trip. One expense of each split type, then simplify and settle.
    static void verificationTrace() {
        System.out.println("=== Verification trace ===");
        SplitwiseService s = new SplitwiseService();
        s.createGroup("trip", List.of("A", "B", "C"));

        Expense e1 = s.addExpense("trip", "e1", "A", 1000, EQUAL, List.of("A", "B", "C"), List.of());
        expect(e1.getSplits().toString(), "[A:334, B:333, C:333]", "e1 EQUAL 1000/3, first participant takes the spare cent");
        expect(s.getBalances("trip"), Map.of("A", 666L, "B", -333L, "C", -333L), "after e1 (payer owes their own share too)");

        s.addExpense("trip", "e2", "B", 600, EXACT, List.of("A", "B", "C"), List.of(100L, 200L, 300L));
        expect(s.getBalances("trip"), Map.of("A", 566L, "B", 67L, "C", -633L), "after e2 EXACT 100/200/300");

        s.addExpense("trip", "e3", "C", 1000, PERCENT, List.of("A", "B", "C"), List.of(3333L, 3333L, 3334L));
        expect(s.getBalances("trip"), Map.of("A", 232L, "B", -266L, "C", 34L), "after e3 PERCENT 33.33/33.33/33.34");

        expect(s.simplifyDebts("trip"), List.of(new Transfer("B", "A", 232), new Transfer("B", "C", 34)),
                "simplify: B pays both creditors, 2 transfers");

        s.settleUp("trip", "B", "A", 232);
        expect(s.getBalances("trip"), Map.of("A", 0L, "B", -34L, "C", 34L), "after B settles 232 with A");
        expect(s.simplifyDebts("trip"), List.of(new Transfer("B", "C", 34)), "simplify after settle: one transfer left");

        s.settleUp("trip", "B", "C", 34);
        expect(s.simplifyDebts("trip"), List.of(), "all settled: nothing to pay");
    }

    // The trap: dividing money with doubles or plain integer division loses cents, and the balances stop summing to 0.
    static void roundingTrapDoesNotHappen() {
        System.out.println("\n=== Rounding trap: no cent is lost or created ===");
        SplitwiseService s = new SplitwiseService();
        s.createGroup("g", List.of("A", "B", "C"));
        s.addExpense("g", "x1", "A", 100, EQUAL, List.of("A", "B", "C"), List.of());
        s.addExpense("g", "x2", "B", 1, EQUAL, List.of("A", "B", "C"), List.of());
        s.addExpense("g", "x3", "C", 999, PERCENT, List.of("A", "B", "C"), List.of(3333L, 3333L, 3334L));
        s.addExpense("g", "x4", "A", 7, EQUAL, List.of("B", "C"), List.of()); // payer not a participant
        check(sumOf(s.getBalances("g")) == 0, "balances must sum to exactly 0, got " + sumOf(s.getBalances("g")));
        System.out.println("OK     balances " + s.getBalances("g") + " sum to 0");
        long paidOut = 0;
        for (Transfer t : s.simplifyDebts("g")) paidOut += t.getAmountCents();
        long owed = 0;
        for (long v : s.getBalances("g").values()) if (v > 0) owed += v;
        expect(paidOut, owed, "simplify pays every creditor exactly what they are owed");
    }

    // Every rejected call must leave the ledger exactly as it was (validate before mutate).
    static void invalidInputChangesNothing() {
        System.out.println("\n=== Invalid input is rejected before any change ===");
        SplitwiseService s = new SplitwiseService();
        s.createGroup("trip", List.of("A", "B", "C"));
        s.addExpense("trip", "e1", "A", 900, EQUAL, List.of("A", "B", "C"), List.of());
        Map<String, Long> before = s.getBalances("trip");
        List<String> abc = List.of("A", "B", "C");

        expectThrows(() -> s.addExpense("trip", "e1", "A", 900, EQUAL, abc, List.of()),
                DuplicateExpenseException.class, "retried e1 is not counted twice");
        expectThrows(() -> s.addExpense("nope", "e2", "A", 100, EQUAL, abc, List.of()),
                GroupNotFoundException.class, "unknown group");
        expectThrows(() -> s.addExpense("trip", "e2", "Z", 100, EQUAL, abc, List.of()),
                NotAGroupMemberException.class, "payer not in group");
        expectThrows(() -> s.addExpense("trip", "e2", "A", 100, EQUAL, List.of("A", "Z"), List.of()),
                NotAGroupMemberException.class, "participant not in group");
        expectThrows(() -> s.addExpense("trip", "e2", "A", 600, EXACT, abc, List.of(100L, 200L, 299L)),
                InvalidSplitException.class, "EXACT shares sum to 599, not 600");
        expectThrows(() -> s.addExpense("trip", "e2", "A", 600, EXACT, abc, List.of(300L, 300L)),
                InvalidSplitException.class, "EXACT with 2 values for 3 people");
        expectThrows(() -> s.addExpense("trip", "e2", "A", 600, PERCENT, abc, List.of(3333L, 3333L, 3333L)),
                InvalidSplitException.class, "PERCENT totals 99.99%");
        expectThrows(() -> s.addExpense("trip", "e2", "A", 600, PERCENT, abc, List.of(-100L, 5100L, 5000L)),
                InvalidSplitException.class, "negative percent");
        expectThrows(() -> s.addExpense("trip", "e2", "A", 600, EQUAL, abc, List.of(1L, 1L, 1L)),
                InvalidSplitException.class, "EQUAL with values");
        expectThrows(() -> s.addExpense("trip", "e2", "A", 0, EQUAL, abc, List.of()),
                IllegalArgumentException.class, "zero amount");
        expectThrows(() -> s.addExpense("trip", "e2", "A", 100, EQUAL, List.of("A", "A"), List.of()),
                IllegalArgumentException.class, "duplicate participant");
        expectThrows(() -> s.addExpense("trip", "e2", "A", 100, EQUAL, List.of(), List.of()),
                IllegalArgumentException.class, "no participants");
        expectThrows(() -> s.settleUp("trip", "B", "B", 100), IllegalArgumentException.class, "settle with yourself");
        expectThrows(() -> s.settleUp("trip", "B", "Z", 100), NotAGroupMemberException.class, "settle with a stranger");
        expectThrows(() -> s.settleUp("trip", "B", "A", -5), IllegalArgumentException.class, "negative settle amount");

        expect(s.getBalances("trip"), before, "ledger unchanged after every rejected call");
    }

    static void badGroupSetupFails() {
        System.out.println("\n=== Bad group setup fails ===");
        SplitwiseService s = new SplitwiseService();
        s.createGroup("trip", List.of("A", "B"));
        expectThrows(() -> s.createGroup("trip", List.of("C", "D")), IllegalArgumentException.class, "duplicate group id");
        expectThrows(() -> s.createGroup("solo", List.of("A")), IllegalArgumentException.class, "one-member group");
        expectThrows(() -> s.createGroup("dup", List.of("A", "A")), IllegalArgumentException.class, "duplicate member");
        expectThrows(() -> s.createGroup(" ", List.of("A", "B")), IllegalArgumentException.class, "blank group id");
    }

    // A readable walk-through, still checked.
    static void demoGoaTrip() {
        System.out.println("\n=== Demo: Goa trip (rupees as paise) ===");
        SplitwiseService s = new SplitwiseService();
        s.createGroup("goa", List.of("asha", "ben", "chen", "dev"));
        s.addExpense("goa", "hotel", "asha", 1_200_000, EQUAL, List.of("asha", "ben", "chen", "dev"), List.of());
        s.addExpense("goa", "dinner", "ben", 400_000, EXACT, List.of("asha", "ben", "chen", "dev"),
                List.of(50_000L, 150_000L, 100_000L, 100_000L));
        s.addExpense("goa", "taxi", "chen", 90_000, PERCENT, List.of("ben", "chen", "dev"), List.of(5000L, 2500L, 2500L));
        System.out.println("balances " + s.getBalances("goa"));
        List<Transfer> plan = s.simplifyDebts("goa");
        System.out.println("plan     " + plan);
        check(plan.size() <= 3, "greedy needs at most members - 1 transfers");
        for (Transfer t : plan) s.settleUp("goa", t.getFrom(), t.getTo(), t.getAmountCents());
        expect(s.getBalances("goa"), Map.of("asha", 0L, "ben", 0L, "chen", 0L, "dev", 0L), "paying the plan settles everyone");
    }

    // ---- helpers ----

    static long sumOf(Map<String, Long> balances) {
        long sum = 0;
        for (long v : balances.values()) sum += v;
        return sum;
    }

    static void expect(Object got, Object want, String label) {
        check(got.equals(want), label + ": expected " + want + ", got " + got);
        System.out.println("OK     " + label + "  " + got);
    }

    static void expectThrows(Runnable action, Class<? extends RuntimeException> type, String label) {
        try {
            action.run();
        } catch (RuntimeException e) {
            check(type.isInstance(e), label + ": expected " + type.getSimpleName() + ", got " + e);
            System.out.println("THROWS " + label + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return;
        }
        throw new IllegalStateException(label + ": expected an exception");
    }

    static void check(boolean ok, String message) {
        if (!ok) throw new IllegalStateException("CHECK FAILED: " + message);
    }
}
