package jobscheduler.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jobscheduler.model.DeliveryGuarantee;
import jobscheduler.model.Job;
import jobscheduler.model.JobStatus;
import jobscheduler.retry.RetryPolicy;
import jobscheduler.schedule.FixedRateSchedule;
import jobscheduler.schedule.OnceSchedule;
import jobscheduler.schedule.Schedule;
import jobscheduler.store.InMemoryJobStore;
import jobscheduler.store.JobStore;
import jobscheduler.task.Task;

// Orchestrator. Time comes in as tick(now). Several threads may call tick() at once (EXT 4).
public class Scheduler {
    private final Map<String, Job> jobs = new ConcurrentHashMap<>();
    private final RetryPolicy defaultRetryPolicy;
    private final JobStore store;
    private final AtomicLong lastId;

    // Phase 1 constructors still work: 3 attempts, 30 s apart, in memory.
    public Scheduler() {
        this(3, Duration.ofSeconds(30));
    }

    public Scheduler(int maxAttempts, Duration retryDelay) {
        this(RetryPolicy.fixed(maxAttempts, retryDelay), new InMemoryJobStore());
    }

    // EXT 3: a scheduler started over an existing store picks up its jobs; call recover(now) before the first tick.
    public Scheduler(RetryPolicy defaultRetryPolicy, JobStore store) {
        if (defaultRetryPolicy == null || store == null) throw new IllegalArgumentException("retry policy and store required");
        this.defaultRetryPolicy = defaultRetryPolicy;
        this.store = store;
        for (Job job : store.loadAll()) jobs.put(job.getId(), job);
        this.lastId = new AtomicLong(jobs.size());
    }

    // Req 1 (Phase 1 API)
    public String scheduleOnce(Task task, Instant runAt, DeliveryGuarantee guarantee) {
        return schedule(task, new OnceSchedule(runAt), guarantee);
    }

    // Req 1 (Phase 1 API)
    public String scheduleRecurring(Task task, Instant startAt, Duration interval, DeliveryGuarantee guarantee) {
        return schedule(task, new FixedRateSchedule(startAt, interval), guarantee);
    }

    // EXT 1: any Schedule, e.g. new CronSchedule("0 9 * * MON-FRI", now)
    public String schedule(Task task, Schedule schedule, DeliveryGuarantee guarantee) {
        return schedule(task, schedule, guarantee, defaultRetryPolicy);
    }

    // EXT 2: per-job retry policy
    public String schedule(Task task, Schedule schedule, DeliveryGuarantee guarantee, RetryPolicy retryPolicy) {
        Job job = new Job("job-" + lastId.incrementAndGet(), task, schedule, guarantee, retryPolicy); // a rejected job may skip an id; harmless
        store.save(job);
        jobs.put(job.getId(), job);
        return job.getId();
    }

    // Req 2, 3. Iterates a snapshot; runs a job only if this thread won its claim.
    public void tick(Instant now) {
        if (now == null) throw new IllegalArgumentException("invalid time");
        List<Job> due = jobs.values().stream()
                .filter(job -> job.isDue(now))
                .sorted(Comparator.comparing(Job::getNextRunAt))
                .toList();
        for (Job job : due) {
            if (job.tryClaim(now)) runClaimed(job, now); // EXT 4
        }
    }

    private void runClaimed(Job job, Instant now) {
        store.save(job); // EXT 3: RUNNING is durable BEFORE the task starts; this is what makes at-most-once hold
        boolean succeeded = true;
        try {
            job.getTask().run(job.executionKey());
        } catch (RuntimeException e) { // an Error (e.g. the process dying) is not a failed attempt: it propagates
            succeeded = false;
            System.err.println("[warn] " + job.executionKey() + " attempt " + job.getAttempts() + " failed: " + e.getMessage());
        }
        job.afterRun(now, succeeded);
        store.save(job);
    }

    // EXT 3: jobs left RUNNING by a crash. Returns how many were recovered.
    public int recover(Instant now) {
        if (now == null) throw new IllegalArgumentException("invalid time");
        int recovered = 0;
        for (Job job : jobs.values()) {
            if (job.recoverAfterCrash(now)) {
                store.save(job);
                recovered++;
            }
        }
        return recovered;
    }

    // Req 4
    public void cancel(String jobId) {
        Job job = find(jobId);
        job.cancel();
        store.save(job);
    }

    // Req 5
    public JobStatus getStatus(String jobId) {
        return find(jobId).getStatus();
    }

    private Job find(String jobId) {
        Job job = jobId == null ? null : jobs.get(jobId); // ConcurrentHashMap rejects null keys
        if (job == null) throw new JobNotFoundException(jobId);
        return job;
    }
}
