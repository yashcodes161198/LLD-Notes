# Answer key 02: Entities + class design (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Names don't have to match exactly; responsibilities do.

## Part A: Entities (35 pts)

### E1: Noun list · 5 pts
- **Must:** at least 6 of: gateway/API, client, request, tier, limit (capacity/rate), token, bucket, rate limiter/system, thread, time/clock.
- 1 pt per noun up to 5.

### E2: Keep/drop decisions · 15 pts
| Noun | Expected | Reason that must appear | Pts |
|---|---|---|---|
| Gateway/API | DROP | external caller | 1 |
| Client | DROP | no profile stored; its id is just a key, its tier is owned by the account service | 2 |
| Request | DROP | owned by the gateway; we only need who sent it | 4 |
| Token | DROP | just a number inside the bucket; no identity or rules | 3 |
| RateLimiter | KEEP | orchestrator / entry point | 2 |
| TokenBucket | KEEP | per-client state with its own rules (refill, cap, atomic consume) | 2 |
| Limit | KEEP | capacity + rate per tier (a value object; fields on the Tier enum also accepted) | 1 |
- **Red flag −6:** keeps Request as a class *without* saying why (url/headers are never used). With an explicit, defensible reason, −2 only.
- **Red flag −4:** keeps Client/User or Token as classes.
- Thread / Clock: dropping them, or naming Clock as an injected dependency (not an entity), are both fine. No points either way.

### E3: Dropped nouns whose data survives · 6 pts
- **Must:** Request → only the **client id** survives (3); Client → its **tier**, owned by another system (3). Both become **parameters** of `allowRequest(clientId, tier)`.

### E4: The "just a number" entity · 6 pts
- **Must:** TokenBucket is more than an int in a map because it needs **the last refill time** to refill over time (2), a **cap** at capacity (2), and **one atomic consume / one lock per client** (2).

### E5: Enums · 3 pts
- **Must:** `Tier {FREE, PREMIUM}`.

## Part B: Class design (40 pts)

### C1: RateLimiter (orchestrator) · 18 pts
| Member | Pts | Accept |
|---|---|---|
| `- limits: Map<Tier, Limit>` | 3 | limits as fields on the Tier enum |
| `- buckets: Map<string, TokenBucket>` | 4 | must be per client id; −1 if not noted as concurrent/thread-safe |
| `+ allowRequest(clientId, tier) -> boolean \| error` | 5 | any name (isAllowed, tryAcquire); must **return boolean**, take the id (−2 if it takes a Request object) |
| `- getOrCreateBucket(clientId, tier)` (private) | 3 | inlined get-or-create noted in a comment |
| `- clock` (injected), or `now` passed in | 3 | reading the system time directly gets 1 |

### C2: TokenBucket · 12 pts
- `limit` (or capacity + refillPerSecond) (3), `tokens: number` (2), `lastRefill: timestamp` (2), `tryConsume(now) -> boolean` (3), private `refill(now)` (2).
- **Red flag −3:** the refill arithmetic lives on RateLimiter instead of on the bucket (breaks Information Expert).

### C3: Limit · 6 pts
- `capacity` (2), `refillPerSecond` (2), immutable and/or validated (> 0) on construction (2).

### C4: Top-down and traceability · 4 pts
- RateLimiter written first (2), and members annotated with the requirement they come from (2).

## Part C: Design decisions (25 pts)

### D1: Which collection, and why · 5 pts
- **Must:** a (concurrent) map from client id to bucket (2). Here **O(1) really matters**: millions of clients, and the lookup is on every request's hot path (2). Contrast with Amazon Locker, where the map was for clarity at n ≈ 50 (1).

### D2: Where tokens and lastRefill live · 7 pts
- **Must:** on **TokenBucket** (3); principle **Information Expert** / encapsulation (2); concurrency benefit: the two fields change together, so **one object = one lock** around them (2).

### D3: Rejected refill alternative · 5 pts
- **Must:** a background thread/timer that adds tokens to every bucket each tick → O(all clients) per tick, wasted on idle clients, drift. Lazy refill on request is O(1) and exact.
- **Accept also (full points):** two parallel maps (tokensByClient, lastRefillByClient) → they drift and can't be locked together.

### D4: A wrong algorithm · 5 pts
- **Must:** fixed-window counter (2). It breaks at the window **boundary**: a full limit just before the reset plus a full limit just after = **2× the burst** in a moment (3).

### D5: Methods not implemented · 3 pts
- **Must:** Limit's constructor/getters, the Tier enum, and the clock (a dependency).

---
**Level calibration:** ≥85 senior · 70–84 mid · 50–69 junior · <50 redo Step 2–3 of `../rate_limiter.py`.
