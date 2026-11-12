"""
WORKBOOK 04: IMPLEMENTATION (NO HINTS)           Job Scheduler
Time box: 12 minutes. Closed book. Do this on a DIFFERENT day from workbook 03.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the class design (same as workbook 03)
    class Scheduler:
        - jobs: Map<string, Job>   - maxAttempts = 3   - retryDelay = 30s
        + scheduleOnce(task, runAt, guarantee) -> jobId
        + scheduleRecurring(task, startAt, interval, guarantee) -> jobId
        + tick(now)   + cancel(jobId)   + getStatus(jobId)
    class Job:  id, task, interval | null, guarantee, status, scheduledFor, nextRunAt, attempts
        | isDue(now), isRecurring(), executionKey(), canRetry(maxAttempts), markRunning(),
          scheduleRetry(at), advanceToNextSlot(now), finish(status), cancel()
    interface Task:  run(executionKey)       # throws = this attempt failed
    enums: JobStatus {SCHEDULED, RUNNING, SUCCEEDED, FAILED, CANCELLED},
           DeliveryGuarantee {AT_MOST_ONCE, AT_LEAST_ONCE}

For EACH method:
  1. Write the docstring yourself: "Core logic:" (numbered steps) and
     "Edge cases:" (condition -> what happens).
  2. Then write the body so it follows your steps in order.
Add any private helper you need.
"""

class Scheduler:

    scheduleRecurring(task, startAt, interval, guarantee):
        """
        Core logic:
        TODO
        Edge cases:
        TODO
        """
        TODO                                               # [I4-1]


    tick(now):
        """
        Core logic:
        TODO
        Edge cases:
        TODO
        """
        TODO                                               # [I4-2]


    # helper(s), e.g. runOnce(job, now)
    """
    Core logic:
    TODO
    Edge cases:
    TODO
    """
    TODO                                                   # [I4-3]


class Job:

    advanceToNextSlot(now):
        """
        Core logic:
        TODO
        Edge cases:
        TODO
        """
        TODO                                               # [I4-4]


# ---------------------------------------------------------------------------
# Q1. Write the 2-3 sentences you'd say about why AT_MOST_ONCE jobs are never
#     retried, and what AT_LEAST_ONCE jobs must do in return.  [I4-5]
# TODO
#
# Q2. A daily 09:00 AT_LEAST_ONCE job fails at 09:00 and succeeds on the
#     09:00:30 retry. Prove in 1-2 lines that your code runs it at 09:00 the
#     next day, with fresh attempts, and that both day-1 attempts got the same
#     execution key.                                           [I4-6]
# TODO
#
# Q3. Write a 4-line verification trace (calls, then the resulting state) that
#     covers one happy path and one error.                     [I4-7]
# TODO
