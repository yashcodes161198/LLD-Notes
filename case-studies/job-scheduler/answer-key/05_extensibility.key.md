# Answer key 05: Extensibility (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Each question is scored on **Pattern / Lands in / Say / Code**. "Describe only" for Code is acceptable for X5–X6 (the code points go to the description instead). For X1–X4 a code delta is expected.

General red flag for any question, **−5:** the answer rewrites the design or touches many classes when one place would do.

---

### X1: Cron expressions · 20 pts
- **Pattern (4):** **Strategy**: a `Schedule` interface replaces `interval` (`OnceSchedule`, `FixedRateSchedule`, `CronSchedule`).
- **Lands in (5):** Job's `interval` field → `schedule`; `advanceToNextSlot` asks `schedule.nextSlotAfter(scheduledFor, now)`. `tick` and `runOnce` unchanged.
- **Say (4):** "next slot" is the only thing that varies; real cron syntax comes from a library, hand-write only the subset needed.
- **Code (7):**
```
interface Schedule
    + firstSlot() -> timestamp
    + nextSlotAfter(slot, now) -> timestamp | null    # null = done
Job.advanceToNextSlot(now):
    next = schedule.nextSlotAfter(scheduledFor, now)
    if next == null: finish(...); return
    scheduledFor = nextRunAt = next; attempts = 0; status = SCHEDULED
```
- **Bonus +2 (cap 20):** raises **time zones / DST** (09:00 in which zone; 02:30 missing or doubled).

### X2: Per-job exponential backoff · 15 pts
- **Pattern (4):** **promote the two constants to a value class** (`RetryPolicy`) owned by the job.
- **Lands in (4):** Job gets `retryPolicy`; `canRetry` and the retry time in `runOnce` ask it. The scheduler's constants become the default policy.
- **Say (3):** exponential with a cap, **plus jitter** so many jobs that failed together don't retry in the same second.
- **Code (4):** `delayAfter(attempt) = min(initial * multiplier^(attempt-1), maxDelay)`; `canRetry(attempts) = attempts < maxAttempts`; validated in the constructor.

### X3: Crash mid-run; exactly-once · 25 pts
- **Pattern (6):** **durable state transitions** (a `JobStore`) **+ idempotency key**.
- **Lands in (5):** Scheduler saves the job at each transition and gains `recover(now)` at startup; Job unchanged.
- **Say (5):** at-most-once = persist RUNNING **before** running; on recovery a RUNNING at-most-once job is FAILED, never re-run. At-least-once = on recovery, run it again **with the same key**. Exactly-once is an **effect**: at-least-once + an idempotent consumer that dedupes on the key (e.g. a payment API idempotency key).
- **Code (6):** `recover(now)`: for each RUNNING job: AT_MOST_ONCE → finish the run as failed; AT_LEAST_ONCE → `scheduleRetry(now)` (same slot) if attempts remain, else failed; save.
- **Must mention (3):** the RUNNING write must be **durable before** `task.run()`; written after, a crash in between re-runs the job.
- **Red flag −5:** claims the scheduler alone can guarantee exactly-once execution.

### X4: Worker threads, no double run · 20 pts
- **Pattern (6):** **atomic claim**: compare-and-set SCHEDULED → RUNNING (a synchronized `tryClaim`, or a conditional DB update across machines).
- **Lands in (5):** Job gets `tryClaim(now)`; `tick` runs a job only if it won the claim; the jobs map becomes concurrent.
- **Say (5):** names the **check-then-act** race (two workers both see the job due and both run it); across machines, `UPDATE … WHERE status='SCHEDULED'` plus a **lease** so a dead worker's job is picked up again.
- **Code (4):**
```
Job.tryClaim(now):        # synchronized
    if !isDue(now) return false
    markRunning(); return true
tick: for job in due: if job.tryClaim(now): executor.submit(() -> runClaimed(job, now))
```
- **Red flag −4:** "use a lock" around the whole tick including the task run (serialises every job, defeats the pool) with no comment.

### X5: 10 million jobs · 10 pts
- **Pattern (4):** a **min-heap / timing wheel** keyed by `nextRunAt` instead of a scan; or partition jobs across scheduler nodes by job id.
- **Lands in (3):** how Scheduler finds due jobs (its collection), not Job.
- **Say (3):** cancel/retry leave **stale heap entries**: skip them lazily by checking the job's real `nextRunAt` on pop.

### X6: A job hangs forever · 10 pts
- **Pattern (4):** a **timeout** per run (run on a worker with a deadline / `Future.get(timeout)`).
- **Lands in (3):** `runOnce` (or the worker wrapper); a timeout counts as a failed attempt, so the retry rules apply unchanged.
- **Say (3):** you can't safely kill a thread; the task must honour interruption, and a timed-out AT_MOST_ONCE run is still never retried (it may have done its side effect).

---
**Level calibration:** ≥80 senior/staff · 60–79 senior-ish mid · 40–59 mid · <40 junior (juniors often never reach this step, so don't be discouraged).
