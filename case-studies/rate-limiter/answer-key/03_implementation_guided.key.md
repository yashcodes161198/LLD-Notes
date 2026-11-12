# Answer key 03: Implementation, guided (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Any pseudo-code syntax is fine. Judge **logic, order and errors**, not syntax. Mentally run every body against the trace in I3-7 (Limit(1, 1), calls at t = 0, 0.5, 1.0, 1.5, 2.0), and against two threads calling with tokens = 1.0.

### I3-1: allowRequest · 16 pts
```
if clientId == null || clientId.isEmpty()
    throw Error("invalid client id")
bucket = getOrCreateBucket(clientId, tier)
return bucket.tryConsume(clock.now())
```
| Check | Pts |
|---|---|
| Null/empty id → throw **before** any lookup or state change | 5 |
| Uses the helper to get the bucket | 3 |
| Reads the clock once and passes `now` to the bucket | 3 |
| Returns the bucket's boolean | 3 |
| Over the limit is a **return false**, not an exception | 2 |
- **Red flag −4:** throws an error when the client is over the limit. **−3:** does the token arithmetic here instead of in the bucket.

### I3-2: getOrCreateBucket · 18 pts
```
limit = limits[tier]
return buckets.computeIfAbsent(clientId, id -> new TokenBucket(limit, clock.now()))
# never removing buckets: ~100 bytes each, 1M clients ≈ 100 MB, fine.
# Later: evict buckets idle > capacity / refillPerSecond (they're full again anyway).
```
| Check | Pts |
|---|---|
| Looks up the limit by tier | 3 |
| Get-or-create is **atomic** (computeIfAbsent, putIfAbsent + re-read, or a lock) | 7 |
| New bucket built from the limit and the current time (starts full) | 3 |
| Returns the bucket | 1 |
| Eviction decision comment **with a reason** (memory maths, or the idle-is-full argument) | 4 |
- **Partial (atomic check):** plain `get / if null / put` = 0, or 3 if they flag the race in a comment. A `synchronized` getOrCreateBucket = 5 (correct, but every request for every client queues on it).

### I3-3: tryConsume · 22 pts
```
synchronized tryConsume(now):
    refill(now)
    if tokens >= 1
        tokens = tokens - 1
        return true
    return false
```
| Check | Pts |
|---|---|
| Refills **first** | 4 |
| Checks `tokens >= 1` | 4 |
| Takes one token and returns true | 3 |
| Returns false **without** consuming | 3 |
| Refill + check + consume are **one** critical section | 6 |
| The lock is **per bucket**, not global | 2 |
- **Red flag −6:** the check and the decrement aren't under the same lock (e.g. lock only around the decrement, or no lock). **−3:** checks `tokens > 0` with decimal tokens (admits at 0.3 and goes negative).

### I3-4: refill · 22 pts
Two answers are possible. **A** (floor + `lastRefill = now`) earns at most 10. **B** (keeps the fraction) earns up to 22.

**B, best:**
```
elapsed = now - lastRefill
if elapsed <= 0  return
tokens = min(limit.capacity, tokens + elapsed * limit.refillPerSecond)   # decimal: keeps the fraction
lastRefill = now
```
| Check | Pts |
|---|---|
| Computes elapsed from lastRefill | 3 |
| Guards elapsed ≤ 0 | 3 |
| Adds elapsed × rate | 3 |
| Caps at capacity with min | 4 |
| Updates lastRefill | 2 |
| Keeps the fraction (decimal tokens, **or** int tokens with `lastRefill += tokensToAdd / rate` so leftover time carries), with a comment saying why | 7 |
- **A (max 10):** `floor(elapsed * rate)` and `lastRefill = now` every call. Score the first five checks, then cap at 10.
- **Red flag −4:** no cap (an idle client banks unlimited tokens).

### I3-5: Two assumptions · 6 pts
- **Time (3):** the clock is **monotonic**; a wall clock can jump (NTP) and add or remove tokens. Bonus wording: inject it so tests are deterministic.
- **Map (3):** computeIfAbsent (or the chosen get-or-create) is **atomic per key**; that's true of Java's ConcurrentHashMap, and in pseudo-code they assume it and say so.

### I3-6: The race · 6 pts
- **Interleaving (3):** A reads tokens = 1, B reads tokens = 1, both pass `>= 1`, both subtract → two admitted, tokens = −1.
- **Fix (3):** one lock per bucket around refill + check + consume (2); not global, because a global lock makes every client wait on every other client (1).

### I3-7: Trace · 10 pts
**(a) Correct version (3):**
| t | tokens after refill | result | tokens after | lastRefill |
|---|---|---|---|---|
| 0 | 1.0 (new) | allow | 0.0 | 0 |
| 0.5 | 0.5 | deny | 0.5 | 0.5 |
| 1.0 | 1.0 | allow | 0.0 | 1.0 |
| 1.5 | 0.5 | deny | 0.5 | 1.5 |
| 2.0 | 1.0 | allow | 0.0 | 2.0 |

3 of 5 allowed: about 1 per second, as agreed.

**(b) floor version (3):** t=0 allow (0 tokens). Every later call: elapsed 0.5 → floor = 0, lastRefill reset to now → **deny, forever**.

**Must (4):** names the bug: the floor drops the fraction **and** resetting lastRefill drops the time it came from, so a client calling more often than once per token-interval is **starved**: it never gets a token back, although it's within its rate. Fix: keep the fraction.
- If they wrote version B and explain why (b) starves while theirs doesn't, give full points.

---
**Level calibration:** ≥85 senior · 70–84 mid · 50–69 junior. I3-7's starvation spot is the senior signal: without it, cap the level at "mid" whatever the score.
