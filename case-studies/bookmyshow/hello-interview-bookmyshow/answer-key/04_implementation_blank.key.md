# Answer key 04: Implementation, no hints (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. The **reference bodies are the same as key 03** (I3-1 to I3-4); use those tables for the code checks, scaled as stated. This key adds points for the student writing their own **Core logic / Edge cases** docstrings, and for planning before coding.

### I4-1: holdSeats · 20 pts
- **Docstring (6):** core logic validate → find show → under lock: now, create PENDING booking → show.hold → store → return id (3). Edge cases: invalid input incl. duplicates, unknown show, seat taken → nothing stored (3).
- **Body (14):** score with the I3-1 table (18), scaled to 14 (multiply by 14/18 and round).
- **−3** if the body doesn't follow the order of the docstring.

### I4-2: Show.hold · 20 pts
- **Docstring (6):** check ids → check all free → then write all (3). Edge cases: unknown seat, taken seat with nothing changed, expired holder counts as free (3).
- **Body (14):** I3-2 table (20) scaled to 14.
- **Accept:** the lock taken inside `hold` instead of in the caller, if `now` is still read under it.

### I4-3: confirmBooking · 20 pts
- **Docstring (6):** lookup → lock → now → status/expiry checks → confirm (3). Edge cases: not found, already confirmed (with a decision), cancelled, **expired as a distinct error** (3).
- **Body (14):** I3-3 table (20) scaled to 14.

### I4-4: cancelBooking + Show.release · 20 pts
- **Docstring (6):** lookup → lock → release → cancel (2), with the explicit **"only seats it still holds"** step (2). Edge cases: not found, already cancelled, expired hold whose seats were re-held (2).
- **Body (14):** I3-4 table (20) scaled to 14. AS WRITTEN release (remove by seat): max 8.

### I4-5: Product trade-off · 8 pts
- **Must:** payment services retry callbacks, so a second confirm of a CONFIRMED booking is a **no-op**, not an error the paying user would see (4). Payment arriving just after expiry is **rejected** per the agreed rule; the payment service refunds; a grace period is a product decision to **raise with the interviewer**, not decide alone (4).

### I4-6: Safety proof · 6 pts
- **Must:** release removes an entry only if `seatHolders[seat] == thisBooking`; a newer hold **overwrote** that entry, so the comparison fails and the newer booking's seat is untouched (4). And it runs under the show's lock, so no hold can interleave between the compare and the remove (2).
- Answered "safe" with the AS WRITTEN release: 0, because it's wrong. AS WRITTEN but answered "not safe, a re-held seat is freed": 3.

### I4-7: Verification trace · 6 pts
- Real calls and the state after each (2); one happy path (1), one error such as seat unavailable or hold expired (1), one race (two users, same seat → one id, one "seat unavailable") (2).

---
**Comparison with workbook 03:** the evaluator must report the score delta between 03 and 04 (see RULEBOOK §4). If 04 is more than 15 points below 03, the hints are carrying the student.
**Level calibration:** ≥85 senior · 70–84 mid · 50–69 junior. Without release-by-owner in I4-4 or the I4-6 proof, cap at "mid".
