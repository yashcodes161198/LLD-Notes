# Answer key 03: Implementation, guided (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Any pseudo-code syntax is fine. Judge **logic, order and errors**, not syntax. Mentally run every body against the trace in I3-7 and the verification trace in `../llm_usage_limits.py`.

### I3-1: handle · 28 pts
```
if request == null                      throw Error("invalid request")
account = accounts.computeIfAbsent(request.userId, -> new UsageAccount(...))
estimate = estimateUnits(request)
blocking = account.tryReserve(estimate, clock.now())
if blocking != null                     throw UsageLimitExceeded(blocking.getType(), blocking.getResetAt())
actual = 0
try
    result = backend.generate(request.model, request.messages)
    actual = actualUnits(request.model, result)
    return result
finally
    account.settle(estimate, actual, clock.now())
```
| Check | Pts |
|---|---|
| Validates the request **before** creating/reserving anything | 3 |
| Gets or creates the account | 2 |
| Estimates, then reserves via **one** `tryReserve` call | 4 |
| Blocked → throws an error carrying **type and reset time** | 4 |
| Calls the backend **after** reserving and **outside** the lock | 4 |
| Settles in a **finally** (or catch + re-throw that also settles) | 6 |
| Settle charges **0 on failure**, the actual on success | 3 |
| Returns the result | 2 |
- **Red flag −6:** settles only on the success path (a backend error leaks the reservation forever). **−5:** wraps the whole method (including the backend call) in the account lock. **−3:** uses `now` from before the call for settle (accept if they say time doesn't matter there; it does across a reset).

### I3-2: tryReserve · 24 pts
```
for w in windows
    w.rollIfExpired(now); w.startIfIdle(now)
blocking = null
for w in windows
    if !w.hasRoom() and (blocking == null or w.getResetAt() > blocking.getResetAt())
        blocking = w
if blocking != null  return blocking
for w in windows     w.reserve(units)
return null
```
| Check | Pts |
|---|---|
| Rolls/starts every window first | 4 |
| Checks **all** windows **before** reserving in **any** | 8 |
| Uses `used + reserved` (via `hasRoom`) | 4 |
| Picks the blocking window with the **latest reset** | 4 |
| Reserves in every window and returns null on success | 4 |
- **Red flag −8:** one loop that reserves window by window and returns early when one is full (partial reservation leak).

### I3-3: settle · 14 pts
```
for w in windows
    w.rollIfExpired(now)
    w.release(reservedUnits)
    if actualUnits > 0
        w.startIfIdle(now)
        w.charge(actualUnits)
```
- Rolls first (3), releases the **reserved** amount (4), charges the **actual** (4), skips the charge / start when actual is 0 (3).
- **Red flag −4:** charges the estimate instead of the actual.

### I3-4: rollIfExpired · 12 pts
```
if resetAt != null and now >= resetAt
    used = 0
    resetAt = null
    # reserved is NOT reset: it belongs to requests that are still running
```
- Guard on an active window (2), `now >= resetAt` (2), clears used (2), clears resetAt (2), **keeps reserved, with the reason** (4).
- **Red flag −4:** sets `reserved = 0` (a request that settles after the reset drives reserved negative, giving free headroom).

### I3-5: Admission rule · 6 pts
- **Must:** the `+ estimate` rule blocks users who still have usage left, because of a **guess** (3). The requirement says block only when it has run out, so we admit while anything is left (1); we give up a **bounded overshoot**, about one estimate (2).

### I3-6: The lock · 6 pts
- **Must:** `tryReserve` and `settle` run under the lock (2); `backend.generate` must **not** (2), or the user's parallel chats run **one at a time**, breaking Req 5 (2).

### I3-7: Trace (the trap) · 10 pts
- **(a) (3):** all three see 400 left and all run → used **1200**; with 20 chats → **4600** (accept "unbounded").
- **(b) (4):** A admitted (reserved 206), B admitted (600 + 206 = 806 < 1000, reserved 412), C **rejected** (600 + 412 = 1012). End: used 1000, reserved 0.
- **(c) (3):** "check-then-act" (or "race condition", "TOCTOU"), across the slow model call.

---
**Level calibration:** ≥85 senior · 70–84 mid · 50–69 junior. I3-7 (a)+(c) is the senior signal: without naming the check-then-act bug, cap the level at "mid" whatever the score.
