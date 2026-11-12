package jobscheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import jobscheduler.retry.RetryPolicy;
import jobscheduler.schedule.CronSchedule;
import jobscheduler.schedule.OnceSchedule;
import jobscheduler.scheduler.JobNotFoundException;
import jobscheduler.scheduler.Scheduler;
import jobscheduler.store.InMemoryJobStore;
import jobscheduler.store.JobStore;
import jobscheduler.task.ChargeSubscriptionTask;
import jobscheduler.task.NightlyReportTask;
import jobscheduler.task.SendReminderEmailTask;
import jobscheduler.task.Task;

import static jobscheduler.model.DeliveryGuarantee.*;
import static jobscheduler.model.JobStatus.*;

// Self-checks: each one throws if the scheduler misbehaves.
public class Main {
    static final Duration DAY = Duration.ofDays(1);

    public static void main(String[] args) throws InterruptedException {
        baseBehaviourStillHolds();
        ext1Cron();
        ext2Backoff();
        ext3CrashRecovery();
        ext4ConcurrentWorkers();
        badConfigFailsAtStartup();
        demoWithRealTasks();
        System.out.println("\nAll checks passed.");
    }

    // The Phase 1 trace and trap check, now through Schedule + RetryPolicy, using the Phase 1 API.
    static void baseBehaviourStillHolds() {
        System.out.println("=== Base trace (Phase 1 API) ===");
        Scheduler s = new Scheduler();
        ScriptedTask email = new ScriptedTask(true);
        ScriptedTask report = new ScriptedTask(true, false, true, true, false); // D1 fail, ok; D2 fail, fail, ok
        String j1 = s.scheduleOnce(email, at(1, "09:00:00"), AT_MOST_ONCE);
        String j2 = s.scheduleRecurring(report, at(1, "09:00:00"), DAY, AT_LEAST_ONCE);

        for (String time : List.of("08:59:00", "09:00:00", "09:00:30")) s.tick(at(1, time));
        expect(s.getStatus(j1), FAILED, "AT_MOST_ONCE failure -> FAILED, never retried");
        expect(email.keys.size(), 1, "  tried exactly once");
        expect(report.keys.get(1), report.keys.get(0), "retry uses the same key");
        for (String time : List.of("09:00:00", "09:00:30", "09:01:00")) s.tick(at(2, time));
        expect(report.keys.subList(2, 5), Collections.nCopies(3, key(j2, 2, "09:00:00")),
                "D2 starts at 09:00 (no drift) with 3 fresh attempts (TRICKY #5)");
        s.tick(at(5, "10:00:00"));
        s.tick(at(5, "23:00:00"));
        expect(report.keys.size(), 6, "after downtime: one run, missed slots skipped");
        s.cancel(j2);
        s.tick(at(6, "09:00:00"));
        expect(report.keys.size(), 6, "cancelled job doesn't run");
        expectThrows(() -> s.cancel("nope"), JobNotFoundException.class, "cancel unknown id");
        expectThrows(() -> s.scheduleRecurring(report, at(1, "09:00:00"), Duration.ZERO, AT_LEAST_ONCE),
                IllegalArgumentException.class, "zero interval");
    }

    // EXT 1: a cron Schedule; tick/afterRun unchanged. 2026-01-01 is a Thursday.
    static void ext1Cron() {
        System.out.println("\n=== EXT 1: cron schedule ===");
        Scheduler s = new Scheduler();
        ScriptedTask t = new ScriptedTask();
        String id = s.schedule(t, new CronSchedule("0 9 * * MON-FRI", at(2, "08:00:00")), AT_LEAST_ONCE);

        s.tick(at(2, "09:00:00"));                                   // Friday
        s.tick(at(3, "09:00:00"));                                   // Saturday: not a slot
        s.tick(at(4, "09:00:00"));                                   // Sunday: not a slot
        s.tick(at(5, "09:00:00"));                                   // Monday
        expect(t.keys, List.of(key(id, 2, "09:00:00"), key(id, 5, "09:00:00")), "Fri 09:00 then Mon 09:00, weekend skipped");

        ScriptedTask every = new ScriptedTask();
        String id2 = s.schedule(every, new CronSchedule("15,45 * * * *", at(1, "10:15:00")), AT_MOST_ONCE);
        s.tick(at(1, "10:15:00"));
        s.tick(at(1, "10:44:59"));
        s.tick(at(1, "10:45:00"));
        expect(every.keys, List.of(key(id2, 1, "10:15:00"), key(id2, 1, "10:45:00")), "'15,45 * * * *' runs at :15 and :45");

        expectThrows(() -> new CronSchedule("0 9 1 * *", at(1, "00:00:00")), IllegalArgumentException.class, "day-of-month unsupported");
        expectThrows(() -> new CronSchedule("61 9 * * *", at(1, "00:00:00")), IllegalArgumentException.class, "minute out of range");
        expectThrows(() -> new CronSchedule("0 9 * *", at(1, "00:00:00")), IllegalArgumentException.class, "4 fields");
        expectThrows(() -> new CronSchedule("0 x * * *", at(1, "00:00:00")), IllegalArgumentException.class, "not a number");
    }

