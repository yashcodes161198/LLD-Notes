# Answer key 01: Requirements (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Score each item against its **Must**. Give **Partial** points where they're listed. Apply **Red flags** as deductions, and accept anything **Equivalent** in meaning. Don't penalise wording.

---

### Q1: The question categories · 10 pts
- **Must:** names all three: primary capabilities (or "core features / what it does"), error handling (or "failure cases / edge cases"), and scope boundaries (or "in/out of scope").
- **Partial:** 3 pts per category.
- **Bonus +1 (cap 10):** mentions a 4th, "rules & completion", as optional for games or workflows.
- **Model:** 1. primary capabilities 2. error handling 3. scope boundaries

### Q2: Primary-capability questions · 15 pts
| Topic | Pts | Expected answer |
|---|---|---|
| One-off vs recurring, and what "recurring" means (interval vs cron) | 7 | both; recurring = start + fixed interval; cron is a follow-up |
| Do we care what the job does? | 5 | no: the client hands us a task; we decide when and how many times |
| Cancel / query a job | 3 | yes, by job id |
- **Partial:** a question with no expected answer gets half points.
- **Red flag −3:** the questions are about implementation (DB, language, thread pool size) instead of behaviour.

### Q3: Error-handling questions · 25 pts
Each needs both the question and a plausible expected answer:
| Topic | Pts | Expected answer |
|---|---|---|
| What happens when a job throws? | 6 | retry, 3 attempts in total, 30 s apart (any concrete numbers are fine) |
| Is every job safe to retry? / delivery guarantee | 9 | no: each job declares AT_MOST_ONCE (never retried) or AT_LEAST_ONCE (retried, must be idempotent) |
| Missed runs after downtime / late tick | 4 | run once, skip the missed slots (catch-up is accepted only if stated as a choice) |
| A recurring job's run fails for good | 3 | that run is lost; the schedule continues |
| Unknown job id on cancel/query | 3 | specific "job not found" error |
- **Partial:** a question with no expected answer gets half points.
- **Accept also (+2 each, cap 25):** how a retry knows it's a retry (execution / idempotency key); job scheduled in the past; cancel while running.

### Q4: Scope-boundary questions · 10 pts
- **Must (5 each):** single process in memory vs durable/distributed (answer: in memory, one process); who drives time / do we own threads (answer: an outside loop calls `tick(now)`; jobs run inside tick).
- **Partial 3:** a scope question without either framing.

### R: Final requirements · 25 pts (5 per requirement)
| # | Must contain | Pts |
|---|---|---|
| R1 | Client schedules **one-off or recurring** with a **delivery guarantee**; returns a **job id**; error on missing task / **non-positive interval** | 5 |
| R2 | Ticked with **now**, runs every **due** job; each run gets an **execution key**; recurring moves to the **next slot after now, skipping missed** | 5 |
| R3 | Retry **only AT_LEAST_ONCE**, **per run**, with the **same key**; AT_MOST_ONCE never re-run; one-off → FAILED, recurring **keeps its schedule** | 5 |
| R4 | Cancel by id; never runs again; **error if unknown**; finished → no-op | 5 |
| R5 | Query status (the 5 states); error if unknown | 5 |
- **Partial:** 3/5 if the requirement is there but missing the bolded detail.
- **Red flag −3:** invented requirements that were never agreed (DAGs, priorities, persistence in scope). −2 if the requirements are not numbered.

### S: Out of scope · 8 pts
- **Must:** what tasks actually do / task implementations (3); threads, timers and the tick loop (3); persistence or multiple workers (2).
- **Accept also:** cron expressions, priorities, dependencies, timeouts (no extra points, no penalty).

### T: Talk track · 7 pts
- **T1 (2):** says they'll pin down requirements (especially failure and retry policy) before designing. Red flag: starts with classes.
- **T2 (3):** an exception doesn't prove the side effect didn't happen (2), with an example like sending an email or charging a card (1).
- **T3 (2):** the scheduler can only give at-most-once or at-least-once across failures; exactly-once *effect* = at-least-once + an idempotent job keyed by a stable execution/idempotency key.

---
**Level calibration:** ≥85 senior-ready · 70–84 mid · 50–69 junior · <50 redo after re-reading Step 1 of `../job_scheduler.py`.
