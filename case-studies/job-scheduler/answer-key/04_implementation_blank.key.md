# Answer key 04: Implementation, no hints (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. The **reference bodies are the same as key 03** (I3-1 to I3-4); use them for the code checks. This key adds points for the student writing their own **Core logic / Edge cases** docstrings, and for planning before coding.

### I4-1: scheduleRecurring · 12 pts
- **Docstring (4):** core logic validate → create job (slot = next run = start) → store → return id (2); edge cases missing task/guarantee, interval ≤ 0, start in the past (2).
- **Body (8):** I3-1 table scaled to 8 (multiply by 8/12 and round).

### I4-2: tick · 14 pts
- **Docstring (5):** collect due → earliest first → run each (2); edge cases nothing due, a task throws, a task changes the job map (3).
- **Body (9):** I3-2 table scaled to 9.

### I4-3: runOnce (or the inlined equivalent) · 30 pts
- **Docstring (10):** core logic mark attempt **before** run → run with key → retry if allowed (same slot) → else advance (recurring) or finish (one-off) (5); edge cases AT_MOST_ONCE never retried, last attempt, cancelled during run, recurring final failure keeps the schedule (5).
- **Body (20):** I3-3 table scaled to 20.
- **−2** if it's inlined in tick with no helper: the loop body no longer reads against a docstring.
- **−3** if the body doesn't follow the order of the docstring.

### I4-4: advanceToNextSlot · 18 pts
- **Docstring (6):** core logic from scheduledFor → skip missed → set both fields → reset attempts (4); edge case: missed many slots (2).
- **Body (12):** I3-4 table scaled to 12.

### I4-5: The delivery-guarantee talk · 8 pts
- **Must:** an exception doesn't prove the side effect didn't happen (e.g. the email went out, then logging threw), so retrying could do it twice (4). AT_LEAST_ONCE jobs must be **idempotent** (2), using the execution key that stays the same across retries (2).

### I4-6: Proof the trap doesn't happen · 10 pts
- Retry sets only nextRunAt = 09:00:30; scheduledFor stays day 1 09:00, so both attempts' key is `id@D1 09:00` (4).
- After success, next slot = scheduledFor + 1 day = **day 2 09:00** (3), and attempts = 0 (3).
- A one-field version answered "yes, it runs at 09:00": 0 (wrong statement, §3.1.8). A one-field version answered "no, it drifts to 09:00:30": 4, since they found the bug but didn't fix it.

### I4-7: Verification trace · 8 pts
- Has real calls and the state after each (4); covers one happy path and one error, such as invalid interval, unknown id, or an AT_MOST_ONCE failure going to FAILED (4).

---
**Comparison with workbook 03:** the evaluator must report the score delta between 03 and 04 (see RULEBOOK §4). If 04 is more than 15 points below 03, the hints are carrying the student.
**Level calibration:** ≥85 senior · 70–84 mid · 50–69 junior. Without the two-field design (I4-6 below 6), cap at "mid".
