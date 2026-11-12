package splitwise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import splitwise.model.Split;
import splitwise.model.SplitType;
import splitwise.model.Transfer;
import splitwise.notifier.EmailNotifier;
import splitwise.notifier.Notifier;
import splitwise.notifier.PushNotifier;
import splitwise.service.DuplicateExpenseException;
import splitwise.service.ExpenseNotFoundException;
import splitwise.service.InvalidSplitException;
import splitwise.service.SplitwiseService;
import splitwise.strategy.SplitStrategy;

import static splitwise.model.SplitType.*;

// Self-checks: each one throws if the service misbehaves. Amounts are cents.
public class Main {
    static final List<String> ABC = List.of("A", "B", "C");

    public static void main(String[] args) throws Exception {
        baseBehaviourStillHolds();
        ext1SharesSplit();
        ext2EditAndDelete();
        ext3ConcurrentAdds();
        ext4Notifications();
        badConfigFailsAtStartup();
        demoWithRealNotifiers();
        System.out.println("\nAll checks passed.");
    }

    // The Phase 1 trace, now through the strategy map. Same calls, same numbers.
    static void baseBehaviourStillHolds() {
        System.out.println("=== Base trace (Phase 1 calls unchanged) ===");
        SplitwiseService s = new SplitwiseService();
        s.createGroup("trip", ABC);
        expect(s.addExpense("trip", "e1", "A", 1000, EQUAL, ABC, List.of()).getSplits().toString(),
                "[A:334, B:333, C:333]", "e1 EQUAL 1000/3, spare cent to the first participant");
        s.addExpense("trip", "e2", "B", 600, EXACT, ABC, List.of(100L, 200L, 300L));
        s.addExpense("trip", "e3", "C", 1000, PERCENT, ABC, List.of(3333L, 3333L, 3334L));
        expect(s.getBalances("trip"), Map.of("A", 232L, "B", -266L, "C", 34L), "after e1..e3");
        expect(s.simplifyDebts("trip"), List.of(new Transfer("B", "A", 232), new Transfer("B", "C", 34)), "simplify");
        expectThrows(() -> s.addExpense("trip", "e1", "A", 1000, EQUAL, ABC, List.of()),
                DuplicateExpenseException.class, "retried e1 is not counted twice");
        s.settleUp("trip", "B", "A", 232);
        expect(s.getBalances("trip"), Map.of("A", 0L, "B", -34L, "C", 34L), "after B settles 232 with A");
    }

    static void ext1SharesSplit() {
        System.out.println("\n=== EXT 1: SHARES split, a new strategy, no service change ===");
        SplitwiseService s = new SplitwiseService();
        s.createGroup("g", ABC);
        expect(s.addExpense("g", "x1", "A", 1000, SHARES, ABC, List.of(2L, 1L, 1L)).getSplits().toString(),
                "[A:500, B:250, C:250]", "2:1:1 of 1000");
        expect(s.addExpense("g", "x2", "A", 100, SHARES, ABC, List.of(1L, 1L, 1L)).getSplits().toString(),
                "[A:34, B:33, C:33]", "1:1:1 of 100 keeps every cent");
        expect(s.addExpense("g", "x3", "B", 10, SHARES, ABC, List.of(0L, 3L, 0L)).getSplits().toString(),
                "[A:0, B:10, C:0]", "zero shares are allowed");
        expectThrows(() -> s.addExpense("g", "x4", "A", 100, SHARES, ABC, List.of(0L, 0L, 0L)),
                InvalidSplitException.class, "all-zero shares");
        expectThrows(() -> s.addExpense("g", "x4", "A", 100, SHARES, ABC, List.of(1L, 1L)),
                InvalidSplitException.class, "2 shares for 3 people");
        check(sumOf(s.getBalances("g")) == 0, "balances must sum to 0");
    }

