"""
WORKBOOK 05: EXTENSIBILITY                       Job Scheduler
Time box: 3 minutes per question (~20 min total). Closed book.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the finished base design. Scheduler holds Map<id, Job> and two
constants (3 attempts, 30 s). tick(now) scans for due jobs and calls
runOnce(job, now): markRunning -> task.run(executionKey) -> retry (AT_LEAST_ONCE
only, same slot) or advanceToNextSlot (recurring) or finish (one-off).
Job keeps scheduledFor (the slot, used in the key) apart from nextRunAt (the
next try), resets attempts per slot, and computes the next slot from
scheduledFor + interval, skipping missed slots.

For EACH question, fill in all four parts:
  Pattern:  the reusable design move (name it)
  Lands in: exactly which class(es) and method(s) change; everything else stays the same
  Say:      what you'd say out loud, 2-3 sentences
  Code:     the minimal code delta (pseudo-code). Write "describe only" if you'd
            only talk it through, but you still need the Pattern and Lands-in.
"""

# X1. "Support cron expressions, like '0 9 * * MON-FRI'."
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X2. "Retries should back off exponentially, and be configurable per job."
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X3. "The process crashes in the middle of running a job. What do at-most-once
#      and at-least-once mean now? Can you give me exactly-once?"
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X4. "Run jobs on a pool of worker threads. Make sure no job runs twice."
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X5. "We now have 10 million scheduled jobs."
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X6. "A job sometimes hangs forever."
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO
