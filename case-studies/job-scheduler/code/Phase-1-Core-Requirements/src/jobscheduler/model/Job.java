package jobscheduler.model;

import java.time.Duration;
import java.time.Instant;

import jobscheduler.task.Task;

// WHEN and HOW MANY TIMES a task runs. The slot (scheduledFor) and the next try (nextRunAt) are kept apart,
// and attempts count per run, so a retry never moves the schedule (TRICKY #5).
public class Job {
    private final String id;
    private final Task task;
    private final Duration interval; // null = one-off
    private final DeliveryGuarantee guarantee;
    private JobStatus status = JobStatus.SCHEDULED;
    private Instant scheduledFor;
    private Instant nextRunAt;
    private int attempts;

    public Job(String id, Task task, Instant firstRunAt, Duration interval, DeliveryGuarantee guarantee) {
        if (id == null || id.isBlank() || task == null || firstRunAt == null || guarantee == null) {
            throw new IllegalArgumentException("invalid job");
        }
        if (interval != null && (interval.isZero() || interval.isNegative())) {
            throw new IllegalArgumentException("invalid interval"); // 0 would loop forever
        }
        this.id = id;
        this.task = task;
        this.interval = interval;
        this.guarantee = guarantee;
        this.scheduledFor = firstRunAt;
        this.nextRunAt = firstRunAt;
    }

    public boolean isDue(Instant now) {
        return status == JobStatus.SCHEDULED && !nextRunAt.isAfter(now);
    }

    public boolean isRecurring() { return interval != null; }

    // Same for every retry of one run, so an idempotent task can dedupe on it (Req 2, 3).
    public String executionKey() { return id + "@" + scheduledFor; }

    public boolean canRetry(int maxAttempts) {
        return guarantee == DeliveryGuarantee.AT_LEAST_ONCE && attempts < maxAttempts;
    }

    public void markRunning() {
        attempts++;
        status = JobStatus.RUNNING;
    }

    // Moves the next try only; the slot and the key stay the same.
    public void scheduleRetry(Instant at) {
        nextRunAt = at;
        status = JobStatus.SCHEDULED;
    }

    // Fixed-rate: anchored on the slot, not on when it actually ran. Missed slots are skipped in one step.
    public void advanceToNextSlot(Instant now) {
        Instant next = scheduledFor.plus(interval);
        if (!next.isAfter(now)) {
            long missed = Duration.between(scheduledFor, now).toNanos() / interval.toNanos();
            next = scheduledFor.plus(interval.multipliedBy(missed + 1));
        }
        scheduledFor = next;
        nextRunAt = next;
        attempts = 0; // a new run gets fresh attempts
        status = JobStatus.SCHEDULED;
    }

    public void finish(JobStatus outcome) {
        if (outcome != JobStatus.SUCCEEDED && outcome != JobStatus.FAILED) {
            throw new IllegalArgumentException("finish needs SUCCEEDED or FAILED");
        }
        status = outcome;
    }

    // Req 4: idempotent; a finished job stays as it is.
    public void cancel() {
        if (status == JobStatus.SUCCEEDED || status == JobStatus.FAILED) return;
        status = JobStatus.CANCELLED;
    }

    public String getId() { return id; }
    public Task getTask() { return task; }
    public JobStatus getStatus() { return status; }
    public Instant getScheduledFor() { return scheduledFor; }
    public Instant getNextRunAt() { return nextRunAt; }
    public int getAttempts() { return attempts; }

    @Override
    public String toString() { return id + "(" + status + ", next " + nextRunAt + ")"; }
}
