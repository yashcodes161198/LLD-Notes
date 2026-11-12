"""
WORKBOOK 02: ENTITIES + CLASS DESIGN             Job Scheduler
Time box: 15 minutes. Closed book.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the agreed requirements (don't change them)
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

Out of scope: what tasks do (Task is an interface); threads/timers and the loop
that calls tick(); persistence, multiple workers, cron expressions, priorities,
dependencies, timeouts.
"""

# ===========================================================================
# PART A: ENTITIES                                         [E-items]
# ===========================================================================

# A1 [E1]. List EVERY noun in the requirements that could be a class (at least 6).
# - TODO

# A2 [E2]. For each noun: keep or drop, and why (one line each).
#     Format:  <Noun>: KEEP/DROP/ENUM/INTERFACE: <reason>
# - TODO

# A3 [E3]. Some nouns are dropped as classes but survive as something smaller
#     (a field, a constant, a derived value). Name two and say what they become.
# TODO

# A4 [E4]. "Isn't a job just a Runnable / a function pointer?" Answer it.
# TODO

# A5 [E5]. Any enums?
# TODO


# ===========================================================================
# PART B: CLASS DESIGN                                     [C-items]
#   Notation:   - field: Type        + method(params) -> ReturnType
#   Go TOP-DOWN (start with the orchestrator). Next to each member, add a
#   comment naming the requirement it comes from, e.g.  # Req 3      [C4]
# ===========================================================================

# [C1] class TODO_Orchestrator:
#     - TODO
#     + TODO


# [C2] class TODO:
#     - TODO
#     + TODO


# [C3] interface TODO:
#     + TODO


# ===========================================================================
# PART C: DESIGN DECISIONS (answer like you're talking to the interviewer)  [D-items]
# ===========================================================================

# C1 [D1]. How does tick() find due jobs? Would you use a priority queue? Why / why not?
# TODO

# C2 [D2]. Where does a job's status live, and why there? Name the principle.
# TODO

# C3 [D3]. How many time fields does a recurring job need, and what is each one for?
#     What breaks if you use only one?
# TODO

# C4 [D4]. Is `attempts` counted per job (lifetime) or per run? What breaks with the other?
# TODO

# C5 [D5]. Why is time a PARAMETER (tick(now)) instead of the scheduler owning a
#     clock and threads?
# TODO
