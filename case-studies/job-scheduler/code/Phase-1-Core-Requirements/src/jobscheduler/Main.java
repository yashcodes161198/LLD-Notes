package jobscheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jobscheduler.model.JobStatus;
import jobscheduler.scheduler.JobNotFoundException;
import jobscheduler.scheduler.Scheduler;
import jobscheduler.task.NightlyReportTask;
import jobscheduler.task.SendReminderEmailTask;
import jobscheduler.task.Task;

import static jobscheduler.model.DeliveryGuarantee.*;
import static jobscheduler.model.JobStatus.*;

// Self-checks: each one throws if the scheduler misbehaves.
public class Main {
    static final Duration DAY = Duration.ofDays(1);

    public static void main(String[] args) {
        verificationTrace();
        retriesBelongToTheRun();
        oneOffRetriesThenFails();
        tasksThatChangeTheScheduler();
        inputValidation();
        badConfigFailsAtStartup();
        demoWithRealTasks();
        System.out.println("\nAll checks passed.");
    }

    // The trace from job_scheduler.py (3 attempts, 30 s apart).
    static void verificationTrace() {
        System.out.println("=== Verification trace ===");
        Scheduler s = new Scheduler();
        ScriptedTask email = new ScriptedTask(true);                // fails once
        ScriptedTask report = new ScriptedTask(true);               // fails on its first attempt, then succeeds
        String j1 = s.scheduleOnce(email, at(1, "09:00:00"), AT_MOST_ONCE);
        String j2 = s.scheduleRecurring(report, at(1, "09:00:00"), DAY, AT_LEAST_ONCE);

        s.tick(at(1, "08:59:00"));
        expect(email.keys.size() + report.keys.size(), 0, "D1 08:59 nothing due");

        s.tick(at(1, "09:00:00"));
        expect(s.getStatus(j1), FAILED, "D1 09:00 J1 AT_MOST_ONCE fails -> FAILED");
        expect(report.keys, List.of(j2 + "@" + at(1, "09:00:00")), "D1 09:00 J2 ran (and failed) even though J1 failed");
        expect(s.getStatus(j2), SCHEDULED, "J2 waiting for its retry");

        s.tick(at(1, "09:00:15"));
        expect(report.keys.size(), 1, "D1 09:00:15 retry not due yet");
        s.tick(at(1, "09:00:30"));
        expect(report.keys.get(1), report.keys.get(0), "D1 09:00:30 retry uses the SAME key");
        s.tick(at(1, "09:00:30"));
        expect(report.keys.size(), 2, "same tick twice runs nothing new");
        expect(email.keys.size(), 1, "AT_MOST_ONCE job was never retried");

        s.tick(at(2, "09:00:00"));
        expect(last(report.keys), j2 + "@" + at(2, "09:00:00"), "D2 runs at 09:00, not 09:00:30 (no drift)");

        s.tick(at(5, "10:00:00"));
        expect(report.keys.size(), 4, "D5 10:00 after downtime: runs ONCE");
        expect(last(report.keys), j2 + "@" + at(3, "09:00:00"), "  for the oldest missed slot");
        s.tick(at(5, "23:59:00"));
        expect(report.keys.size(), 4, "D4 and D5 slots were skipped, no catch-up burst");

        s.cancel(j2);
        expect(s.getStatus(j2), CANCELLED, "cancel(J2)");
        s.tick(at(6, "09:00:00"));
        expect(report.keys.size(), 4, "D6 09:00 cancelled job doesn't run");

        s.cancel(j1);
        expect(s.getStatus(j1), FAILED, "cancel on a finished job is a no-op");
        expectThrows(() -> s.cancel("nope"), JobNotFoundException.class, "cancel unknown id");
        expectThrows(() -> s.getStatus("nope"), JobNotFoundException.class, "getStatus unknown id");
    }

