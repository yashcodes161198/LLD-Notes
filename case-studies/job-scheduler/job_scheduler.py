"""
================================================================================
 JOB / TASK SCHEDULER (cron-like, retries, at-most-once / at-least-once)
 Source: a commonly reported LLD / machine-coding question (no video).
         Designed with the Hello Interview delivery framework (Amazon Locker video).
 Level: senior. Language: pseudo-code.
================================================================================

This is the editor file a strong candidate would type, with coaching notes
added. The obvious tick() has a real bug (see TRICKY #5); the BEST version
fixes it.

LEGEND (search for these):
    # 💬 SAY:      a talking point to say out loud in the interview, near-verbatim
    # ⚠️ TRICKY:   where candidates lose points, or a subtle bug
    # ✅ BEST:     the strongest approach, and why it beats the alternatives
    # 🎯 GRADED:   which rubric dimension this moment is scored on
    # ⏱            time budget for the step
    # 📸           matching screenshot in ./screenshots/

THE 5-STEP DELIVERY FRAMEWORK (📸 00-delivery-framework.png)
    1. Requirements      ⏱ ~5 min
    2. Entities          ⏱ ~3 min
    3. Class design      ⏱ ~10-15 min
    4. Implementation    ⏱ ~10 min
    5. Extensibility     ⏱ ~5 min (senior+; juniors may never get here)

💬 SAY (at the start): "Schedulers hide a lot of policy: when things run, what
   happens on failure, and how many times a job may run. I'd like to pin those
   down first, by primary capabilities, error handling and scope, and then
   write down what we agree on."
================================================================================


--------------------------------------------------------------------------------
STEP 1: REQUIREMENTS                                  ⏱ ~5 min   📸 00
--------------------------------------------------------------------------------

Design a job scheduler that runs tasks at a given time or on a recurring schedule, and retries them when they fail.

# ⚠️ TRICKY: "retries" sounds like a detail. It is the whole problem. Retrying
#    a job that already sent an email sends it twice. Don't write a class until
#    you know which jobs MAY run twice.
# 🎯 GRADED: Problem analysis. Can you turn "a scheduler" into precise rules
#    for time, failure and delivery guarantees?

1. primary capabilities
2. error handling
3. scope boundaries

Primary capabilities:

- One-off jobs, recurring jobs, or both? What does "recurring" look like: cron expressions or a fixed interval?
      -> Both. Recurring = a start time plus a fixed interval (every 5 min, every day).
         Full cron expressions are a follow-up.
- What is a "job" to us? Do we care what it does?
      -> No. The client hands us a task (a callable). We decide WHEN and HOW MANY
         TIMES it runs, never what it does.
- Can clients cancel a job or ask about it?
      -> Yes: cancel by job id, and query its status.
# ✅ BEST: asking "do we care what the job does?" lets the interviewer give you
#    a seam: Task is an interface, its implementations are out of scope.

Error handling:

- What happens when a job throws?
      -> Retry it, up to 3 attempts in total, 30 seconds apart. Then give up.
- Is every job safe to retry? e.g. "send the welcome email", "charge the card".
      -> No. Each job declares a delivery guarantee:
         AT_MOST_ONCE  = never run twice; a failure is final (email, payment).
         AT_LEAST_ONCE = retried; the job must be idempotent (rebuild a report).
- Then how does an at-least-once job know a retry is a retry?
      -> We pass it an execution key that stays the same across retries of the
         same run, so it can dedupe.
- If a recurring job's run fails for good, does the whole schedule stop?
      -> No. That run is lost; the next run still happens (cron behaviour).
- What if the scheduler was down and missed several runs of a recurring job?
      -> Run it once, skip the missed runs. No catch-up burst.
- Cancel or query an unknown job id?
      -> Throw a specific "job not found" error.
# 💬 SAY: "The key question for me is retries. A thrown exception doesn't mean
#    the side effect didn't happen: the email may have gone out and THEN the
#    logging call threw. So retrying is only safe for idempotent jobs."
# ⚠️ TRICKY: "exactly-once" is the trap word here. Say it out loud: across a
#    failure you can only get at-most-once or at-least-once from the scheduler.
#    Exactly-once EFFECT = at-least-once + an idempotent job keyed by the
#    execution key. (Coded in Phase 2.)

Scope boundaries:

- One process, in memory? Or durable and distributed across machines?
      -> One process, in memory. Persistence and multiple workers are follow-ups.
- Who drives time? Do we own threads and timers?
      -> No. Something outside calls tick(now) every second or so. Jobs run
         synchronously inside tick. Time is a parameter, which also makes it testable.
# ✅ BEST: "time is a parameter" removes threads, sleeps and clocks from the
#    core design. You can still talk about a thread pool in extensibility.


Requirements:
1. Client schedules a job: one-off (task, runAt) or recurring (task, startAt, interval), plus a delivery guarantee
   - System returns a job id
   - Error if the task is missing or the interval is not positive
2. When ticked with the current time, the system runs every due job, earliest first
   - Each run gets an execution key (job id + the time slot it is for)
   - A recurring job then moves to its next slot after now; missed slots are skipped
3. A failed run is retried only if the job is AT_LEAST_ONCE
   - Up to 3 attempts per run, 30 s apart, same execution key on every attempt
   - AT_MOST_ONCE jobs are never run twice; a failure is final for that run
   - A one-off job whose run fails for good is FAILED; a recurring job keeps its schedule
4. Client cancels a job by id
   - The job never runs again
   - Error if the id is unknown; cancelling a finished job does nothing
5. Client queries a job's status (SCHEDULED, RUNNING, SUCCEEDED, FAILED, CANCELLED)
   - Error if the id is unknown

# ✅ BEST: each requirement = ACTOR + ACTION + INPUT with sub-bullets for the
#    reaction and the error. Reqs 1, 2, 4, 5 become the 4 public methods; Req 3
#    becomes the branch inside tick().
# ⚠️ TRICKY: Req 3 says "per run" and "same execution key". Those two words
#    decide your class design: retry state belongs to ONE RUN, not to the job.

Out of scope:
- What tasks do (sending email, building reports): Task is an interface
- Threads, timers and the loop that calls tick()
- Persistence / crash recovery, multiple workers, cron expressions, priorities, job dependencies, timeouts
# 💬 SAY: "Everything not on this list I'll treat as out of scope, and several
#    of these are natural follow-ups I'm happy to come back to."


--------------------------------------------------------------------------------
STEP 2: ENTITIES                                      ⏱ ~3 min   📸 00
--------------------------------------------------------------------------------
# ✅ BEST: write down EVERY noun, then prune each one out loud.
#
#   Noun               Keep?  Why
#   ---------------    -----  -----------------------------------------------------
#   Client/Caller       NO    External actor. It calls our API; we store nothing about it.
#   Scheduler           YES   The orchestrator / entry point. Holds all jobs, runs due ones.
#   Job                 YES   ⚠️ TRICKY: "isn't a job just a Runnable?" No. The task is
#                             WHAT runs. The job is WHEN and HOW MANY TIMES: schedule,
#                             guarantee, status, attempts. That is state with rules.
#   Task                YES   As an INTERFACE only (the seam). Its implementations
#   (interface)               are out of scope; we only call run(executionKey).
#   Schedule/interval   NO*   Just a Duration on Job for now (one-off = no interval).
#                             *It becomes a class in EXT 1 (cron), not before.
#   Retry / Attempt     NO    In the base design it is a counter on Job plus two
#                             scheduler-wide constants (3 attempts, 30 s).
#   Execution key       NO    Derived: jobId + "@" + the slot time. Nothing to store.
#   Timer/Clock         NO    Out of scope; time comes in as tick(now).
#   Status              ENUM  JobStatus.
#   Delivery guarantee  ENUM  DeliveryGuarantee.
#
# 💬 SAY: "I'll keep Scheduler and Job, plus Task as an interface. Retry
#    settings are two constants today. If the interviewer asks for per-job
#    backoff, they get promoted to a RetryPolicy class."
# 🎯 GRADED: Class design (the right entities, and not too many).

Entities:
- Scheduler
- Job
- Task (interface)
- (enum) JobStatus: SCHEDULED, RUNNING, SUCCEEDED, FAILED, CANCELLED
- (enum) DeliveryGuarantee: AT_MOST_ONCE, AT_LEAST_ONCE


--------------------------------------------------------------------------------
STEP 3: CLASS DESIGN, TOP-DOWN                        ⏱ ~10-15 min  📸 00
--------------------------------------------------------------------------------
# ✅ BEST: start with the ORCHESTRATOR and work down.
#       state    = "what must I REMEMBER to enforce the requirements?"
#       behavior = "what client ACTION needs this?"  -> public methods
"""

