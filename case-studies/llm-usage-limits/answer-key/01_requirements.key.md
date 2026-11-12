# Answer key 01: Requirements (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Score each item against its **Must**. Give **Partial** points where they're listed. Apply **Red flags** as deductions, and accept anything **Equivalent** in meaning. Don't penalise wording.

---

### Q1: The question categories · 6 pts
- **Must:** names all three: primary capabilities (or "core features"), error handling (or "failure / edge cases"), scope boundaries (or "in/out of scope").
- **Partial:** 2 pts per category.
- **Model:** 1. primary capabilities 2. error handling 3. scope boundaries

### Q2: Primary-capability questions · 18 pts
| Topic | Pts | Expected answer |
|---|---|---|
| What a request carries | 4 | userId, model, messages; **no cost** |
| How usage is **measured** | 6 | tokens (input + output) × a per-model weight. "Requests" alone = 2 pts |
| How the windows work | 5 | start at first use, reset after 5h / 7d (accept fixed calendar resets if stated as the interviewer's answer) |
| Both limits on every request / either blocks | 3 | yes, both; blocked if either is used up |
- **+2 (cap 18):** asks about per-user/plan limits (→ one plan for now) or a remaining-usage view.
- **Red flag −3:** questions about databases, languages or frameworks instead of behaviour.

### Q3: Error-handling questions · 22 pts
Each needs the question and a plausible expected answer:
| Topic | Pts | Expected answer |
|---|---|---|
| Cost unknown: when do we decide / estimate | 6 | estimate, then correct with the real cost |
| Parallel requests / chats | 6 | allowed, run in parallel, blocked only when it has run out |
| Admitted request turns out expensive: cut off or finish? | 5 | finishes, charged in full, next request blocked |
| Out of usage: what error | 3 | a specific error naming the limit and the reset time |
| Model call fails: charged? | 2 | no |
- **Partial:** a question with no expected answer gets half points.
- **Accept also (+1 each, cap 22):** invalid request; the window resetting while a request is running.

### Q4: Scope-boundary question · 8 pts
- **Must (5):** do we run the model or only gate it? → we gate it and call an external backend.
- **Must (3):** one server or many / persistence → one process, in memory, many threads.

### R: Final requirements · 30 pts (5 per requirement)
| # | Must contain | Pts |
|---|---|---|
| R1 | User sends userId + model + messages; system runs it and returns the result; **"invalid request"** error | 5 |
| R2 | Charged in units = **tokens × model weight**, against **both** windows | 5 |
| R3 | **Reserve an estimate** at admission; **release it and charge the actual** at the end; **charge nothing on failure** | 5 |
| R4 | Windows start at first use and **reset after their duration**; the error **names the limit and its reset time** | 5 |
| R5 | Many requests in flight; admission counts **used + reserved**; blocked only when that reaches a limit; an admitted request **completes even past the limit** | 5 |
| R6 | User can check **remaining usage per window** | 5 |
- **Partial:** 3/5 if the requirement is there but is missing the bolded detail.
- **Red flag −3:** invented requirements that were never agreed (billing, plans, auth in scope). −2 if not numbered.

### S: Out of scope · 6 pts
- **Must (2 each, any 3):** running the model / the backend; plans & billing; auth; streaming & mid-stream cutoff; exact tokenization; multiple servers / persistence.

### T: Talk track · 10 pts
- **T1 (2):** says they'll clarify requirements before designing, ideally flagging that the cost is unknown up front.
- **T2 (4):** names **"never exceed the limit"** vs **"never block while usage is left"** (2), and says they'd ask the interviewer which wins (2).
- **T3 (4):** parallel chats all check before any of them is charged, so they'd all see the same remaining usage (2); counting the **estimates of in-flight requests** in the check fixes it (2).

---
**Level calibration:** ≥85 senior-ready · 70–84 mid · 50–69 junior · <50 redo after re-reading Step 1 of `../llm_usage_limits.py`.