    // TRICKY #5: the trap must NOT happen. Retries move the next try, never the slot; attempts reset per slot;
    // a recurring job survives a run that fails for good.
    static void retriesBelongToTheRun() {
        System.out.println("\n=== Retries belong to the run, not the job (TRICKY #5) ===");
        Scheduler s = new Scheduler();
        // D1: fail, ok. D2: fail, fail, ok (needs all 3 attempts). D3: fail x3. D4: ok.
        ScriptedTask t = new ScriptedTask(true, false, true, true, false, true, true, true, false);
        String id = s.scheduleRecurring(t, at(1, "09:00:00"), DAY, AT_LEAST_ONCE);

        for (String time : List.of("09:00:00", "09:00:30")) s.tick(at(1, time));
        for (String time : List.of("09:00:00", "09:00:30", "09:01:00")) s.tick(at(2, time));
        expect(t.keys.size(), 5, "D2 started at 09:00 and got all 3 attempts (lifetime counter would give 0 retries)");
        expect(t.keys.subList(2, 5), List.of(key(id, 2), key(id, 2), key(id, 2)), "D2 attempts share one key");

        for (String time : List.of("09:00:00", "09:00:30", "09:01:00", "09:01:30")) s.tick(at(3, time));
        expect(t.keys.size(), 8, "D3 failed 3 times, no 4th attempt");
        expect(s.getStatus(id), SCHEDULED, "recurring job keeps its schedule after a lost run");

        s.tick(at(4, "09:00:00"));
        expect(last(t.keys), key(id, 4), "D4 runs on its 09:00 slot");

        ScriptedTask once = new ScriptedTask(true);
        String daily = s.scheduleRecurring(once, at(5, "09:00:00"), DAY, AT_MOST_ONCE);
        s.tick(at(5, "09:00:00"));
        s.tick(at(5, "09:00:30"));
        expect(once.keys.size(), 1, "recurring AT_MOST_ONCE failure: no retry");
        s.tick(at(6, "09:00:00"));
        expect(last(once.keys), key(daily, 6), "  but the next day still runs");
    }

    static void oneOffRetriesThenFails() {
        System.out.println("\n=== One-off AT_LEAST_ONCE: 3 attempts, then FAILED ===");
        Scheduler s = new Scheduler();
        ScriptedTask t = new ScriptedTask(true, true, true, true);
        String id = s.scheduleOnce(t, at(1, "12:00:00"), AT_LEAST_ONCE);
        for (String time : List.of("12:00:00", "12:00:30", "12:01:00", "12:01:30", "12:02:00")) s.tick(at(1, time));
        expect(t.keys.size(), 3, "exactly 3 attempts");
        expect(s.getStatus(id), FAILED, "then FAILED");

        ScriptedTask ok = new ScriptedTask();
        String id2 = s.scheduleOnce(ok, at(1, "11:00:00"), AT_MOST_ONCE); // in the past: runs on the next tick
        s.tick(at(1, "12:05:00"));
        expect(s.getStatus(id2), SUCCEEDED, "runAt in the past runs on the next tick");
    }

    static void tasksThatChangeTheScheduler() {
        System.out.println("\n=== Tasks that cancel or schedule jobs while they run ===");
        Scheduler s = new Scheduler();
        List<String> ran = new ArrayList<>();
        String[] other = new String[1];
        String selfCancelling = s.scheduleRecurring(key -> {
            ran.add("self");
            s.cancel(key.substring(0, key.indexOf('@')));
        }, at(1, "09:00:00"), DAY, AT_LEAST_ONCE);
        s.scheduleOnce(key -> {
            ran.add("scheduler");
            s.scheduleOnce(k -> ran.add("child"), at(1, "09:00:00"), AT_MOST_ONCE); // map changes mid-tick
            s.cancel(other[0]);
        }, at(1, "09:00:00"), AT_MOST_ONCE);
        other[0] = s.scheduleOnce(k -> ran.add("other"), at(1, "09:00:00"), AT_MOST_ONCE);

        s.tick(at(1, "09:00:00"));
        expect(s.getStatus(selfCancelling), CANCELLED, "a job that cancels itself stays CANCELLED");
        expect(ran, List.of("self", "scheduler"), "no ConcurrentModificationException; cancelled 'other' skipped");
        s.tick(at(1, "09:00:01"));
        expect(ran, List.of("self", "scheduler", "child"), "the child runs on the next tick");
    }

