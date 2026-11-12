# Answer key 04: Implementation, no hints (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. The **reference bodies are the same as key 03** (I3-1 to I3-4); use them for the code checks. This key adds points for the student writing their own **Core logic / Edge cases** docstrings, and for planning before coding.

### I4-1: handle · 28 pts
- **Docstring (8):** core logic validate → account → estimate → reserve → call backend → actual → settle → return (5). Edge cases: invalid request; limit reached → error with type + reset; backend failure → release, charge 0 (3).
- **Body (20):** score with the I3-1 table, scaled to 20 (multiply by 20/28 and round).
- **−3** if the body doesn't follow the order of the docstring.

### I4-2: tryReserve · 22 pts
- **Docstring (6):** roll/start → find blocking → return latest-reset blocker → else reserve all (3); edge cases "both full → later reset" and "one full → reserve nothing" (3).
- **Body (16):** I3-2 table scaled to 16. The partial-reservation red flag applies at full size (−8).

### I4-3: settle · 12 pts
- **Docstring (4):** roll → release estimate → charge actual (2); edge cases "actual 0" and "reset during the request" (2).
- **Body (8):** I3-3 table scaled to 8.

### I4-4: rollIfExpired · 12 pts
- **Docstring (4):** expired → used 0, resetAt null (2); edge case "requests in flight across the reset → reserved kept" (2).
- **Body (8):** I3-4 table scaled to 8. The `reserved = 0` red flag applies (−4).

### I4-5: Estimate-size trade-off · 8 pts
- **Must:** reserving the **max** reply blocks users with usage left, after only a few parallel chats (3). Reserving a **typical** reply admits more but can **overshoot** by about one estimate (3). Settle corrects the number either way; estimate quality only changes the overshoot, not correctness (2).

### I4-6: Safety proofs · 10 pts
- **(a) (5):** check and reserve are **one atomic step** per user, so each chat sees the reservations of the chats admitted before it; once used + reserved reaches the limit the rest are rejected.
- **(b) (5):** settle runs in a **finally** with actual = 0, so the reservation is released and nothing is charged.
- A claim that the teammate's check-then-act version is safe: 0 for (a).

### I4-7: Verification trace · 8 pts
- Has real calls and the state after each (used/reserved/resetAt) (3); covers a happy path (1), a limit error naming the window (2), and a reset that zeroes used (2).

---
**Comparison with workbook 03:** the evaluator must report the score delta between 03 and 04 (see RULEBOOK §4). If 04 is more than 15 points below 03, the hints are carrying the student.
**Level calibration:** ≥85 senior · 70–84 mid · 50–69 junior.
