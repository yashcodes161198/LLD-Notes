# Answer key 03: Implementation, guided (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Any pseudo-code syntax is fine. Judge **logic, order and errors**, not syntax. Mentally run every body against the trace in I3-7 and against the verification trace in `../job_scheduler.py`.

### I3-1: scheduleRecurring · 12 pts
```
if task == null || startAt == null || guarantee == null
    throw Error("invalid job")
if interval == null || interval <= 0
    throw Error("invalid interval")
job = new Job(generateId(), task, interval, guarantee, startAt)   # scheduledFor = nextRunAt = startAt, SCHEDULED
jobs[job.id] = job
return job.id
```
| Check | Pts |
|---|---|
| Validates task / startAt / guarantee | 3 |
| interval ≤ 0 → its own error | 3 |
| All validation **before** creating or storing the job | 2 |
| Job starts with scheduledFor = nextRunAt = startAt, SCHEDULED | 2 |
| Stores by id and returns the id | 2 |
- **Red flag −3:** rejects a startAt in the past (the edge case says allowed).

### I3-2: tick · 14 pts
```
due = [job for job in jobs.values() if job.isDue(now)]
due.sortBy(job -> job.nextRunAt)
for job in due
    if job.isDue(now)             # an earlier task may have cancelled it
        runOnce(job, now)
```
| Check | Pts |
|---|---|
| Collects due jobs into a **separate list** before running any (doesn't iterate the live map) | 4 |
| Due = SCHEDULED and nextRunAt ≤ now (via `isDue`) | 3 |
| Earliest first | 2 |
| Calls `runOnce` for each | 2 |
| Re-checks due-ness before running each one | 3 |
- **Red flag −4:** a try/catch around the whole loop, so one failing job stops the others.

### I3-3: runOnce · 30 pts
```
job.markRunning()                                  # attempts++, RUNNING
succeeded = true
try
    job.task.run(job.executionKey())
catch error
    succeeded = false
if job.status == CANCELLED
    return
if !succeeded && job.canRetry(maxAttempts)         # AT_LEAST_ONCE && attempts < maxAttempts
    job.scheduleRetry(now + retryDelay)            # nextRunAt only; slot and key unchanged
    return
if job.isRecurring()
    job.advanceToNextSlot(now)
else
    job.finish(succeeded ? SUCCEEDED : FAILED)
```
| Check | Pts |
|---|---|
| Marks the attempt **before** calling the task | 5 |
| Passes the execution key | 3 |
| try/catch around the task **per job** | 4 |
| Leaves a job cancelled during its own run CANCELLED | 3 |
| Retries **only** when AT_LEAST_ONCE and attempts < max | 5 |
| Retry moves `nextRunAt` to now + retryDelay and **not** `scheduledFor` | 4 |
| Recurring → `advanceToNextSlot(now)`, **also after a final failure** | 3 |
| One-off → SUCCEEDED or FAILED | 3 |
- **Red flag −6:** an AT_MOST_ONCE job can be retried. **−5:** a recurring job goes to FAILED after a failed run (kills the schedule; Req 3 says it keeps it).

### I3-4: advanceToNextSlot · 16 pts
```
next = scheduledFor + interval
if next <= now
    missed = floor((now - scheduledFor) / interval)
    next = scheduledFor + (missed + 1) * interval
scheduledFor = next
nextRunAt = next
attempts = 0
status = SCHEDULED
```
| Check | Pts |
|---|---|
| Next slot computed from **scheduledFor** (not from now, not from nextRunAt) | 5 |
| Skips all missed slots in one step (arithmetic, or a loop with a comment on cost) | 4 |
| Sets **both** scheduledFor and nextRunAt | 3 |
| Resets attempts | 3 |
| Status back to SCHEDULED | 1 |
- **Accept:** a `while next <= now: next += interval` loop gets 2 of the 4 "skip" points (correct, but O(missed slots)); full 4 if they say why it's fine or bounded.
- Anchoring on `now` (fixed-delay) gets 0 of the first 5 unless they say it's a deliberate fixed-delay choice; then 3.

### I3-5: Mark before run · 6 pts
- **Must:** once state is persisted, writing "started" before running **is** the at-most-once guarantee: after a crash we see RUNNING and know not to run it again (4). Writing it after would let a crash between run and write re-run the job (2).

### I3-6: Map miss and finished jobs · 6 pts
- **Must:** a miss returns null in my pseudo-code, which is language-dependent, said out loud (2); unknown id → throw "job not found" (2); an already SUCCEEDED/FAILED/CANCELLED job → no-op, because cancel is idempotent (2).

### I3-7: Trace (the trap) · 16 pts
- Day 1: 09:00 fails (attempts 1), retry at 09:00:30 succeeds (attempts 2); next = 09:00:30 + 1 day → **day 2 runs at 09:00:30, not 09:00** (4).
- At day 2's first try **attempts = 3** (lifetime counter, counts successes too) (3).
- So 3 < 3 is false: **no retry, the job is FAILED and the daily schedule is dead** (3).
- **Root cause (3):** the run's state (retry time, attempts) and the schedule share one field and one counter.
- **Own version (3):** retry moves only nextRunAt; next slot is computed from scheduledFor (day 2 09:00); attempts reset per slot; a final failure on a recurring job advances the slot instead of FAILED.
- If they only spot the drift: 7 of 16 max.

---
**Level calibration:** ≥85 senior · 70–84 mid · 50–69 junior. I3-7's root cause (run vs schedule) is the senior signal: without it, cap the level at "mid" whatever the score.
