# Answer key 05: Extensibility (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Each question is scored on **Pattern / Lands in / Say / Code**. "Describe only" for Code is acceptable for X4–X6 (the code points go to the description instead). For X1–X3 a code delta is expected.

General red flag for any question, **−5:** the answer rewrites `handle()` or touches many classes when one place would do.

---

### X1: Plans + upgrade mid-window · 20 pts
- **Pattern (4):** config becomes a **lookup**: a `Plan` (enum or table) with limits or a multiplier.
- **Lands in (4):** `UsageAccount.applyPlan(plan)` + `UsageWindow.setLimit()`; a `setPlan(userId, plan)` entry point. `handle()` unchanged.
- **Say (5):** on upgrade change the **limit**, keep **used** (and reserved) (3); don't reset the window, or toggling plans becomes a free refill (2).
- **Code (5):**
```
enum Plan: FREE(x1), PRO(x5), MAX(x20)
UsageAccount.applyPlan(plan):           # synchronized
    for w in windows: w.setLimit(w.baseLimit * plan.multiplier)
```
- **Must mention (2):** a downgrade can leave used > limit; the user is just blocked until reset.

### X2: Better estimates · 18 pts
- **Pattern (5):** **Strategy**: a `UsageEstimator` interface with `expectedReplyTokens(userId, model)` and `record(...)`.
- **Lands in (4):** `estimateUnits()` + one `record()` call on the success path of `handle()`.
- **Say (5):** a per-(user, model) **moving average** of output tokens, seeded with the typical value (3); estimate quality only shrinks the overshoot, settle keeps the numbers correct (2).
- **Code (4):** `avg = 0.8 * avg + 0.2 * actual` (any smoothing / average accepted).

### X3: OPUS-only weekly limit · 20 pts
- **Pattern (5):** a window gets a **scope** (set of models); the account only touches windows that apply.
- **Lands in (5):** `UsageWindow.appliesTo(model)`, the loops in `tryReserve`/`settle` filter on it, `LimitType.OPUS_WEEKLY`. No new class for "OPUS limit".
- **Say (4):** "this is why the account holds a list of windows"; HAIKU skips it, OPUS needs room in all three.
- **Code (4):** `relevant = [w for w in windows if w.appliesTo(model)]`.
- **Must mention (2):** `settle` must filter with the **same model**, or it releases from a window it never reserved in.
- **Red flag −4:** a separate `opusUsed` counter on the account, outside the window list (duplicated window logic).

### X4: 500 parallel chats · 14 pts
- **Pattern (5):** **cap concurrency per user**: an in-flight counter on the account checked in `tryReserve`.
- **Lands in (4):** `UsageAccount` (`inFlight`, `maxInFlight`), one new error ("too many concurrent requests").
- **Say (5):** reservations already bound the **overshoot** (each chat sees the others), so what's left is **load** (2); cap in-flight per user, reject with a **different** error since it isn't a usage limit (3).
- **Red flag −4:** answers "lock the whole handle() per user" (kills parallel chats).

### X5: 50 servers · 16 pts
- **Pattern (5):** shared state with an **atomic** reserve (e.g. a Redis Lua script or a DB row update with a condition).
- **Lands in (3):** the account's storage; `handle()` unchanged.
- **Say (5):** the per-process lock no longer works (2); a crash between reserve and settle has **no finally**, so reservations need a **TTL** (3).
- **Describe (3):** what is atomic: roll the windows + check all + reserve in all, in **one** round trip.
- **Bonus +2 (cap 16):** local leases of a block of units per server to cut round trips, at the cost of a larger overshoot.
- **Red flag −3:** "use a database transaction" with no explanation of what is atomic.

### X6: Streaming, mid-answer limit · 12 pts
- **Pattern (4):** **incremental settle** (charge per chunk).
- **Lands in (3):** the settle step in `handle()` (called per chunk with the delta).
- **Say (5):** describe the hard stop (finish the sentence, stop, mark truncated) (3) **and** push back that the agreed rule is "finish and charge", since cutting answers is a worse experience (2).

---
**Level calibration:** ≥80 senior/staff · 60–79 senior-ish mid · 40–59 mid · <40 junior (juniors often never reach this step, so don't be discouraged).
