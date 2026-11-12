package jobscheduler.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jobscheduler.model.DeliveryGuarantee;
import jobscheduler.model.Job;
import jobscheduler.model.JobStatus;
import jobscheduler.task.Task;

// Orchestrator. Time comes in as tick(now); jobs run synchronously inside it.
public class Scheduler {
    private final Map<String, Job> jobs = new LinkedHashMap<>(); // Req 4, 5: by id; Req 2: scanned each tick
    private final int maxAttempts;                               // Req 3
    private final Duration retryDelay;                           // Req 3

    public Scheduler() {
        this(3, Duration.ofSeconds(30));
    }

    public Scheduler(int maxAttempts, Duration retryDelay) {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
        if (retryDelay == null || retryDelay.isNegative()) throw new IllegalArgumentException("invalid retryDelay");
        this.maxAttempts = maxAttempts;
        this.retryDelay = retryDelay;
    }

    // Req 1
    public String scheduleOnce(Task task, Instant runAt, DeliveryGuarantee guarantee) {
        return add(task, runAt, null, guarantee);
    }

    // Req 1
    public String scheduleRecurring(Task task, Instant startAt, Duration interval, DeliveryGuarantee guarantee) {
        if (interval == null) throw new IllegalArgumentException("invalid interval");
        return add(task, startAt, interval, guarantee);
    }

    private String add(Task task, Instant firstRunAt, Duration interval, DeliveryGuarantee guarantee) {
        Job job = new Job("job-" + (jobs.size() + 1), task, firstRunAt, interval, guarantee); // validates before we store
        jobs.put(job.getId(), job);
        return job.getId();
    }

    // Req 2, 3. Iterates a snapshot: a task may schedule or cancel jobs while it runs.
    public void tick(Instant now) {
        if (now == null) throw new IllegalArgumentException("invalid time");
        List<Job> due = new ArrayList<>();
        for (Job job : jobs.values()) {
            if (job.isDue(now)) due.add(job);
        }
        due.sort(Comparator.comparing(Job::getNextRunAt));
        for (Job job : due) {
            if (job.isDue(now)) runOnce(job, now); // an earlier task may have cancelled it
        }
    }

    private void runOnce(Job job, Instant now) {
        job.markRunning(); // record the attempt BEFORE running: with persistence this is the at-most-once guarantee
        boolean succeeded = true;
        try {
            job.getTask().run(job.executionKey());
        } catch (RuntimeException e) {
            succeeded = false;
            System.err.println("[warn] " + job.executionKey() + " attempt " + job.getAttempts() + " failed: " + e.getMessage());
        }

        if (job.getStatus() == JobStatus.CANCELLED) return; // the task cancelled its own job
        if (!succeeded && job.canRetry(maxAttempts)) {
            job.scheduleRetry(now.plus(retryDelay)); // same slot, same key
            return;
        }
        if (job.isRecurring()) {
            job.advanceToNextSlot(now); // decision: a lost run doesn't stop the schedule
        } else {
            job.finish(succeeded ? JobStatus.SUCCEEDED : JobStatus.FAILED);
        }
    }

    // Req 4
    public void cancel(String jobId) {
        find(jobId).cancel();
    }

    // Req 5
    public JobStatus getStatus(String jobId) {
        return find(jobId).getStatus();
    }

    private Job find(String jobId) {
        Job job = jobs.get(jobId);
        if (job == null) throw new JobNotFoundException(jobId);
        return job;
    }
}
