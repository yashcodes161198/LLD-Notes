# Answer key 04: Implementation, no hints (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. The **reference bodies are the same as key 03** (I3-1 to I3-4); use them for the code checks. This key adds points for the student writing their own **Core logic / Edge cases** docstrings, and for planning before coding.

### I4-1: depositPackage · 22 pts
- **Docstring (8):** core logic has these steps in order: find → open → mark occupied → generate token → store → return code (5). Edge case "no compartment of that size → throw" (3).
- **Body (14):** score with the I3-1 table, scaled to 14 (multiply by 14/22 and round).
- **−3** if the body doesn't follow the order of the docstring.

### I4-2: Helper(s) · 10 pts
- `getAvailableCompartment(size)` with a docstring (scan → match size and free → first match; none → null) (4), and a correct body (6, per I3-2 scaled).
- **+0, no penalty:** inlining the scan in depositPackage. But the helper should be named after a core-logic step; if it's inlined, deduct 2 for readability.

### I4-3: pickup · 28 pts
- **Docstring (10):** core logic lookup → get compartment → open → mark free → remove (5); edge cases empty / unknown / expired, with **distinct expired error** (5).
- **Body (18):** I3-3 table scaled to 18.

### I4-4: openExpiredCompartments · 20 pts
- **Docstring (6):** the steps, plus an explicit **"remove from map?" decision** line (3); edge cases empty / none / all / twice (3).
- **Body (14):** I3-4 scale; version A max 10, version B max 14.
- **Extra state:** if they added `expiredCodes` (or similar) at the top with a reason, that's part of version B credit.

### I4-5: Product trade-off · 8 pts
- **Must:** deleting expired tokens makes a late customer see "invalid" instead of "expired", which is a worse experience (4). Keeping them costs memory, but that's tiny (~50 compartments × 100 days ≈ 5k entries) (2). A periodic cleanup handles very old ones (2).

### I4-6: Idempotency proof · 6 pts
- Version B: "the expired code is removed from the live map, so the second run can't find it and won't reopen the reused compartment" (6).
- Version A answered "yes, safe": 0, because it's wrong. Version A answered "no, because a reused compartment is reopened": 4, since they found the bug but didn't fix it.

### I4-7: Verification trace · 6 pts
- Has real calls and the state after each (3); covers one happy path and one error, such as invalid or expired (3).

---
**Comparison with workbook 03:** the evaluator must report the score delta between 03 and 04 (see RULEBOOK §4). If 04 is more than 15 points below 03, the hints are carrying the student.
**Level calibration:** ≥85 senior · 70–84 mid · 50–69 junior.