    // EXT 2: per-job exponential backoff 10 s, 20 s, then capped at 25 s.
    static void ext2Backoff() {
        System.out.println("\n=== EXT 2: exponential backoff per job ===");
        Scheduler s = new Scheduler();
        ScriptedTask t = new ScriptedTask(true, true, true);           // succeeds on the 4th attempt
        RetryPolicy backoff = RetryPolicy.exponential(4, Duration.ofSeconds(10), 2.0, Duration.ofSeconds(25));
        String id = s.schedule(t, new OnceSchedule(at(1, "12:00:00")), AT_LEAST_ONCE, backoff);

        int[] expectedCalls = {1, 1, 2, 2, 3, 3, 4};
        String[] times = {"12:00:00", "12:00:09", "12:00:10", "12:00:29", "12:00:30", "12:00:54", "12:00:55"};
        for (int i = 0; i < times.length; i++) {
            s.tick(at(1, times[i]));
            expect(t.keys.size(), expectedCalls[i], "tick " + times[i]);
        }
        expect(s.getStatus(id), SUCCEEDED, "4th attempt succeeded (delays 10 s, 20 s, 25 s capped)");
        expect(new HashSet<>(t.keys).size(), 1, "all 4 attempts used one key");

        ScriptedTask once = new ScriptedTask(true);
        String id2 = s.schedule(once, new OnceSchedule(at(1, "13:00:00")), AT_MOST_ONCE, backoff);
        s.tick(at(1, "13:00:00"));
        s.tick(at(1, "13:01:00"));
        expect(s.getStatus(id2), FAILED, "a retry policy never makes an AT_MOST_ONCE job retry");
        expect(once.keys.size(), 1, "  tried once");

        expectThrows(() -> RetryPolicy.exponential(3, Duration.ofSeconds(10), 0.5, Duration.ofSeconds(60)),
                IllegalArgumentException.class, "multiplier < 1");
        expectThrows(() -> RetryPolicy.exponential(3, Duration.ofSeconds(10), 2, Duration.ofSeconds(5)),
                IllegalArgumentException.class, "maxDelay < initialDelay");
    }

    // EXT 3: the process dies mid-run (an Error, not a failed attempt). A new Scheduler over the same store recovers.
    static void ext3CrashRecovery() {
        System.out.println("\n=== EXT 3: crash mid-run, then recover ===");
        JobStore store = new InMemoryJobStore();
        RetryPolicy policy = RetryPolicy.fixed(3, Duration.ofSeconds(30));
        Scheduler before = new Scheduler(policy, store);

        ScriptedTask email = new ScriptedTask();
        ChargeSubscriptionTask charge = new ChargeSubscriptionTask("cust-42", 999);
        CrashOnce crashingEmail = new CrashOnce(email);
        CrashOnce crashingCharge = new CrashOnce(charge);
        String emailJob = before.scheduleOnce(crashingEmail, at(1, "09:00:00"), AT_MOST_ONCE);
        String chargeJob = before.scheduleRecurring(crashingCharge, at(1, "09:00:01"), Duration.ofDays(30), AT_LEAST_ONCE);

        expectThrows(() -> before.tick(at(1, "09:00:00")), SimulatedCrash.class, "process dies after the email went out");
        expect(before.getStatus(emailJob), RUNNING, "  the store says RUNNING");

        Scheduler after1 = new Scheduler(policy, store);
        expect(after1.recover(at(1, "09:00:05")), 1, "restart: 1 job recovered");
        expect(after1.getStatus(emailJob), FAILED, "AT_MOST_ONCE: we can't know if it ran, so it is NOT re-run");

        // the charge job is due; it charges, then the process dies again
        expectThrows(() -> after1.tick(at(1, "09:01:00")), SimulatedCrash.class, "charge goes through, then the process dies");
        expect(email.keys.size(), 1, "  the email was not re-run: sent exactly once");
        expect(charge.getChargeCount(), 1, "  charged once so far");

        Scheduler after2 = new Scheduler(policy, store);
        after2.recover(at(1, "09:02:00"));
        expect(after2.getStatus(chargeJob), SCHEDULED, "AT_LEAST_ONCE: retried after recovery");
        after2.tick(at(1, "09:02:30"));
        expect(crashingCharge.keys, List.of(key(chargeJob, 1, "09:00:01"), key(chargeJob, 1, "09:00:01")),
                "the rerun gets the SAME key");
        expect(charge.getChargeCount(), 1, "exactly-once EFFECT: the provider deduped on the key");
        expect(after2.getStatus(chargeJob), SCHEDULED, "next charge in 30 days");
        String fresh = after2.scheduleOnce(new ScriptedTask(), at(1, "10:00:00"), AT_MOST_ONCE);
        expect(fresh, "job-3", "ids continue after a restart");
    }

