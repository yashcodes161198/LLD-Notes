# Answer key 05: Extensibility (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Each question is scored on **Pattern / Lands in / Say / Code**. "Describe only" for Code is acceptable for X4–X6 (they carry no code points). For X1–X3 a code delta is expected.

General red flag for any question, **−5:** the answer rewrites the design or touches many classes when one place would do.

---

### X1: A second algorithm · 25 pts
- **Pattern (6):** **Strategy**: an interface with `tryConsume(now)`; TokenBucket and the new algorithm implement it. A small factory picks one from the Limit.
- **Lands in (6):** a new interface + a new class, plus the one `new TokenBucket(...)` line in getOrCreateBucket. `allowRequest` is unchanged (it only ever called tryConsume).
- **Say (5):** names the "only one creation line knows there are several algorithms" point. Bonus wording: "I didn't build the interface up front because one algorithm didn't need it."
- **Code (8):**
```
interface ClientLimiter:  + tryConsume(now) -> boolean
class SlidingWindowLog implements ClientLimiter:
    - limit, windowSeconds, timestamps: Queue<timestamp>
    synchronized tryConsume(now):
        while timestamps not empty && timestamps.peek() <= now - windowSeconds
            timestamps.poll()
        if timestamps.size() < limit.capacity
            timestamps.add(now); return true
        return false
createLimiter(limit, now): SLIDING_WINDOW_LOG -> new SlidingWindowLog(...), else new TokenBucket(...)
```
- **Bonus +2 (cap 25):** notes the sliding log costs memory per request (up to capacity timestamps per client) vs O(1) for the bucket.
- **Red flag −2:** makes RateLimiter itself abstract, with one subclass per algorithm, each holding its own map of all clients. It works (it's the design in this folder's `phases/`), but the orchestrator becomes the strategy, so the map, the id check and the get-or-create get repeated in every subclass, and the change is no longer in one place.

### X2: Per-endpoint limits · 20 pts
- **Pattern (5):** **composite key**: bucket key = (client, endpoint); limit lookup = (tier, endpoint) with a fallback to the tier default.
- **Lands in (5):** allowRequest gains an `endpoint` parameter; getOrCreateBucket's key and limit lookup change. TokenBucket unchanged.
- **Say (4):** "a bucket doesn't care what it's counting."
- **Code (6):** `limit = endpointLimits[(tier, endpoint)] ?? limits[tier]`; `key = clientId + "|" + endpoint`; computeIfAbsent on that key.
- **Bonus +2 (cap 20):** asks whether endpoints without an override share one default budget or get one each.

### X3: Retry-After · 15 pts
- **Pattern (4):** return a **result object** (allowed + retryAfterSeconds) instead of a boolean.
- **Lands in (4):** TokenBucket.tryConsume's return value, passed through allowRequest.
- **Say (3):** retry time = (1 − tokens) / refillPerSecond, computed **inside the same lock** as the decision.
- **Code (4):** `return Decision(false, (1 - tokens) / limit.refillPerSecond)` on the deny path, `Decision(true, 0)` on allow.
- **Red flag −3:** a separate `getRetryAfter()` called after the boolean (it races with other threads).

### X4: Key rotation / no key · 12 pts
- **Pattern (4):** **layered limits**: also limit by IP (or another key that can't be rotated cheaply), and deny if any layer denies.
- **Lands in (4):** the gateway calls the same `allowRequest` twice, with namespaced ids (`"ip:" + ip`, `"key:" + apiKey`). RateLimiter is **unchanged**, because the id is just a string.
- **Say (4):** names both layers, and that an IP token is spent even if the key check then fails (acceptable), or orders the checks on purpose.

### X5: Tier upgrade with a live bucket · 13 pts
- **Pattern (5):** a **stale reference**: the bucket still holds the FREE Limit. Detect it and rebuild: compare `bucket.limit` with `limits[tier]`.
- **Lands in (4):** getOrCreateBucket only (`compute` instead of `computeIfAbsent`, replacing the bucket on a mismatch).
- **Say (4):** what happens today (stuck on FREE until the bucket is evicted, possibly forever), the fix, and what the new bucket starts with (full is fine: an upgrade gift).

### X6: 50 API servers · 15 pts
- **Pattern (5):** shared state: move (tokens, lastRefill) per key into a central store (e.g. Redis) and run refill + consume as **one atomic server-side operation** (script / transaction). Accept also: an approximate local limit of limit / N per server, with its weakness named (uneven load balancing).
- **Lands in (4):** TokenBucket's storage; the RateLimiter API is unchanged.
- **Say (6):** today a client gets 50× its limit (2); why the operation must be atomic in the store, otherwise the check-then-act race comes back over the network (2); **fail open vs fail closed** when the store is down, with a choice (2).
- **Red flag −3:** "just use Redis" with no atomicity.

---
**Level calibration:** ≥80 senior/staff · 60–79 senior-ish mid · 40–59 mid · <40 junior (juniors often never reach this step, so don't be discouraged).
