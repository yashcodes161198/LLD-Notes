package jobscheduler.model;

import java.time.Instant;

import jobscheduler.retry.RetryPolicy;
import jobscheduler.schedule.Schedule;
import jobscheduler.task.Task;

// WHEN and HOW MANY TIMES a task runs. The slot (scheduledFor) and the next try (nextRunAt) are kept apart,
// and attempts count per run, so a retry never moves the schedule (TRICKY #5).
// Every state change is synchronized: worker threads and cancel() may touch a job at the same time (EXT 4).
public class Job {
    private final String id;
    private final Task task;
    private final Schedule schedule;          // EXT 1 (was: interval)
    private final DeliveryGuarantee guarantee;
    private final RetryPolicy retryPolicy;    // EXT 2 (was: two scheduler constants)
    private JobStatus status = JobStatus.SCHEDULED;
    private Instant scheduledFor;
    private Instant nextRunAt;
    private int attempts;

    public Job(String id, Task task, Schedule schedule, DeliveryGuarantee guarantee, RetryPolicy retryPolicy) {
        if (id == null || id.isBlank() || task == null || schedule == null || guarantee == null || retryPolicy == null) {
            throw new IllegalArgumentException("invalid job");
        }
        this.id = id;
        this.task = task;
        this.schedule = schedule;
        this.guarantee = guarantee;
        this.retryPolicy = retryPolicy;
        this.scheduledFor = schedule.firstSlot();
        this.nextRunAt = scheduledFor;
    }

    public synchronized boolean isDue(Instant now) {
        return status == JobStatus.SCHEDULED && !nextRunAt.isAfter(now);
    }

    // EXT 4: "is due" and "mark running" as one atomic step, so two workers can't both run this slot.
    public synchronized boolean tryClaim(Instant now) {
        if (!isDue(now)) return false;
        attempts++;
        status = JobStatus.RUNNING;
        return true;
    }

    public synchronized String executionKey() { return id + "@" + scheduledFor; }

    // After a claimed attempt. One synchronized step, so a concurrent cancel() can't slip between the checks.
    public synchronized void afterRun(Instant now, boolean succeeded) {
        if (status != JobStatus.RUNNING) return; // cancelled while running
        if (!succeeded && guarantee == DeliveryGuarantee.AT_LEAST_ONCE && retryPolicy.canRetry(attempts)) {
            nextRunAt = now.plus(retryPolicy.delayAfter(attempts)); // same slot, same key
            status = JobStatus.SCHEDULED;
            return;
        }
        Instant next = schedule.nextSlotAfter(scheduledFor, now);
        if (next == null) {
            status = succeeded ? JobStatus.SUCCEEDED : JobStatus.FAILED; // one-off: no more slots
            return;
        }
        scheduledFor = next; // a lost run doesn't stop a recurring schedule
        nextRunAt = next;
        attempts = 0;
        status = JobStatus.SCHEDULED;
    }

    // EXT 3: found RUNNING after a restart. We can't know if the task ran, so it counts as a failed attempt:
    // AT_MOST_ONCE is never re-run; AT_LEAST_ONCE is retried with the same key (if attempts remain).
    public synchronized boolean recoverAfterCrash(Instant now) {
        if (status != JobStatus.RUNNING) return false;
        afterRun(now, false);
        return true;
    }

    // Req 4: idempotent; a finished job stays as it is.
    public synchronized void cancel() {
        if (status == JobStatus.SUCCEEDED || status == JobStatus.FAILED) return;
        status = JobStatus.CANCELLED;
    }

    public String getId() { return id; }
    public Task getTask() { return task; }
    public DeliveryGuarantee getGuarantee() { return guarantee; }
    public synchronized JobStatus getStatus() { return status; }
    public synchronized Instant getScheduledFor() { return scheduledFor; }
    public synchronized Instant getNextRunAt() { return nextRunAt; }
    public synchronized int getAttempts() { return attempts; }

    @Override
    public synchronized String toString() { return id + "(" + status + ", next " + nextRunAt + ")"; }
}