class Scheduler:
    - jobs: Map<string, Job>                 # Req 4, 5: look up by id; Req 2: scan for due jobs
    - maxAttempts: int = 3                   # Req 3
    - retryDelay: Duration = 30s             # Req 3

    + scheduleOnce(task, runAt, guarantee) -> jobId                 # Req 1
    + scheduleRecurring(task, startAt, interval, guarantee) -> jobId # Req 1
    + tick(now) -> void                                             # Req 2, 3
    + cancel(jobId) -> void | error                                 # Req 4
    + getStatus(jobId) -> JobStatus | error                         # Req 5

    # ✅ BEST: one Map<id, Job> and a LINEAR SCAN in tick().
    # 💬 SAY: "A min-heap keyed by next run time is the textbook answer, but a
    #    heap needs re-ordering on every retry, reschedule and cancel. That's a
    #    second copy of nextRunAt that can go stale. With thousands of jobs and a
    #    tick per second, a scan is fine. I'd switch to a heap when profiling
    #    says so."
    # ⚠️ TRICKY: LeetCode habit says "priority queue!" immediately. The interviewer
    #    rewards you for knowing why the heap is harder (stale entries on
    #    cancel/retry) and when it's worth it.


class Job:
    - id: string
    - task: Task
    - interval: Duration | null              # Req 1: null = one-off
    - guarantee: DeliveryGuarantee           # Req 3
    - status: JobStatus                      # Req 5
    - scheduledFor: timestamp                # Req 2: the SLOT this run belongs to
    - nextRunAt: timestamp                   # Req 3: when to try next (the slot, or a retry time)
    - attempts: int                          # Req 3: attempts for THIS run, not lifetime

    + isDue(now) -> boolean                  # status == SCHEDULED && nextRunAt <= now
    + isRecurring() -> boolean
    + executionKey() -> string               # id + "@" + scheduledFor
    + canRetry(maxAttempts) -> boolean       # AT_LEAST_ONCE && attempts < maxAttempts
    + markRunning()                          # attempts++, status = RUNNING
    + scheduleRetry(at)                      # nextRunAt = at; slot and key unchanged
    + advanceToNextSlot(now)                 # next slot after now; attempts = 0
    + finish(status)                         # SUCCEEDED / FAILED
    + cancel()                               # no-op if already finished

    # ✅ BEST (Information Expert): the JOB owns its status and its run state.
    #    The scheduler asks "are you due?" and "can you retry?"; it never pokes
    #    at fields. Status lives on Job, not in a separate Map<id, Status>.
    # ⚠️ TRICKY: TWO time fields, not one. scheduledFor is the slot (09:00 every
    #    day); nextRunAt is when we'll actually try (09:00:30 after a failure).
    #    Merge them and a retry silently moves the whole schedule. See TRICKY #5.
    # 🎯 GRADED: Class design (each field traced to a requirement).


