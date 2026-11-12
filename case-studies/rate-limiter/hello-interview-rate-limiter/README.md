# Hello Interview Rate Limiter

Runnable Java code for the design in [`../rate_limiter.py`](../rate_limiter.py) (the Hello Interview delivery framework version).
It's separate from `../phases/`, which follows the video's design. This one follows the leaner interview design: one `TokenBucket` object per client, locked with `synchronized`.

Run either phase with `./run.sh` (JDK 17+). `Main` is a set of self-checks: it prints each step and throws if anything is wrong.

## Phase 1: Core Requirements

Requirements 1-5 from Step 1 of the design, using the best versions from Step 4.

| Class | Role |
|---|---|
| `RateLimiter` | Orchestrator. `allowRequest(clientId, tier) -> boolean`. Validates config at startup, gets or creates each client's bucket with `computeIfAbsent`. |
| `TokenBucket` | One client's `tokens` + `lastRefillNanos`. `synchronized tryConsume` = refill + check + take as one step, locked per bucket. |
| `Limit` | Immutable value class: `capacity`, `refillPerSecond`. |
| `Tier` | `FREE`, `PREMIUM`. |
| `Clock`, `SystemClock`, `ManualClock` | Injected monotonic time (`System.nanoTime`), and a test clock that only moves when told to. |

Checks in `Main`: the verification trace from the design, the "poller isn't starved" case (TRICKY #3), config validation at startup, and 500 races of 20 threads on one client (exactly 10 allowed every time).

## Phase 2: Extensions

The three extensions from Step 5, each landing in one place:

| Extension | Pattern | What changed |
|---|---|---|
| 1. A second algorithm | Strategy | New `ClientLimiter` interface; `TokenBucket` and new `SlidingWindowLog` implement it. `Limit` gains an `Algorithm`. The `createLimiter` factory in `RateLimiter` is the only place that knows there's more than one. |
| 2. Per-endpoint limits | Composite key | `allowRequest(clientId, tier, endpoint)`. Limiters are keyed by `(clientId, endpoint)`; limits are looked up by `(endpoint, tier)`, falling back to the tier default. Endpoints without an override share one default budget per client. |
| 3. Retry-After | Result object | `tryConsume` returns a `Decision(allowed, retryAfterSeconds)`, computed under the same lock as the decision. |

Not coded (answered in words in the design's "More follow-ups"): tier upgrades while a bucket is alive, multi-server limits (Redis), idle-bucket eviction, IP + key double limiting.
