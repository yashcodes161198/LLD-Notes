# Answer key 05: Extensibility (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Each question is scored on **Pattern / Lands in / Say / Code**. "Describe only" for Code is acceptable for X4–X6 (their points sit in Pattern / Lands in / Say). For X1–X3 a code delta is expected.

General red flag for any question, **−5:** the answer rewrites the design or touches many classes when one place would do.

---

### X1: New channel (WhatsApp) · 18 pts
- **Pattern (4):** **Open/Closed** through the handler map: each channel is a plug-in behind `ChannelHandler`.
- **Lands in (5):** the `Channel` enum + one handler registered at construction. `route`, `resolveChannels` and `dispatch` are **unchanged**.
- **Say (4):** URGENT picks the new channel up automatically via `Channel.values()`; a forgotten handler fails at **startup** (constructor check).
- **Code (5):**
```
enum Channel: EMAIL, SMS, PUSH, WHATSAPP
router = NotificationRouter({EMAIL: e, SMS: s, PUSH: p, WHATSAPP: w})
```
- **Bonus +2 (cap 18):** asks whether URGENT should really include a channel that costs per message.
- **Red flag −4:** adds an `if channel == WHATSAPP` branch anywhere in the router.

### X2: Multiple / per-type preferences · 18 pts
- **Pattern (4):** the map value goes from one `Channel` to a `Set<Channel>`, and is **promoted to a class** (`UserPreference`) once it carries rules (per type, defaults).
- **Lands in (5):** the `preferences` value type + the **NORMAL branch of `resolveChannels`**. `dispatch` is unchanged because it already takes a set.
- **Say (4):** names that returning a **set** from resolveChannels is what made this cheap.
- **Code (5):**
```
class UserPreference:
    - channelsByType: Map<NotificationType, Set<Channel>>
    - defaultChannels: Set<Channel>
    + channelsFor(type) -> Set<Channel>
# resolveChannels NORMAL: pref = preferences[userId]; empty/null -> throw; return pref.channelsFor(n.getType())
```
- **Bonus +2 (cap 18):** raises **opt-out** (marketing: none) and whether URGENT may override it.

### X3: Fallback + retries · 22 pts
- **Pattern (5):** an **ordered fallback list**: try preferred, then the others in an explicit order, **stop at the first success**.
- **Lands in (4):** `dispatch`'s NORMAL path (or a `sendWithFallback` helper). `resolveChannels` and URGENT unchanged.
- **Say (4):** fallback is a **product decision** (a user who chose SMS may not want email), so ask first; retries are per channel, driven by the FAILED entries in the result.
- **Code (5):**
```
ordered = [preferred] + FALLBACK_ORDER.without(preferred)
for channel in ordered
    try   handlers[channel].send(n); results[channel] = SENT; return results
    catch results[channel] = FAILED
return results
```
- **Must mention (4):** an **idempotency key** (`notificationId + channel`) so a retry after a timeout can't double-send; a timeout isn't proof the message wasn't sent.

### X4: HIGH priority + quiet hours · 14 pts
- **Pattern (5):** **Strategy**: `Map<Priority, RoutingPolicy>`, each policy returns a `Set<Channel>`.
- **Lands in (4):** `resolveChannels` only; it becomes a lookup of the policy.
- **Say (5):** with two priorities an `if` is clearer, so Strategy wasn't in the base design (**KISS / YAGNI**); it pays off at the third priority or once rules like quiet hours arrive.
- **Red flag −4:** adds HIGH as another `if/else` inside `dispatch`, mixing deciding and sending.

### X5: URGENT abuse · 14 pts
- **Pattern (5):** treat URGENT as a **privilege**: an **allow-list** of caller services that may send it, plus a **per-user rate cap** on URGENT.
- **Lands in (4):** a check at the **top of `route`** (needs the caller's identity, e.g. a `callerId` on the notification or the call); nothing else changes.
- **Say (5):** both parts, the authorisation and the cap; a rejected URGENT is either an error or downgraded to NORMAL, and that's a product choice they'd confirm.

### X6: Millions per minute, slow SMS provider · 14 pts
- **Pattern (5):** **async per-channel queues**: `dispatch` publishes `(notification, channel)` instead of calling `send`; handlers become **consumers** with their own retries and backoff.
- **Lands in (4):** `dispatch` only; the result status becomes **ENQUEUED** instead of SENT. `resolveChannels` is unchanged.
- **Say (5):** a slow SMS provider no longer blocks EMAIL/PUSH; consumers **dedupe on (notificationId, channel)**; `preferences` becomes a **concurrent map** (or a cache in front of the preference service).
- **Red flag −3:** "use Kafka" with no word on what's queued, or how duplicates are prevented.

---
**Level calibration:** ≥80 senior/staff · 60–79 senior-ish mid · 40–59 mid · <40 junior (juniors often never reach this step, so don't be discouraged).