interface Task:
    + run(executionKey: string) -> void      # throws = this attempt failed
    # Implementations are out of scope, e.g. SendReminderEmailTask (AT_MOST_ONCE:
    # a duplicate email is worse than a missed one) and NightlyReportTask
    # (AT_LEAST_ONCE: it overwrites the report for its key, so reruns are harmless).


"""
--------------------------------------------------------------------------------
STEP 4: IMPLEMENTATION                                ⏱ ~10 min   📸 00
--------------------------------------------------------------------------------
# 💬 SAY: "Pseudo-code or a real language?" Then: "The interesting logic is
#    tick() and its helper runOnce(), plus advanceToNextSlot on Job, which is
#    where recurring time is computed. schedule, cancel and getStatus are
#    mostly validation, so I'll do them quickly."
#
# ✅ BEST: for EVERY method, write "Core logic" and "Edge cases" FIRST.
# 🎯 GRADED: Code quality plus Communication.
"""

# Implementation
# 1. Define the core logic
# 2. Consider the edge cases

class Scheduler:
    scheduleRecurring(task, startAt, interval, guarantee):
        """
        Core logic:
        1. Validate the inputs
        2. Create a Job with scheduledFor = nextRunAt = startAt, status SCHEDULED
        3. Store it by id, return the id

        Edge cases:
        1. task / startAt / guarantee missing -> throw "invalid job"
        2. interval <= 0 -> throw "invalid interval"   (0 would loop forever)
        3. startAt in the past -> allowed: runs on the next tick
        """
        if task == null || startAt == null || guarantee == null
            throw Error("invalid job")
        if interval == null || interval <= 0
            throw Error("invalid interval")
        job = new Job(generateId(), task, interval, guarantee, startAt)
        jobs[job.id] = job
        return job.id
    # scheduleOnce is the same with interval = null.
    # ⚠️ TRICKY: validate BEFORE creating or storing anything. A half-built job
    #    in the map gets picked up by the next tick.


    tick(now):
        """
        Core logic:
        1. Collect due jobs (SCHEDULED and nextRunAt <= now) into a list, earliest first
        2. Run each one with runOnce(job, now)

        Edge cases:
        1. nothing due -> no-op
        2. a task throws -> handled inside runOnce; the other due jobs still run
        3. a task schedules or cancels jobs while it runs -> we loop over a
           COPY, so the map can change safely
        4. tick called twice with the same now -> the second finds nothing due,
           because every run job is RUNNING-then-moved or finished
        """
        due = [job for job in jobs.values() if job.isDue(now)]
        due.sortBy(job -> job.nextRunAt)
        for job in due
            if job.isDue(now)                 # re-check: an earlier task may have cancelled it
                runOnce(job, now)


    private runOnce(job, now):
        """
        Core logic:
        1. Mark the attempt (attempts++, RUNNING) BEFORE calling the task
        2. Call task.run(executionKey); remember success / failure
        3. Failed and can retry -> schedule a retry at now + retryDelay (same slot, same key)
        4. Otherwise the run is over:
           recurring -> advance to the next slot after now
           one-off   -> SUCCEEDED or FAILED

        Edge cases:
        1. AT_MOST_ONCE failure -> never retried (canRetry is false)
        2. 3rd failed attempt of an AT_LEAST_ONCE run -> run is over
        3. the task cancelled its own job -> leave it CANCELLED
        4. recurring run failed for good -> ??? decision: keep the schedule (Req 3)
        """
        job.markRunning()
        # 💬 SAY: "I record the attempt BEFORE running. In memory it barely
        #    matters, but once this is persisted it IS the at-most-once
        #    guarantee: if we crash mid-run, the record says 'started', so we
        #    know not to run it again."

        succeeded = true
        try
            job.task.run(job.executionKey())
        catch error
            succeeded = false
            log(job.id, error)

        if job.status == CANCELLED                       # edge case 3
            return
        if !succeeded && job.canRetry(maxAttempts)       # Req 3
            job.scheduleRetry(now + retryDelay)
            return
        if job.isRecurring()
            job.advanceToNextSlot(now)                   # ??? decision: a lost run doesn't stop the schedule
        else
            job.finish(succeeded ? SUCCEEDED : FAILED)

        # ⚠️ TRICKY: catch errors PER JOB. One throwing task must not stop the
        #    other due jobs in this tick.


