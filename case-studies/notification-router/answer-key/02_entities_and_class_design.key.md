# Answer key 02: Entities + class design (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Names don't have to match exactly; responsibilities do.

## Part A: Entities (35 pts)

### E1: Noun list · 5 pts
- **Must:** at least 6 of: user, preferred channel/preference, channel, notification, priority, router, handler/sender, calling service, result/status, content.
- 1 pt per noun up to 5.

### E2: Keep/drop decisions · 15 pts
| Noun | Expected | Reason that must appear | Pts |
|---|---|---|---|
| User | DROP | the user service owns the profile; we only need userId → channel | 4 |
| Calling service / e-commerce site | DROP | external actor / context; it just calls route() | 2 |
| Preference | DROP as a class | one value per user; a map entry is enough (until it grows rules) | 2 |
| NotificationRouter | KEEP | orchestrator / entry point | 2 |
| Notification | KEEP | the unit routed and reported on; carries priority and id | 2 |
| ChannelHandler | KEEP (interface) | the seam the router calls; implementations out of scope | 2 |
| Channel / Priority / status | ENUM | fixed small sets of values | 1 |
- **Red flag −6:** keeps User as a class *without* a reason. With an explicit, defensible reason (e.g. "it'll hold multi-channel prefs later"), −2 only.
- **Red flag −4:** concrete EmailSender/SmsSender/PushSender classes with bodies (that's the out-of-scope part).

### E3: Dropped noun whose fact survives · 6 pts
- **Must:** User → its **preferred channel** (3) → lives as **`Map<userId, Channel>` on the router** (3).

### E4: The handler-shaped thing that's in scope · 6 pts
- **Must:** a **`ChannelHandler` interface** (2). The **implementations** are out of scope, but the **contract/seam** is in scope because the router must call something (2). One per channel, **looked up by Channel** (2).

### E5: Enums · 3 pts
- **Must:** `Channel {EMAIL, SMS, PUSH}` (1), `Priority {NORMAL, URGENT}` (1), `DeliveryStatus {SENT, FAILED}` or an equivalent result type (1).

## Part B: Class design (40 pts)

### C1: NotificationRouter (orchestrator) · 18 pts
| Member | Pts | Accept |
|---|---|---|
| `- preferences: Map<string, Channel>` | 4 | any map from user id to channel |
| `- handlers: Map<Channel, ChannelHandler>` | 4 | a list of handlers that each say which channel they serve gets 2 |
| `+ setPreference(userId, channel)` | 3 | any name like updatePreference |
| `+ route(notification) -> Map<Channel, DeliveryStatus>` | 5 | must return **per-channel results** (−3 if void, or a single boolean) |
| constructor taking handlers, **or** private `resolveChannels` / `dispatch` | 2 | either one shows the split or the fail-fast check |

### C2: Notification · 10 pts
- `id` (2), `userId` (2), `priority: Priority` (2), `content` (1), `isUrgent()` (2), stated as **immutable / value object** (1).
- **Red flag −3:** Notification holds the channel list or calls the handlers itself (mixes routing into the data).

### C3: ChannelHandler · 8 pts
- Declared as an **interface/abstract**, not a concrete class (3); `send(notification)` that can fail (3); one per channel, **keyed by Channel** in the router (2).

### C4: Top-down and traceability · 4 pts
- Router written first (2), and members annotated with the requirement they come from (2).

## Part C: Design decisions (25 pts)

### D1: Where the preference lives · 6 pts
- **Must:** in a **map on the router** (2), because it's a **relationship** (user → channel) and the User profile is **owned by another system** (2); rule: **intrinsic state on the entity, relationships in the orchestrator** (2).

### D2: Decide vs send · 6 pts
- **Must:** two of these (3 each): the decision is **pure and testable** without handlers; **routing-rule changes land in one method**; the target set is **complete before anything is sent**, so validation errors never leave a half-sent notification; dispatch can isolate failures per channel.

### D3: Return type of the decide step · 5 pts
- **Must:** a **Set** (2), because URGENT includes the preferred channel and **R4 says exactly once each** (3). A List with an explicit "no duplicates" note gets 3.

### D4: A rejected way to call senders · 5 pts
- **Must:** one of: a `switch(channel)` calling vendors directly from route (router knows every vendor; every new channel edits it), or one `NotificationSender` with `sendEmail/sendSms/sendPush` (5). Or: "send to preferred, then loop all if URGENT", because it double-sends (5).

### D5: Methods not implemented · 3 pts
- **Must:** Notification getters / `isUrgent` (one comparison) and the handler implementations (out of scope).

---
**Level calibration:** ≥85 senior · 70–84 mid · 50–69 junior · <50 redo Step 2–3 of `../notification_router.py`.
