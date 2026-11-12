# Answer key 01: Requirements (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Score each item against its **Must**. Give **Partial** points where they're listed. Apply **Red flags** as deductions, and accept anything **Equivalent** in meaning. Don't penalise wording.

---

### Q1: The question categories · 10 pts
- **Must:** names all three: primary capabilities (or "core features / what it does"), error handling (or "failure cases / edge cases"), and scope boundaries (or "in/out of scope").
- **Partial:** 3 pts per category.
- **Bonus +1 (cap 10):** mentions a 4th, "rules & completion", as optional for games or workflows.
- **Model:** 1. primary capabilities 2. error handling 3. scope boundaries

### Q2: Primary-capability questions · 15 pts
- **Must (7 pts each):**
  - a question about **what we limit by** (user / API key / IP). Expected answer: a client id (API key) passed in as a string.
  - a question about **the limits themselves, including whether bursts are allowed**. Expected answer: per tier (FREE 10 burst then 1/s, PREMIUM 100 then 10/s); short bursts are fine, the average rate is enforced. A limits question with no burst angle gets 4.
- **+1 (cap 15):** any other sensible capability question (e.g. "what does the caller get back?" → allowed/denied, the gateway sends 429).
- **Red flag −3:** the questions are about implementation (Redis, which language, which data structure) instead of behaviour.

### Q3: Error-handling questions · 25 pts
Each needs both the question and a plausible expected answer:
| Topic | Pts | Expected answer |
|---|---|---|
| Client over its limit: what happens, and does a denied request count? | 9 | deny (return false, not an error); denied requests don't use capacity |
| Concurrent calls, including for the same client | 9 | yes, many request threads; the limit must hold exactly |
| Missing / empty / unknown client id | 7 | throw (caller bug) |
- **Partial:** a question with no expected answer gets half points.
- **Accept also (+2 each, cap 25):** clock changes / time going backwards; what happens to a client's state when it is idle for a long time (memory).

### Q4: Scope-boundary question · 10 pts
- **Must:** asks whether the limit must hold across **many API servers** or only within one process, and answers "one process; multi-server is a follow-up".
- **Partial 5:** a scope question without the single-vs-many-servers framing (e.g. only "who decides the tier?").

### R: Final requirements · 25 pts (5 per requirement)
| # | Must contain | Pts |
|---|---|---|
| R1 | The gateway asks with **client id (+ tier)**; system returns **allowed/denied**; **error on empty id** | 5 |
| R2 | Each tier has a **capacity (burst) and a refill rate**, with the agreed numbers | 5 |
| R3 | **Burst up to capacity, then the average rate**; refill is continuous and **capped**; **denied requests don't consume** | 5 |
| R4 | Limits are **per client** (isolated); a **new client starts full** | 5 |
| R5 | **Concurrent callers**; never admits more than the limit | 5 |
- **Partial:** 3/5 if the requirement is there but is missing the bolded detail.
- **Red flag −3:** invented requirements that were never agreed (distributed limits, 429 headers, per-endpoint rules, persistence in scope). −2 if the requirements are not numbered.

### S: Out of scope · 8 pts
- **Must (4 each):** limiting across many servers; building the HTTP response / headers.
- **Accept also:** how the tier is determined, per-endpoint limits, persistence across restarts (no extra points, no penalty).

### T: Talk track · 7 pts
- **T1 (2):** says they'll clarify requirements before designing. Red flag: opens with "I'll use a token bucket".
- **T2 (2):** "anything not on this list is out of scope", or equivalent.
- **T3 (3):** identifies "burst up to capacity, **then** held to the refill rate" (Req 3). A counter that resets each window lets a full window's worth through just before the reset and another just after, so **2× the burst** in a moment, which breaks that requirement.

---
**Level calibration:** ≥85 senior-ready · 70–84 mid · 50–69 junior · <50 redo after re-reading Step 1 of `../rate_limiter.py`.