class Job:
    advanceToNextSlot(now):
        """
        Core logic:
        1. next = scheduledFor + interval
        2. If next is not after now (we were down), jump to the first slot after now
        3. scheduledFor = nextRunAt = next
        4. attempts = 0, status = SCHEDULED       (a NEW run gets fresh attempts)

        Edge cases:
        1. missed many slots -> skip them with arithmetic, not one tick per slot
        """
        next = scheduledFor + interval
        if next <= now
            missed = floor((now - scheduledFor) / interval)
            next = scheduledFor + (missed + 1) * interval
        scheduledFor = next
        nextRunAt = next
        attempts = 0
        status = SCHEDULED
    # 💬 SAY: "I anchor on scheduledFor, not on now, so a 09:00 daily job stays
    #    on 09:00 even if it actually ran at 09:00:30. That's fixed-RATE.
    #    Anchoring on now would be fixed-DELAY, and it drifts."


    canRetry(maxAttempts):
        return guarantee == AT_LEAST_ONCE && attempts < maxAttempts

    executionKey():
        return id + "@" + scheduledFor      # same for every retry of this run


class Scheduler:
    cancel(jobId):
        """
        Core logic:
        1. Look up the job
        2. Tell it to cancel itself

        Edge cases:
        1. unknown id -> throw JobNotFound
        2. already SUCCEEDED / FAILED / CANCELLED -> no-op (idempotent)
        3. currently RUNNING (task cancels its own job) -> CANCELLED; runOnce sees it after run
        """
        job = jobs[jobId]
        # 💬 SAY: "In my pseudo-code a map miss returns null."
        if job == null
            throw Error("job not found")
        job.cancel()              # inside Job: if status is terminal, return

    getStatus(jobId):
        job = jobs[jobId]
        if job == null
            throw Error("job not found")
        return job.status

    # 💬 SAY (product trade-off): "Finished jobs stay in the map so getStatus
    #    keeps answering. A one-off job is a few hundred bytes; at 100k jobs a
    #    day that's fine for weeks. A cleanup that drops finished jobs after N
    #    days handles the rest."
    # 🎯 GRADED: Communication (you raise the trade-off instead of quietly picking).


# ---------------------------------------------------------------------------
# ⚠️ TRICKY #5: the version most candidates write first (a real bug; spotting
#    it is the SENIOR signal of this question)
# ---------------------------------------------------------------------------

# ---- AS WRITTEN (one time field, one lifetime counter) ----
tick(now):
    for job in jobs.values()
        if job.status == SCHEDULED && job.nextRunAt <= now
            job.attempts += 1
            try
                job.task.run()
                if job.isRecurring()
                    job.nextRunAt = job.nextRunAt + job.interval
                else
                    job.status = SUCCEEDED
            catch error
                if job.guarantee == AT_LEAST_ONCE && job.attempts < maxAttempts
                    job.nextRunAt = now + retryDelay
                else
                    job.status = FAILED

# What goes wrong, for a daily 09:00 AT_LEAST_ONCE job (3 attempts, 30 s apart):
#   1. The retry OVERWRITES the schedule. Day 1 fails at 09:00, retries at
#      09:00:30 and succeeds. nextRunAt + interval = day 2 09:00:30. Every
#      retry pushes the job later, forever.
#   2. attempts is a LIFETIME counter, and it counts successful runs too. After
#      day 1 (fail + retry) it is already 2. Day 2's first try makes it 3, so
#      if day 2 fails there is NO retry. From the 3rd run ever, any failure is final.
#   3. And when that happens, a RECURRING job goes to FAILED and its schedule
#      dies. One bad night kills a daily job (here: on day 2).
#   4. No stable key: an idempotent job can't tell "retry of the 09:00 run"
#      from "a new run".
#   (Smaller ones: iterating the live map while a task may schedule a job; a
#    tick after downtime runs once per tick until it has caught up.)
#
# ✅ BEST FIX: one root cause, so one fix. "The schedule" and "this run" are two
#    different things. Keep scheduledFor (the slot, and the execution key) apart
#    from nextRunAt (the next try), reset attempts when a NEW slot starts, and
#    compute the next slot from scheduledFor. That's the BEST version above
#    (runOnce + advanceToNextSlot).
# 💬 SAY: "Retries belong to a run, not to the job. So the retry moves
#    nextRunAt but not scheduledFor, and a new slot gets fresh attempts."


