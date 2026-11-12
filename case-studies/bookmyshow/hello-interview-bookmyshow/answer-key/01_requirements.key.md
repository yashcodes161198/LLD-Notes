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
| Is there a **hold / timer** while the user pays? | 6 | yes, 10 minutes, then payment confirms |
| Do **we process payment**? | 5 | no, a payment service calls us with a reference |
| **Several seats** in one booking? | 4 | yes, same show |
- **Partial:** a question with no expected answer gets half points.
- **Accept also (+1 each, cap 15):** start from a show vs browse movies (→ start from a show); seat types (→ REGULAR/PREMIUM, pricing out of scope).
- **Red flag −3:** the questions are about implementation (DB, language, microservices) instead of behaviour.

### Q3: Error-handling questions · 25 pts
Each needs both the question and a plausible expected answer:
| Topic | Pts | Expected answer |
|---|---|---|
| **Two users pick the same seat** at the same moment | 8 | exactly one wins, the other gets "seat unavailable" |
| **One of several seats is taken** | 6 | all-or-nothing, name the taken seat |
| User **never pays** | 6 | hold expires after 10 min, seats go back on sale |
| User **cancels** | 5 | allowed for a hold or a confirmed booking; refund out of scope |
- **Partial:** a question with no expected answer gets half points.
- **Accept also (+2 each, cap 25):** payment arrives after the hold expired (→ "hold expired", refund elsewhere); duplicate seat in one request.

### Q4: Scope-boundary questions · 10 pts
- **Must (5):** only booking seats of an existing show, vs theatre/screen/show administration or search. Expected: only booking.
- **Must (5):** one process vs many servers. Expected: one process with concurrent requests; multi-server is a follow-up.
- **Partial 3 each:** a scope question without the expected answer.

### R: Final requirements · 25 pts (5 per requirement)
| # | Must contain | Pts |
|---|---|---|
| R1 | User views **available** seats of a show (not booked, not held by an **unexpired** hold); unknown show → error | 5 |
| R2 | User holds one or more seats; **all-or-nothing**; **10-minute** hold returns a booking id; **two racing users → exactly one wins** | 5 |
| R3 | User confirms with a **payment reference**; booking CONFIRMED; **distinct errors: not found AND hold expired** | 5 |
| R4 | Holds **expire after 10 min**; expired seats are **available again**; confirming an expired hold fails | 5 |
| R5 | User **cancels** a held or confirmed booking; seats become available; not found → error | 5 |
- **Partial:** 3/5 if the requirement is there but is missing the bolded detail.
- **Red flag −3:** invented requirements that were never agreed (pricing, refunds, notifications, search in scope). −2 if the requirements are not numbered.
- **Red flag −4:** concurrency appears nowhere in the requirements (neither R2's "exactly one wins" nor equivalent).

### S: Out of scope · 8 pts
- **Must:** payment processing/refunds (3), search/browse movies and theatres (3), and any one of: notifications, theatre/show administration, pricing, user accounts, multiple servers (2).
- **Accept also:** any other sensible item (no extra points, no penalty).

### T: Talk track · 7 pts
- **T1 (2):** says they'll clarify requirements before designing. Red flag: starts with classes.
- **T2 (2):** concurrency is the heart of a booking system and the prompt hides it; written as "two users racing for the same seat: exactly one wins" (or equivalent) so it's a testable requirement, not an implementation detail.
- **T3 (3):** R2/"exactly one wins"-style holds **together with** R4 "expired seats are available again" (2): after A's hold expires, B can take the seat while A's booking still lists it (1).

---
**Level calibration:** ≥85 senior-ready · 70–84 mid · 50–69 junior · <50 redo after re-reading Step 1 of `../bookmyshow.py`.