    static void inputValidation() {
        System.out.println("\n=== Input validation ===");
        Scheduler s = new Scheduler();
        expectThrows(() -> s.scheduleOnce(null, at(1, "09:00:00"), AT_MOST_ONCE), IllegalArgumentException.class, "null task");
        expectThrows(() -> s.scheduleOnce(new ScriptedTask(), null, AT_MOST_ONCE), IllegalArgumentException.class, "null runAt");
        expectThrows(() -> s.scheduleOnce(new ScriptedTask(), at(1, "09:00:00"), null), IllegalArgumentException.class, "null guarantee");
        expectThrows(() -> s.scheduleRecurring(new ScriptedTask(), at(1, "09:00:00"), Duration.ZERO, AT_LEAST_ONCE),
                IllegalArgumentException.class, "zero interval");
        expectThrows(() -> s.scheduleRecurring(new ScriptedTask(), at(1, "09:00:00"), Duration.ofSeconds(-5), AT_LEAST_ONCE),
                IllegalArgumentException.class, "negative interval");
        expectThrows(() -> s.scheduleRecurring(new ScriptedTask(), at(1, "09:00:00"), null, AT_LEAST_ONCE),
                IllegalArgumentException.class, "null interval");
        expectThrows(() -> s.tick(null), IllegalArgumentException.class, "tick(null)");
        String id = s.scheduleOnce(new ScriptedTask(), at(1, "09:00:00"), AT_MOST_ONCE);
        expect(id, "job-1", "rejected jobs were never stored");
    }

    static void badConfigFailsAtStartup() {
        System.out.println("\n=== Bad config fails at startup ===");
        expectThrows(() -> new Scheduler(0, Duration.ofSeconds(30)), IllegalArgumentException.class, "maxAttempts 0");
        expectThrows(() -> new Scheduler(3, Duration.ofSeconds(-1)), IllegalArgumentException.class, "negative retryDelay");
    }

    // Production wiring: the stub tasks print instead of calling a provider.
    static void demoWithRealTasks() {
        System.out.println("\n=== Demo with the stub tasks ===");
        Scheduler s = new Scheduler();
        String email = s.scheduleOnce(new SendReminderEmailTask("alice@example.com"), at(1, "08:00:00"), AT_MOST_ONCE);
        String report = s.scheduleRecurring(new NightlyReportTask("daily-sales"), at(1, "02:00:00"), DAY, AT_LEAST_ONCE);
        s.tick(at(1, "08:00:00"));
        s.tick(at(2, "02:00:00"));
        expect(s.getStatus(email), SUCCEEDED, "reminder sent once");
        expect(s.getStatus(report), SCHEDULED, "report scheduled for its next night");
    }

    // ---- helpers ----

    // Test double: records every key it's called with; call i throws if failures[i] is true.
    static class ScriptedTask implements Task {
        final boolean[] failures;
        final List<String> keys = new ArrayList<>();

        ScriptedTask(boolean... failures) { this.failures = failures; }

        @Override
        public void run(String executionKey) {
            int call = keys.size();
            keys.add(executionKey);
            if (call < failures.length && failures[call]) throw new IllegalStateException("scripted failure");
        }
    }

    static Instant at(int day, String time) {
        return Instant.parse(String.format("2026-01-%02dT%sZ", day, time));
    }

    static String key(String jobId, int day) { return jobId + "@" + at(day, "09:00:00"); }

    static String last(List<String> list) { return list.get(list.size() - 1); }

    static void expect(Object got, Object want, String label) {
        check(got.equals(want), label + ": expected " + want + ", got " + got);
        System.out.println("OK     " + label + "  " + got);
    }

    static void expectThrows(Runnable action, Class<? extends RuntimeException> type, String label) {
        try {
            action.run();
        } catch (RuntimeException e) {
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