"""
VERIFICATION TRACE (spend 1-2 min on this; it catches bugs out loud)
    maxAttempts = 3, retryDelay = 30 s. Times are day/hh:mm:ss.
    J1 = scheduleOnce(emailTask, D1 09:00, AT_MOST_ONCE)
    J2 = scheduleRecurring(reportTask, D1 09:00, 1 day, AT_LEAST_ONCE)

    tick(D1 08:59)      -> nothing due
    tick(D1 09:00)      -> J1 runs, throws -> FAILED (never retried)
                           J2 runs key "J2@D1 09:00", throws -> attempts 1, retry at 09:00:30
    tick(D1 09:00:30)   -> J2 runs key "J2@D1 09:00" (SAME key), succeeds
                           -> slot D2 09:00 (not 09:00:30), attempts 0
    tick(D2 09:00)      -> J2 runs key "J2@D2 09:00", succeeds -> slot D3 09:00
    (scheduler down)
    tick(D5 10:00)      -> J2 runs ONCE (key "J2@D3 09:00"), next slot D6 09:00; D4, D5 skipped
    cancel(J2)          -> CANCELLED; tick(D6 09:00) runs nothing
    cancel("nope")      -> throw "job not found"
    scheduleRecurring(task, D1, 0s, ...) -> throw "invalid interval"
"""


"""
--------------------------------------------------------------------------------
STEP 5: EXTENSIBILITY                                 ⏱ ~5 min   📸 00
--------------------------------------------------------------------------------
# ✅ BEST: say WHERE the change lands, describe it, code it if asked.
# 🎯 GRADED: Extensibility (small changes, no rewrite).
"""

# Extensions
"""
1. "Support cron expressions, like '0 9 * * MON-FRI'."
2. "Retries should back off exponentially, and differ per job."
3. "The process crashes mid-run. What do at-most-once and at-least-once mean now? Can you give exactly-once?"
4. "Run jobs on a pool of worker threads (or several machines)."
"""

# ---------------------------------------------------------------------------
# EXT 1: cron expressions
# Pattern: STRATEGY. Replace `interval` with a Schedule interface.
# Lands in: Job (interval -> schedule) and advanceToNextSlot (asks the schedule).
#    tick() and runOnce() are unchanged.
# 💬 SAY: "Today 'what's the next slot' is interval arithmetic inside Job. I'd
#    pull it behind a Schedule interface: OnceSchedule, FixedRateSchedule,
#    CronSchedule. For real cron syntax I'd use a library; I'd only hand-write
#    the subset we need."
# ---------------------------------------------------------------------------
interface Schedule:
    + firstSlot() -> timestamp
    + nextSlotAfter(slot, now) -> timestamp | null     # null = no more runs

class OnceSchedule      implements Schedule     # nextSlotAfter -> null
class FixedRateSchedule implements Schedule     # the arithmetic from advanceToNextSlot
class CronSchedule      implements Schedule     # first matching minute after max(slot, now)

Job.advanceToNextSlot(now):
    next = schedule.nextSlotAfter(scheduledFor, now)
    if next == null
        status = SUCCEEDED (or FAILED if the last run failed)   # a one-off is now just a schedule with no next slot
        return
    scheduledFor = nextRunAt = next; attempts = 0; status = SCHEDULED
# ⚠️ TRICKY: cron needs a TIME ZONE. "09:00" in which zone, and what happens on
#    the DST day when 02:30 doesn't exist or happens twice? Say it, pick UTC.


# ---------------------------------------------------------------------------
# EXT 2: per-job retry policy with exponential backoff
# Pattern: PROMOTE TWO CONSTANTS TO A VALUE CLASS (RetryPolicy).
# Lands in: Job gets a retryPolicy; canRetry and the retry time ask it. The
#    scheduler's two constants become the default policy.
# 💬 SAY: "maxAttempts and retryDelay become a RetryPolicy owned by the job.
#    Exponential backoff with a cap, plus jitter so a thousand jobs that failed
#    together don't all retry in the same second."
# ---------------------------------------------------------------------------
class RetryPolicy:
    - maxAttempts: int
    - initialDelay: Duration
    - multiplier: double          # 1.0 = fixed delay
    - maxDelay: Duration
    + canRetry(attempts) -> boolean
    + delayAfter(attempt) -> Duration     # min(initialDelay * multiplier^(attempt-1), maxDelay)

# in runOnce:
    if !succeeded && job.guarantee == AT_LEAST_ONCE && job.retryPolicy.canRetry(job.attempts)
        job.scheduleRetry(now + job.retryPolicy.delayAfter(job.attempts))


