# Answer key 02: Entities + class design (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Names don't have to match exactly; responsibilities do.

## Part A: Entities (35 pts)

### E1: Noun list · 5 pts
- **Must:** at least 8 of: user, request, model, message/contents, usage, limit, window (5-hour / weekly), estimate/reservation, backend/inference, result/response, chat, token, system/limiter.
- 1 pt per two nouns, up to 5 (8+ nouns = 5).

### E2: Keep/drop decisions · 14 pts
| Noun | Expected | Reason that must appear | Pts |
|---|---|---|---|
| User | DROP | external identity; only the **id is a key** | 2 |
| Message/contents | DROP | owned by the chat service; only its **size** matters, stays inside the request | 2 |
| Reservation | DROP (as a class) | a number held for one call: a local + a `reserved` counter | 2 |
| Request | KEEP | immutable value object, validates itself | 2 |
| Window / limit | KEEP | own counters + reset rule | 2 |
| User's usage / account | KEEP | groups one user's windows; the lock unit | 2 |
| Limiter | KEEP | orchestrator / entry point | 1 |
| Backend + result | SEAM | external system; interface only | 1 |
- **Accept:** keeping `Reservation` as a class if they say why (e.g. it carries the model/weight for settle). No deduction.
- **Red flag −4:** keeps User as a class with fields (name, email, plan). **−4:** keeps Chat/Conversation as a class (see E3).

### E3: Chat · 6 pts
- **Must:** **not a class** (2). We don't need to know which chat a request came from (2); "parallel chats" just means **concurrent requests from one user** (2).

### E4: The "just a number" entity · 6 pts
- **Must:** the **limit/window** (2). It has a **duration and a start/reset time** (2) and its **own counters and roll-over rule** (2).

### E5: Enums · 4 pts
- `Model {HAIKU, SONNET, OPUS}` with a **weight** (2); `LimitType {FIVE_HOUR, WEEKLY}` (2).

## Part B: Class design (40 pts)

### C1: UsageLimiter (orchestrator) · 13 pts
| Member | Pts | Accept |
|---|---|---|
| `- accounts: Map<userId, UsageAccount>` | 3 | any map keyed by user |
| `- backend: InferenceBackend` | 2 | |
| `- clock` (or `now` passed in) | 2 | |
| `+ handle(request) -> result \| error` | 4 | any name (process/infer/submit); must **return the result** |
| `+ getRemaining(userId, type) -> long` | 2 | any read of remaining usage |

### C2: UsageAccount · 10 pts
- `windows: List<UsageWindow>` or two named fields (2).
- `tryReserve(units, now)` as **one atomic check + reserve** (4).
- `settle(reserved, actual, now)` (3).
- `remaining(type, now)` (1).
- **Red flag −4:** separate `canAdmit()` and `reserve()` public methods called one after another by the limiter (check-then-act across two calls).

### C3: UsageWindow · 12 pts
- `limit`, `duration` (2); `used` (2); `reserved` (2); `resetAt` (nullable / "no active window") (2).
- `rollIfExpired(now)` / `startIfIdle(now)` or one equivalent `refresh(now)` (2); `hasRoom()` / `remaining()` (2).
- **Red flag −3:** the account or limiter compares timestamps itself instead of asking the window (breaks Information Expert).

### C4: InferenceRequest + traceability · 5 pts
- `userId`, `model`, `messages`; immutable, validates itself (2).
- Orchestrator written first and members annotated with requirements (3).

## Part C: Design decisions (25 pts)

### D1: The lock · 7 pts
- **Must:** **per user** (on the account) (2); covers **check + reserve** and **settle** (2); must **NOT** cover the **model call**, or the user's parallel chats run one at a time (3).
- One global lock: 2 pts max ("works, but every user waits for every other").

### D2: Who owns reset · 5 pts
- **Must:** the **window** (2); Information Expert: it owns resetAt/used, so it applies its own rule (3).

### D3: Rejected alternative · 5 pts
- **Must:** a per-request **log summed over the last 5h** (grows with traffic, re-sums each time), **or** a sliding window (fair, but not the agreed "starts at first use" rule). Either with a reason = 5. Name without reason = 2.

### D4: Injected clock · 4 pts
- **Must:** every rule depends on time; tests must move time (e.g. jump 5 hours) without waiting (4).

### D5: Fields vs list · 4 pts
- Either answer with a reason. List: "every rule is 'for each window', and a third window is config". Fields: "exactly two, and named fields read more clearly". 4 pts for either with a reason, 1 without.

---
**Level calibration:** ≥85 senior · 70–84 mid · 50–69 junior · <50 redo Step 2–3 of `../llm_usage_limits.py`.