    // EXT 4: 8 threads tick the same moment over 200 due jobs; each job must run exactly once.
    static void ext4ConcurrentWorkers() throws InterruptedException {
        System.out.println("\n=== EXT 4: concurrent ticks, atomic claim ===");
        Scheduler s = new Scheduler();
        Map<String, AtomicInteger> runs = new ConcurrentHashMap<>();
        Task counting = key -> runs.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 200; i++) ids.add(s.scheduleOnce(counting, at(1, "09:00:00"), AT_LEAST_ONCE));

        CountDownLatch start = new CountDownLatch(1);
        List<Thread> workers = new ArrayList<>();
        for (int w = 0; w < 8; w++) {
            Thread worker = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                s.tick(at(1, "09:00:00"));
            });
            worker.start();
            workers.add(worker);
        }
        start.countDown();
        for (Thread worker : workers) worker.join();

        expect(runs.size(), 200, "every job ran");
        check(runs.values().stream().allMatch(c -> c.get() == 1), "a job ran more than once: " + runs);
        System.out.println("OK     no job ran twice");
        check(ids.stream().allMatch(id -> s.getStatus(id) == SUCCEEDED), "not all SUCCEEDED");
        System.out.println("OK     all 200 SUCCEEDED");
    }

    static void badConfigFailsAtStartup() {
        System.out.println("\n=== Bad config fails at startup ===");
        expectThrows(() -> new Scheduler(0, Duration.ofSeconds(30)), IllegalArgumentException.class, "maxAttempts 0");
        expectThrows(() -> new Scheduler(null, new InMemoryJobStore()), IllegalArgumentException.class, "no retry policy");
        expectThrows(() -> new Scheduler(RetryPolicy.fixed(3, Duration.ofSeconds(1)), null), IllegalArgumentException.class, "no store");
    }

    // Production wiring: the stub tasks print instead of calling a provider.
    static void demoWithRealTasks() {
        System.out.println("\n=== Demo with the stub tasks ===");
        Scheduler s = new Scheduler();
        String email = s.scheduleOnce(new SendReminderEmailTask("alice@example.com"), at(5, "08:00:00"), AT_MOST_ONCE);
        String report = s.schedule(new NightlyReportTask("daily-sales"), new CronSchedule("0 2 * * *", at(5, "00:00:00")), AT_LEAST_ONCE,
                RetryPolicy.exponential(5, Duration.ofMinutes(1), 2.0, Duration.ofMinutes(10)));
        ChargeSubscriptionTask charge = new ChargeSubscriptionTask("bob", 1299);
        String billing = s.schedule(charge, new CronSchedule("0 0 * * MON", at(5, "00:00:00")), AT_LEAST_ONCE);
        s.tick(at(5, "08:00:00"));
        s.tick(at(6, "02:00:00"));
        expect(s.getStatus(email), SUCCEEDED, "reminder sent once");
        expect(s.getStatus(report), SCHEDULED, "report waits for 02:00 tomorrow");
        expect(s.getStatus(billing), SCHEDULED, "billing waits for next Monday");
        expect(charge.getChargeCount(), 1, "Monday's charge went out once");
    }

    // ---- helpers ----

    // Test double: records every key it's called with; call i throws if failures[i] is true.
    static class ScriptedTask implements Task {
        final boolean[] failures;
        final List<String> keys = Collections.synchronizedList(new ArrayList<>());

        ScriptedTask(boolean... failures) { this.failures = failures; }

        @Override
        public void run(String executionKey) {
            int call = keys.size();
            keys.add(executionKey);
            if (call < failures.length && failures[call]) throw new IllegalStateException("scripted failure");
        }
    }

    // Simulates the process dying: an Error is not a failed attempt, so it escapes tick() and leaves the job RUNNING.
    static class SimulatedCrash extends Error {
        SimulatedCrash() { super("simulated crash"); }
    }

    // Test double: runs the real task, then "crashes" the first time only.
    static class CrashOnce implements Task {
        final Task inner;
        final List<String> keys = new ArrayList<>();

        CrashOnce(Task inner) { this.inner = inner; }

        @Override
        public void run(String executionKey) {
            keys.add(executionKey);
            inner.run(executionKey);
            if (keys.size() == 1) throw new SimulatedCrash();
        }
    }

    static Instant at(int day, String time) {
        return Instant.parse(String.format("2026-01-%02dT%sZ", day, time));
    }

    static String key(String jobId, int day, String time) { return jobId + "@" + at(day, time); }

    static void expect(Object got, Object want, String label) {
        check(got.equals(want), label + ": expected " + want + ", got " + got);
        System.out.println("OK     " + label + "  " + got);
    }

    static void expectThrows(Runnable action, Class<? extends Throwable> type, String label) {
        try {
            action.run();
        } catch (RuntimeException | SimulatedCrash e) {
            check(type.isInstance(e), label + ": expected " + type.getSimpleName() + ", got " + e);
            System.out.println("THROWS " + label + " -> " + e.getMessage());
            return;
        }
        throw new IllegalStateException(label + ": expected an exception");
    }

    static void check(boolean ok, String message) {
        if (!ok) throw new IllegalStateException("CHECK FAILED: " + message);
    }
}
