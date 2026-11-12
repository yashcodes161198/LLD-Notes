# LLM usage limits: Java code

Runnable Java for the design in [`../llm_usage_limits.py`](../llm_usage_limits.py). Run either phase with `./run.sh` (JDK 17+). `Main` is a set of self-checks: it prints each step and throws if anything is wrong.

```
Phase-N/src/llmusagelimits/
  model/      Model, LimitType, InferenceRequest, InferenceResult, UsageWindow, UsageAccount (+ WindowSpec in Phase 2)
  backend/    InferenceBackend (the seam) + ModelServerBackend: the model call is stubbed with a println
  limiter/    UsageLimiter (orchestrator), UsageLimitExceededException (+ TooManyConcurrentRequestsException in Phase 2)
  plan/       Plan                                                   (Phase 2)
  estimate/   UsageEstimator + FixedEstimator, MovingAverageEstimator (Phase 2)
  Main.java   self-checks
```

## Phase 1: Core Requirements

Requirements 1-6 from Step 1, using the best version of `handle()` from Step 4 (reserve then settle, not the TRICKY #1 check-then-act trap).

| Class | Role |
|---|---|
| `UsageLimiter` | `handle(request)`: estimate → `tryReserve` (atomic) → backend call outside any lock → `settle` in a `finally`. `getRemaining(userId, type)` never creates an account. Fails at construction on a missing backend/clock or a non-positive limit. |
| `UsageAccount` | One user's `FIVE_HOUR` + `WEEKLY` windows and the lock unit. `tryReserve` checks every window before reserving in any, and returns the blocking window with the latest reset. |
| `UsageWindow` | `used`, `reserved`, `resetAt`. Starts at first use; `rollIfExpired` clears `used` but keeps `reserved` (in-flight requests survive a reset). |
| `InferenceRequest` / `InferenceResult` | Immutable value objects that validate themselves. |
| `InferenceBackend` | The seam. `ModelServerBackend` stubs the model call (prints, returns token counts). `Main` uses scripted and gated test doubles for the checks. |
| `UsageLimitExceededException` | The specific error: which limit, and when it resets. |

Checks in `Main`: the verification trace (overshoot allowed, 5h then weekly limits, both full → the later reset), **the trap does not happen** (a chat started inside another chat's model call sees its reservation; 20 real threads released together → exactly 5 run, 15 rejected, used exactly 1000), 10 backend failures leave no phantom reservation, a reset during a request keeps the reservation, input validation, and bad config at startup.

## Phase 2: Extensions

Each extension lands in one place. `handle()` only gained the in-flight enter/leave and the estimator's `record` call.

| Extension | Pattern | What changed |
|---|---|---|
| 1. Plans + upgrade mid-window | Config becomes a lookup | `Plan {FREE x1, PRO x5, MAX x20}`, `setPlan(userId, plan)` → `UsageAccount.applyPlan` → `UsageWindow.applyMultiplier`. Used is kept, so toggling plans isn't a refill; a downgrade just blocks until reset. |
| 2. Better estimates | Strategy | `UsageEstimator` with `FixedEstimator` (Phase 1 behaviour) and `MovingAverageEstimator` (per user + model, α = 0.2, seeded at 100). Fed from `handle()`'s success path. |
| 3. OPUS-only weekly limit | Window scope | `WindowSpec(type, duration, baseLimit, models)`, `LimitType.OPUS_WEEKLY`, `UsageWindow.appliesTo(model)`. `tryReserve` and `settle` filter windows with the same model. |
| 4. Hundreds of parallel chats | Per-user concurrency cap | `UsageAccount.tryEnter/leave`, `maxInFlight`, `TooManyConcurrentRequestsException`. `leave` is in a `finally`, so limit rejections don't leak slots. |

The Phase 1 constructor `UsageLimiter(backend, clock, fiveHourLimit, weeklyLimit)` still works and maps to the default windows, a fixed estimate and no cap. Bad config fails at startup: no windows, a duplicate window type, no estimator, `maxInFlight <= 0`, a window with no models.

Not coded (answered in words in the design's Step 5 and "More follow-ups"): multiple gateway servers (shared store + atomic script + reservation TTL), streaming with incremental settle, idempotent retries, prompt-cache discounts, admin credits.
