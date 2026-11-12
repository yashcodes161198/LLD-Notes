# Job Scheduler: Java code

Runnable Java for the design in [`../job_scheduler.py`](../job_scheduler.py). Run either phase with `./run.sh` (JDK 17+). `Main` is a set of self-checks: it prints each step and throws if anything is wrong. Failed attempts also print a `[warn]` line to stderr; that's expected.

```
Phase-N/src/jobscheduler/
  model/      JobStatus, DeliveryGuarantee, Job
  task/       Task (the seam) + SendReminderEmailTask, NightlyReportTask (+ ChargeSubscriptionTask in Phase 2): the real work is stubbed with a println
  scheduler/  Scheduler (orchestrator), JobNotFoundException
  schedule/   Schedule + OnceSchedule, FixedRateSchedule, CronSchedule   (Phase 2)
  retry/      RetryPolicy                                                (Phase 2)
  store/      JobStore + InMemoryJobStore                                (Phase 2)
  Main.java   self-checks
```

## Phase 1: Core Requirements

Requirements 1-5 from Step 1, using the best version of `tick()` / `runOnce()` from Step 4 (not the TRICKY #5 trap).

| Class | Role |
|---|---|
| `Scheduler` | `scheduleOnce`, `scheduleRecurring` (return a job id), `tick(now)`, `cancel(id)`, `getStatus(id)`. `tick` scans a snapshot of due jobs, earliest first; `runOnce` marks the attempt before running, catches failures per job, and retries only AT_LEAST_ONCE jobs. Fails at construction on bad retry settings. |
| `Job` | Owns its state: `scheduledFor` (the slot, used in the execution key) kept apart from `nextRunAt` (the next try), and `attempts` counted per run. `advanceToNextSlot` is fixed-rate and skips missed slots in one step. Validates itself in the constructor. |
| `Task` | The seam. `SendReminderEmailTask` (AT_MOST_ONCE) and `NightlyReportTask` (AT_LEAST_ONCE, idempotent) implement it with the real work stubbed out (they print). `Main` uses a scripted test double for the checks, then a demo with the stub tasks. |
| `JobNotFoundException` | The specific error for an unknown job id. |

Checks in `Main`: the verification trace from the design, an explicit check that **the trap doesn't happen** (day 2 runs at 09:00, not 09:00:30; each day gets 3 fresh attempts with one shared key; a recurring job survives a run that fails for good), tasks that cancel or schedule jobs mid-tick, input validation, and the bad-config startup check.

## Phase 2: Extensions

Each extension lands where the design says it does. The Phase 1 API (`scheduleOnce`, `scheduleRecurring`, `new Scheduler()`, `new Scheduler(maxAttempts, retryDelay)`) still works.

| Extension | Pattern | What changed |
|---|---|---|
| 1. Cron expressions | Strategy | `Job.interval` became a `Schedule`: `OnceSchedule`, `FixedRateSchedule`, `CronSchedule` (minute, hour, day-of-week subset, UTC). A one-off job is now just a schedule with no next slot. New `schedule(task, Schedule, guarantee)`. |
| 2. Per-job exponential backoff | Promote constants to a value class | `RetryPolicy` (max attempts, initial delay, multiplier, cap), owned by each `Job`; the old constructor builds a fixed policy. New `schedule(..., RetryPolicy)` overload. |
| 3. Crash mid-run; exactly-once effect | Durable transitions + idempotency key | `JobStore` (stub `InMemoryJobStore`); RUNNING is saved **before** the task starts. A new `Scheduler` over the same store loads the jobs and `recover(now)` treats a RUNNING job as a failed attempt: AT_MOST_ONCE is never re-run, AT_LEAST_ONCE is retried with the same key. `ChargeSubscriptionTask` dedupes on that key, so the customer is charged once. |
| 4. Worker threads | Atomic claim | `Job.tryClaim(now)` (synchronized check-and-mark); the post-run transition is one synchronized `afterRun`; jobs live in a `ConcurrentHashMap`. Checked with 8 threads ticking 200 jobs at the same instant. |

Not coded (answered in words in the design's "More follow-ups"): a heap or timing wheel for millions of jobs, per-run timeouts, job dependencies (DAG), priorities, idempotent schedule calls, an outcome listener for metrics, and leases for multi-machine workers.