    static void ext2EditAndDelete() {
        System.out.println("\n=== EXT 2: edit and delete reverse exactly what was applied ===");
        SplitwiseService s = new SplitwiseService();
        s.createGroup("g", ABC);
        s.addExpense("g", "e1", "A", 900, EQUAL, ABC, List.of());
        s.addExpense("g", "e2", "C", 100, EQUAL, ABC, List.of());
        expect(s.getBalances("g"), Map.of("A", 566L, "B", -333L, "C", -233L), "e1 + e2");

        s.editExpense("g", "e1", "B", 600, EXACT, ABC, List.of(600L, 0L, 0L));
        expect(s.getBalances("g"), Map.of("A", -634L, "B", 567L, "C", 67L), "e1 edited: B paid 600, all for A");

        Map<String, Long> before = s.getBalances("g");
        expectThrows(() -> s.editExpense("g", "e1", "B", 600, EXACT, ABC, List.of(1L, 2L, 3L)),
                InvalidSplitException.class, "bad edit");
        expect(s.getBalances("g"), before, "bad edit left the old version in place");
        expectThrows(() -> s.editExpense("g", "nope", "A", 100, EQUAL, ABC, List.of()),
                ExpenseNotFoundException.class, "edit of an unknown expense");

        s.deleteExpense("g", "e1");
        expect(s.getBalances("g"), Map.of("A", -34L, "B", -33L, "C", 67L), "e1 deleted: only e2 remains");
        expectThrows(() -> s.deleteExpense("g", "e1"), ExpenseNotFoundException.class, "second delete of e1");
        expect(s.getBalances("g"), Map.of("A", -34L, "B", -33L, "C", 67L), "second delete reversed nothing");
        s.deleteExpense("g", "e2");
        expect(s.getBalances("g"), Map.of("A", 0L, "B", 0L, "C", 0L), "all deleted: back to zero");
        s.addExpense("g", "e1", "A", 300, EQUAL, ABC, List.of());
        expect(s.getBalances("g"), Map.of("A", 200L, "B", -100L, "C", -100L), "a deleted id can be reused");
    }

    // 8 threads add 500 unique expenses each and all race on the same 500 shared ids, while a reader checks snapshots.
    static void ext3ConcurrentAdds() throws Exception {
        System.out.println("\n=== EXT 3: concurrent adds on one group ===");
        SplitwiseService s = new SplitwiseService();
        s.createGroup("g", ABC);
        int threads = 8, perThread = 500;
        AtomicInteger sharedWins = new AtomicInteger();
        AtomicBoolean done = new AtomicBoolean();
        AtomicBoolean badSnapshot = new AtomicBoolean();
        ExecutorService pool = Executors.newFixedThreadPool(threads + 1);
        Future<?> reader = pool.submit(() -> {
            while (!done.get()) if (sumOf(s.getBalances("g")) != 0) badSnapshot.set(true);
        });
        List<Future<?>> writers = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            int id = t;
            writers.add(pool.submit(() -> {
                for (int i = 0; i < perThread; i++) {
                    s.addExpense("g", "t" + id + "-" + i, "A", 100, EQUAL, ABC, List.of());
                    try {
                        s.addExpense("g", "shared-" + i, "A", 100, EQUAL, ABC, List.of());
                        sharedWins.incrementAndGet();
                    } catch (DuplicateExpenseException expected) {
                        // another thread got this id first
                    }
                }
            }));
        }
        for (Future<?> w : writers) w.get();
        done.set(true);
        reader.get();
        pool.shutdown();
        check(pool.awaitTermination(5, TimeUnit.SECONDS), "pool did not stop");

