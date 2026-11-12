# Answer key 04: Implementation, no hints (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. The **reference bodies are the same as key 03** (I3-1 to I3-4); use them for the code checks. This key adds points for the student writing their own **Core logic / Edge cases** docstrings, and for planning before coding.

### I4-1: allowRequest · 14 pts
- **Docstring (5):** core logic in order: validate id → get or create bucket → consume at now → return (3). Edge cases: empty id → throw; over the limit → return false, **not** an error (2).
- **Body (9):** score with the I3-1 table, scaled to 9 (multiply by 9/16 and round).
- **−3** if the body doesn't follow the order of the docstring.

### I4-2: getOrCreateBucket · 16 pts
- **Docstring (5):** core logic: look up limit → get or create and store (2). Edge case: **two threads, brand-new client → same bucket** (3).
- **Body (11):** I3-2 table scaled to 11 (multiply by 11/18 and round).
- **+0, no penalty:** inlining it in allowRequest. If it's inlined, deduct 2 for readability.

### I4-3: tryConsume · 20 pts
- **Docstring (7):** core logic: refill → check ≥ 1 → take one and return true / return false (3). Edge cases: two threads on one bucket → one critical section (2); denied consumes nothing (2).
- **Body (13):** I3-3 table scaled to 13 (multiply by 13/22 and round).

### I4-4: refill · 22 pts
- **Docstring (8):** the three steps (3). Edge cases: elapsed ≤ 0; long idle → cap; **calls closer than one token-interval → keep the fraction**, written as a decision (5; the fraction line alone is worth 3).
- **Body (14):** I3-4 scale: version A max 6, version B max 14.
- **Extra state:** none is needed. If they added a remainder field instead of decimal tokens, that's version B.

### I4-5: Trade-off: the map never shrinks · 10 pts
- **Must:** memory maths: a bucket is a few fields (~100 bytes with overhead), so a million clients ≈ 100 MB, fine for one server (4). A bucket idle longer than **capacity / refillPerSecond is full again**, the same as a fresh one, so evicting it changes nothing a client can see (4). Do it later as a periodic sweep, removing with `remove(key, bucket)` so it can't delete a bucket a request thread just replaced (2).
- **Partial:** "add a TTL / LRU" with no reason why it's safe = 4 of the last 6.

### I4-6: No-starvation proof · 10 pts
- Version B: "each call adds 0.5 and nothing is thrown away, so every second call reaches 1.0 and is allowed: one per second" (10).
- Version A answered "yes, it works": 0, because it's wrong. Version A answered "no, it starves, because floor(0.5) = 0 and lastRefill is reset": 6, since they found the bug but didn't fix it.

### I4-7: Verification trace · 8 pts
- Has real calls and the state (tokens, lastRefill, result) after each (4); covers one happy path and one error, such as an empty id or a denial (4).

---
**Comparison with workbook 03:** the evaluator must report the score delta between 03 and 04 (see RULEBOOK §4). If 04 is more than 15 points below 03, the hints are carrying the student.
**Level calibration:** ≥85 senior · 70–84 mid · 50–69 junior. Without a version-B refill or a correct I4-6, cap at "mid".
