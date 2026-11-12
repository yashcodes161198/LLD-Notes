"""
WORKBOOK 03: IMPLEMENTATION (GUIDED)             Job Scheduler
Time box: 12 minutes. Closed book. Pseudo-code is fine; any consistent syntax works.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the class design
    class Scheduler:
        - jobs: Map<string, Job>
        - maxAttempts: int = 3
        - retryDelay: Duration = 30s
        + scheduleOnce(task, runAt, guarantee) -> jobId
        + scheduleRecurring(task, startAt, interval, guarantee) -> jobId
        + tick(now) -> void
        + cancel(jobId) -> void | error
        + getStatus(jobId) -> JobStatus | error
    class Job:
        - id  - task: Task  - interval: Duration | null  - guarantee: DeliveryGuarantee
        - status: JobStatus  - scheduledFor: timestamp  - nextRunAt: timestamp  - attempts: int
        + isDue(now)  + isRecurring()  + executionKey()  + canRetry(maxAttempts)
        + markRunning()  + scheduleRetry(at)  + advanceToNextSlot(now)  + finish(status)  + cancel()
    interface Task:  + run(executionKey)       # throws = this attempt failed
    enum JobStatus { SCHEDULED, RUNNING, SUCCEEDED, FAILED, CANCELLED }
    enum DeliveryGuarantee { AT_MOST_ONCE, AT_LEAST_ONCE }
    Settings: 3 attempts per run, 30 s apart. You may call generateId() without implementing it.

The core logic and edge cases are PRE-FILLED as hints. Write the METHOD BODY
under each "# YOUR CODE". Each core-logic step should become one or two lines.
"""

class Scheduler:

    scheduleRecurring(task, startAt, interval, guarantee):
        """
        Core logic:
        1. Validate the inputs
        2. Create a Job with scheduledFor = nextRunAt = startAt, status SCHEDULED
        3. Store it by id, return the id

        Edge cases:
        1. task / startAt / guarantee missing -> throw
        2. interval <= 0 -> throw (a different message)
        3. startAt in the past -> allowed: runs on the next tick
        """
        # YOUR CODE                                        [I3-1]
        TODO


    tick(now):
        """
        Core logic:
        1. Collect due jobs (SCHEDULED and nextRunAt <= now) into a list, earliest first
        2. Run each one with runOnce(job, now)

        Edge cases:
        1. nothing due -> no-op
        2. a task throws -> handled inside runOnce; the other due jobs still run
        3. a task schedules or cancels jobs while it runs -> don't iterate the live map
        """
        # YOUR CODE                                        [I3-2]
        TODO


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
        1. AT_MOST_ONCE failure -> never retried
        2. 3rd failed attempt of an AT_LEAST_ONCE run -> run is over
        3. the task cancelled its own job -> leave it CANCELLED
        4. recurring run failed for good -> keep the schedule
        """
        # YOUR CODE                                        [I3-3]
        TODO


class Job:

    advanceToNextSlot(now):
        """
        Core logic:
        1. next = scheduledFor + interval
        2. If next is not after now (we were down), jump to the first slot after now
        3. scheduledFor = nextRunAt = next
        4. attempts = 0, status = SCHEDULED

        Edge cases:
        1. missed many slots -> skip them all in one step
        """
        # YOUR CODE                                        [I3-4]
        TODO


# ---------------------------------------------------------------------------
# Q1. runOnce marks the attempt BEFORE calling the task. In memory that
#     barely matters. Why say it out loud anyway?              [I3-5]
# TODO
#
# Q2. cancel(jobId): what does a map lookup return for a missing key in your
#     pseudo-code, and what do you do for an already-finished job?   [I3-6]
# TODO
#
# Q3. Trace it. A candidate wrote tick() with ONE time field (nextRunAt) and
#     ONE counter (attempts), like this:
#         attempts += 1, then run the task
#         if job fails: if AT_LEAST_ONCE && attempts < 3: nextRunAt = now + 30s
#                       else status = FAILED
#         if job succeeds and recurring: nextRunAt = nextRunAt + interval
#     Daily AT_LEAST_ONCE job, first slot day 1 09:00.
#     Day 1: fails at 09:00, succeeds on the retry.
#     Day 2: the first try fails.
#     When does the day-2 run actually happen, what is attempts at that point,
#     and what happens to the job? What's the root cause, and what does your
#     version do instead?                                      [I3-7]
# TODO
