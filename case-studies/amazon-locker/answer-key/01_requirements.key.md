# Answer key 01: Requirements (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Score each item against its **Must**. Give **Partial** points where they're listed. Apply **Red flags** as deductions, and accept anything **Equivalent** in meaning. Don't penalise wording.

---

### Q1: The question categories · 10 pts
- **Must:** names all three: primary capabilities (or "core features / what it does"), error handling (or "failure cases / edge cases"), and scope boundaries (or "in/out of scope").
- **Partial:** 3 pts per category.
- **Bonus +1 (cap 10):** mentions a 4th, "rules & completion", as optional for games or workflows.
- **Model:** 1. primary capabilities 2. error handling 3. scope boundaries

### Q2: Primary-capability questions · 15 pts
- **Must (7 pts each):** a question that uncovers **compartment sizes** (expected answer: S/M/L, match the size), and a question about **how the customer gets the code** (expected answer: out of scope, just return it).
- **+1:** any other sensible capability question (e.g. "can the customer choose the locker?" → out of scope).
- **Red flag −3:** the questions are about implementation (DB, language) instead of behaviour.

### Q3: Error-handling questions · 25 pts
Each needs both the question and a plausible expected answer:
| Topic | Pts | Expected answer |
|---|---|---|
| Multiple packages per customer / token uniqueness | 7 | yes, one token per package |
| Code expiry / never picked up | 9 | 7 days, then staff clear it |
| All compartments of a size full | 9 | throw an error (or fall back, if the user chose that; accept either if stated) |
- **Partial:** a question with no expected answer gets half points.
- **Accept also (+2 each, cap 25):** a wrong or empty code entered; brute-force attempts.

### Q4: Scope-boundary question · 10 pts
- **Must:** asks whether we model the whole delivery flow or only from the driver arriving at the locker to customer pickup, and answers "only the locker part".
- **Partial 5:** a scope question without the end-to-end vs locker-only framing.

### R: Final requirements · 25 pts (5 per requirement)
| # | Must contain | Pts |
|---|---|---|
| R1 | Carrier deposits with **size**; system assigns a matching **available** compartment, **opens** it; **error if no space** | 5 |
| R2 | An access token/code is generated and **returned** on deposit; **one per package** | 5 |
| R3 | User retrieves by entering the code; system validates it and opens the compartment; **specific errors for invalid AND expired** | 5 |
| R4 | Tokens **expire after 7 days**; expired codes are rejected; **the package stays** until staff remove it | 5 |
| R5 | **Staff open all expired compartments** to remove packages | 5 |
- **Partial:** 3/5 if the requirement is there but is missing the bolded detail.
- **Red flag −3:** invented requirements that were never agreed (payments, notifications in scope, user accounts). −2 if the requirements are not numbered.

### S: Out of scope · 8 pts
- **Must (4 each):** delivery logistics / how the package arrives; SMS/email notification of the code.
- **Accept also:** lockout after failed attempts, multiple locker stations, payments (no extra points, no penalty).

### T: Talk track · 7 pts
- **T1 (2):** says they'll clarify requirements before designing. Red flag: starts with classes.
- **T2 (2):** "anything not on this list is out of scope", or equivalent.
- **T3 (3):** identifies "throws specific error if code is invalid **or** expired" → two different errors, because the requirement says *specific*, and a customer who is late should learn the code expired rather than being told it's wrong.

---
**Level calibration:** ≥85 senior-ready · 70–84 mid · 50–69 junior · <50 redo after re-reading Step 1 of `../amazon_locker.py`.