# ---------------------------------------------------------------------------
# EXT 3: crash mid-run; at-most-once vs at-least-once vs exactly-once
# Pattern: DURABLE STATE TRANSITIONS (a JobStore) + IDEMPOTENCY KEY.
# Lands in: Scheduler saves each Job through a JobStore at every transition,
#    plus a recover(now) method at startup. The Job class is unchanged.
# 💬 SAY: "The guarantee is decided by WHEN we write. At-most-once: save RUNNING
#    before calling the task, so after a crash we see RUNNING and give up.
#    At-least-once: on recovery, a RUNNING at-least-once job runs again with the
#    SAME key. Exactly-once isn't something a scheduler can promise across a
#    crash. You get an exactly-once EFFECT when an at-least-once job uses the
#    key as an idempotency key, e.g. the payment API dedupes on it."
# ---------------------------------------------------------------------------
interface JobStore:
    + save(job)
    + loadAll() -> Job[]

Scheduler.recover(now):       # called once, after loading jobs from the store
    for job in store.loadAll()
        if job.status == RUNNING                       # we crashed during this run
            if job.guarantee == AT_MOST_ONCE
                finishRun(job, failed)                 # might have run; must not run again
            else if job.canRetry(...)
                job.scheduleRetry(now)                 # same slot -> same key
            else
                finishRun(job, failed)
            store.save(job)
# ⚠️ TRICKY: at-most-once only works if the RUNNING write is DURABLE before
#    task.run() starts. Write after, and a crash in between re-runs the job.


# ---------------------------------------------------------------------------
# EXT 4: worker threads / several machines
# Pattern: ATOMIC CLAIM (compare-and-set SCHEDULED -> RUNNING).
# Lands in: Job gets tryClaim(now); tick() runs a job only if it won the claim.
#    jobs becomes a concurrent map.
# 💬 SAY: "The race is check-then-act: two workers both see the job due and
#    both run it. So 'is due' and 'mark running' must be one atomic step. In
#    one process that's a synchronized tryClaim. Across machines it's a
#    conditional update: UPDATE jobs SET status='RUNNING' WHERE id=? AND
#    status='SCHEDULED', plus a lease so a dead worker's job is picked up again."
# ---------------------------------------------------------------------------
Job.tryClaim(now) -> boolean:        # synchronized / atomic
    if !isDue(now) return false
    markRunning()
    return true


"""
================================================================================
 MORE FOLLOW-UPS SENIORS GET (short answers)
================================================================================
- A job hangs forever?  -> A timeout per job: run it on a worker with a deadline;
  on timeout count it as a failed attempt. Hard to really kill a thread, so the
  job must check for interruption.
- Millions of jobs?  -> Replace the scan with a min-heap or a time-bucketed
  "timing wheel" keyed by nextRunAt; on cancel/retry, lazily skip stale heap
  entries (check the job's real nextRunAt when you pop).
- Job B must run after job A?  -> A dependency DAG: B is SCHEDULED only when all
  its parents SUCCEEDED. New small class; tick is unchanged.
- Priorities when too many jobs are due?  -> Sort the due list by
  (priority, nextRunAt) and run at most N per tick.
- The same job submitted twice by a client retry?  -> An idempotent schedule
  call: the client sends a request id; we return the existing job id.
- Observability?  -> An Observer/listener on run outcome (succeeded, retried,
  failed) feeding metrics and alerts; the scheduler doesn't know about them.

================================================================================
 60-SECOND RECAP (say this at the end)
================================================================================
- Entities: Scheduler (orchestrator), Job (when + how many times), Task
  (interface: what). Time comes in as tick(now), so no threads in the core.
- Delivery guarantee per job: AT_MOST_ONCE is never retried, because an
  exception doesn't prove the side effect didn't happen. AT_LEAST_ONCE is
  retried with the SAME execution key so it can be idempotent. Exactly-once is
  an effect: at-least-once plus an idempotent consumer.
- The trap: a run and the schedule are different things. scheduledFor (the
  slot and the key) is separate from nextRunAt (the next try), and attempts
  reset per slot. Otherwise retries move the schedule and kill recurring jobs.
- Missed slots are skipped, not replayed. Errors are caught per job.
- Extensions land in one place each: cron = Schedule strategy, backoff =
  RetryPolicy value class, crashes = durable transitions + idempotency key,
  workers = atomic claim.
================================================================================
"""