        expect(sharedWins.get(), perThread, "each shared id was accepted exactly once");
        long expenses = (long) threads * perThread + perThread;
        expect(s.getBalances("g"), Map.of("A", 66 * expenses, "B", -33 * expenses, "C", -33 * expenses),
                "no lost update across " + expenses + " expenses");
        check(!badSnapshot.get(), "a reader saw a half-applied expense");
        System.out.println("OK     every snapshot read during the race summed to 0");
    }

    static void ext4Notifications() {
        System.out.println("\n=== EXT 4: participants are notified; a notifier failure doesn't undo the expense ===");
        RecordingNotifier recorder = new RecordingNotifier();
        Notifier broken = (user, message) -> { throw new IllegalStateException("provider down"); };
        SplitwiseService s = new SplitwiseService(SplitwiseService.defaultStrategies(), List.of(broken, recorder));
        s.createGroup("g", ABC);
        s.addExpense("g", "e1", "A", 300, EQUAL, ABC, List.of());
        expect(recorder.sent, List.of("B: A added e1: your share is 100", "C: A added e1: your share is 100"),
                "B and C told, payer A not");
        expect(s.getBalances("g"), Map.of("A", 200L, "B", -100L, "C", -100L), "expense kept despite the broken notifier");
        recorder.sent.clear();
        expectThrows(() -> s.addExpense("g", "e2", "A", 300, EXACT, ABC, List.of(1L, 1L, 1L)),
                InvalidSplitException.class, "rejected expense");
        expect(recorder.sent, List.of(), "nobody notified about a rejected expense");
        s.deleteExpense("g", "e1");
        expect(recorder.sent.size(), 2, "delete notifies the same participants");
    }

    static void badConfigFailsAtStartup() {
        System.out.println("\n=== Bad config fails at startup ===");
        Map<SplitType, SplitStrategy> missingShares = SplitwiseService.defaultStrategies();
        missingShares.remove(SHARES);
        expectThrows(() -> new SplitwiseService(missingShares, List.of()), IllegalArgumentException.class,
                "SHARES has no strategy");
        expectThrows(() -> new SplitwiseService(SplitwiseService.defaultStrategies(), null), IllegalArgumentException.class,
                "null notifier list");
        // A buggy custom strategy is still caught: Expense re-checks that shares add up to the amount.
        Map<SplitType, SplitStrategy> buggy = SplitwiseService.defaultStrategies();
        buggy.put(EQUAL, (amount, people, values) -> List.of(new Split(people.get(0), amount - 1)));
        SplitwiseService s = new SplitwiseService(buggy, List.of());
        s.createGroup("g", ABC);
        expectThrows(() -> s.addExpense("g", "e1", "A", 100, EQUAL, ABC, List.of()), IllegalArgumentException.class,
                "strategy that loses a cent is rejected");
        expect(s.getBalances("g"), Map.of("A", 0L, "B", 0L, "C", 0L), "ledger untouched");
    }

    // Production wiring: the stub notifiers print instead of calling a provider.
    static void demoWithRealNotifiers() {
        System.out.println("\n=== Demo with the stub notifiers ===");
        SplitwiseService s = new SplitwiseService(SplitwiseService.defaultStrategies(),
                List.of(new EmailNotifier(), new PushNotifier()));
        List<String> people = List.of("asha", "ben", "chen");
        s.createGroup("goa", people);
        s.addExpense("goa", "villa", "asha", 900_000, SHARES, people, List.of(2L, 1L, 1L)); // asha is a couple
        s.addExpense("goa", "boat", "ben", 120_000, EQUAL, people, List.of());
        List<Transfer> plan = s.simplifyDebts("goa");
        System.out.println("balances " + s.getBalances("goa") + "\nplan     " + plan);
        for (Transfer t : plan) s.settleUp("goa", t.getFrom(), t.getTo(), t.getAmountCents());
        expect(s.getBalances("goa"), Map.of("asha", 0L, "ben", 0L, "chen", 0L), "paying the plan settles everyone");
    }

    // Test double: remembers "user: message" for every notification.
    static class RecordingNotifier implements Notifier {
        final List<String> sent = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void notify(String userId, String message) { sent.add(userId + ": " + message); }
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
