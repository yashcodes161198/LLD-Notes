# Answer key 02: Entities + class design (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Names don't have to match exactly; responsibilities do.

## Part A: Entities (35 pts)

### E1: Noun list · 5 pts
- **Must:** at least 6 of: client, job, task, scheduler/system, interval/schedule, delivery guarantee, execution key, time slot, attempt/retry, status, clock/time.
- 1 pt per noun up to 5.

### E2: Keep/drop decisions · 15 pts
| Noun | Expected | Reason that must appear | Pts |
|---|---|---|---|
| Client | DROP | external actor; we store nothing about it | 2 |
| Scheduler | KEEP | orchestrator / entry point; holds jobs, runs due ones | 2 |
| Job | KEEP | state with rules: schedule, guarantee, status, attempts | 3 |
| Task | INTERFACE | the seam; implementations out of scope | 3 |
| Interval / schedule | DROP as class (field) | just a Duration on Job until cron is asked for | 2 |
| Retry / attempt | DROP as class | a counter on Job plus two scheduler constants | 2 |
| Execution key / clock | DROP | key is derived (id + slot); time comes in via `tick(now)` | 1 |
- **Red flag −4:** a `Schedule` / `RetryPolicy` / `Execution` class in the base design without a requirement that needs it (speculative). If they name it as "promoted later when X is asked", no penalty.
- **Red flag −3:** keeps Client/User as a class.

### E3: Nouns that survive as something smaller · 6 pts
- **Must (3 each, any two):** interval → a `Duration` field on Job (null = one-off); retry settings → constants on Scheduler (3, 30 s); attempt → an int counter on Job; execution key → derived from id + scheduledFor; time → a parameter of `tick(now)`.

### E4: "Isn't a job just a Runnable?" · 6 pts
- **Must:** the task is WHAT runs (3); the job is WHEN and HOW MANY TIMES: schedule, guarantee, status, attempts, i.e. state with its own rules (3).

### E5: Enums · 3 pts
- **Must:** `JobStatus {SCHEDULED, RUNNING, SUCCEEDED, FAILED, CANCELLED}` (2) and `DeliveryGuarantee {AT_MOST_ONCE, AT_LEAST_ONCE}` (1).

## Part B: Class design (40 pts)

### C1: Scheduler (orchestrator) · 16 pts
| Member | Pts | Accept |
|---|---|---|
| `- jobs: Map<string, Job>` | 3 | a list gets 1 (cancel/getStatus need lookup by id) |
| `- maxAttempts`, `- retryDelay` | 2 | constants or config |
| `+ scheduleOnce(...)` / `+ scheduleRecurring(...) -> jobId` | 4 | one `schedule(task, start, interval?, guarantee)` is fine; must **return the id** |
| `+ tick(now)` | 3 | `runDueJobs(now)`; −2 if it takes no time and reads a global clock without saying why |
| `+ cancel(jobId)` | 2 | |
| `+ getStatus(jobId) -> JobStatus` | 2 | |

### C2: Job · 16 pts
| Member | Pts |
|---|---|
| `id`, `task`, `interval` (nullable), `guarantee` | 3 |
| `status: JobStatus` | 2 |
| **Two** time fields: `scheduledFor` (slot) and `nextRunAt` (next try) | 4 |
| `attempts` (documented as per run) | 2 |
| `isDue(now)`, `canRetry(...)` | 2 |
| `markRunning`, `scheduleRetry`, `advanceToNextSlot`, `finish`, `cancel` (or equivalents) | 3 |
- **Partial:** one time field only → 1 of the 4 (they'll lose more in D3).
- **Red flag −3:** the Scheduler computes "is due" / "can retry" from Job's raw fields instead of asking the job (breaks Information Expert).

### C3: Task · 4 pts
- `interface Task { run(executionKey) }` (3); "throws = this attempt failed" (1). `run()` without the key: 2 of the 3.

### C4: Top-down and traceability · 4 pts
- Scheduler written first (2), and members annotated with the requirement they come from (2).

## Part C: Design decisions (25 pts)

### D1: Scan vs priority queue · 5 pts
- **Must:** a linear scan over the map is fine at this size (2); a heap must be re-ordered on every retry/cancel/reschedule, a second copy of `nextRunAt` that can go stale (2); switch when n or profiling says so (1).
- "Heap, because O(log n)" with no trade-off: 2 pts.

### D2: Where status lives · 5 pts
- **Must:** on **Job** (2); **Information Expert** / encapsulation (2); not a separate `Map<id, Status>` on the scheduler, which duplicates state (1).

### D3: Two time fields · 6 pts
- **Must:** `scheduledFor` = the slot (also the execution key) (2); `nextRunAt` = when we'll actually try, which a retry moves (2); with one field, a retry **moves the schedule** (drift) and the key changes between retries (2).

### D4: Attempts per run · 5 pts
- **Must:** per run, reset when a new slot starts (2). A lifetime counter means later runs get fewer or no retries (2), and a recurring job can end up FAILED for good (1).

### D5: Time as a parameter · 4 pts
- **Must:** keeps threads/timers out of the core design (2) and makes it deterministic and testable (2).

---
**Level calibration:** ≥85 senior · 70–84 mid · 50–69 junior · <50 redo Step 2–3 of `../job_scheduler.py`.
