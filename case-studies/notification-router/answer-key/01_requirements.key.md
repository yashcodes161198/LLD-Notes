# Answer key 01: Requirements (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Score each item against its **Must**. Give **Partial** points where they're listed. Apply **Red flags** as deductions, and accept anything **Equivalent** in meaning. Don't penalise wording.

---

### Q1: The question categories · 10 pts
- **Must:** names all three: primary capabilities (or "core features / what it does"), error handling (or "failure cases / edge cases"), and scope boundaries (or "in/out of scope").
- **Partial:** 3 pts per category.
- **Bonus +1 (cap 10):** says the 4th category, "rules & completion", is for games and isn't needed here.
- **Model:** 1. primary capabilities 2. error handling 3. scope boundaries

### Q2: Primary-capability questions · 15 pts
| Topic | Pts | Expected answer |
|---|---|---|
| Which channels exist; is the list fixed? | 5 | EMAIL, SMS, PUSH; fixed for now |
| How the preference is set; one per user; can it change? | 5 | one per user, changeable, newest wins |
| Does the router send, or only decide and hand off? | 5 | decides and hands off to per-channel handlers; senders out of scope |
- **Accept also (+2, cap 15):** "what exactly do NORMAL / URGENT mean?" with the right answer.
- **Red flag −3:** the questions are about implementation (DB, Kafka, language) instead of behaviour.

### Q3: Error-handling questions · 25 pts
Each needs both the question and a plausible expected answer:
| Topic | Pts | Expected answer |
|---|---|---|
| NORMAL for a user with no preference | 7 | throw a specific error (accept "default to EMAIL" if they say it's the interviewer's call) |
| URGENT for a user with no preference | 5 | still goes to every channel |
| One handler fails during the URGENT fan-out | 9 | the others still go out; report which channel failed |
| A channel with no handler registered | 4 | configuration bug; fail fast at startup |
- **Partial:** a question with no expected answer gets half points.
- **Accept also (+2 each, cap 25):** duplicate submission of the same notification; invalid notification (missing user or priority).

### Q4: Scope-boundary question · 10 pts
- **Must:** asks whether retries / queues / templates / contact lookup / opt-outs belong to the router, and answers "no: route one notification and report the result".
- **Partial 5:** a scope question that only covers the senders (already given in the prompt).

### R: Final requirements · 25 pts (5 per requirement)
| # | Must contain | Pts |
|---|---|---|
| R1 | User sets/updates **one** preferred channel; **newest wins**; error on missing user/channel | 5 |
| R2 | A caller submits a notification with **id, user, priority, content**; **invalid → error** | 5 |
| R3 | NORMAL → **preferred channel only**; **no preference → specific error** | 5 |
| R4 | URGENT → **every channel, exactly once**; works **without a preference** | 5 |
| R5 | Hand each channel to its handler; **one failure doesn't stop the others**; **per-channel result**; missing handler → error at creation | 5 |
- **Partial:** 3/5 if the requirement is there but is missing the bolded detail.
- **Red flag −3:** invented requirements that were never agreed (retries, scheduling, user accounts in scope). −2 if the requirements are not numbered.

### S: Out of scope · 8 pts
- **Must:** handler implementations / actual delivery (3); contact lookup: email address, phone, device token (2); retries, queues, rate limits or opt-outs (3, any one of them).
- **Accept also:** templates, quiet hours, analytics (no extra points, no penalty).

### T: Talk track · 7 pts
- **T1 (2):** says they'll clarify requirements before designing. Red flag: starts with classes.
- **T2 (2):** "anything not on this list is out of scope", or equivalent.
- **T3 (3):** "**exactly once each**" (R4) catches the double-send of the preferred channel during URGENT (1.5), and "**one failure doesn't stop the others**" (R5) catches a fan-out loop that one exception aborts (1.5).

---
**Level calibration:** ≥85 senior-ready · 70–84 mid · 50–69 junior · <50 redo after re-reading Step 1 of `../notification_router.py`.
