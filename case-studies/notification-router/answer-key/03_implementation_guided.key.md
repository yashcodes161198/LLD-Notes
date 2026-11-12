# Answer key 03: Implementation, guided (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Any pseudo-code syntax is fine. Judge **logic, order and errors**, not syntax. Mentally run every body against the trace: handlers `{EMAIL: e, SMS: s, PUSH: p}`, `s.send` throws, u1 prefers SMS, u9 has no preference.

### I3-1: setPreference · 12 pts
```
if userId.isEmpty()   throw Error("invalid user")
if channel == null    throw Error("invalid channel")
preferences[userId] = channel
```
| Check | Pts |
|---|---|
| Empty-user check → throw | 3 |
| Null-channel check → throw, **different message** from the user one | 3 |
| Both checks **before** the write | 2 |
| Writes / overwrites the map entry | 3 |
| No "already has a preference" error (R1: newest wins) | 1 |

### I3-2: route · 18 pts
```
if notification == null || notification.getId().isEmpty()
        || notification.getUserId().isEmpty() || notification.getPriority() == null
    throw Error("invalid notification")
channels = resolveChannels(notification)
return dispatch(notification, channels)
```
| Check | Pts |
|---|---|
| Null-notification check | 3 |
| Checks id, userId and priority (1 pt for one or two of them, 4 for all three) | 4 |
| All validation **before** resolve/dispatch | 4 |
| Calls resolveChannels | 3 |
| Calls dispatch and **returns its result map** | 4 |
- **Red flag −4:** wraps resolveChannels in a try/catch that swallows "no preferred channel" (R3 says that's an error to the caller).

### I3-3: resolveChannels · 20 pts
```
if notification.isUrgent()
    return Set(Channel.values())
preferred = preferences[notification.getUserId()]
if preferred == null
    throw Error("no preferred channel for user")
return Set(preferred)
```
| Check | Pts |
|---|---|
| URGENT handled **before** the preference lookup (so no preference can't block it) | 5 |
| URGENT returns **every** channel (enum values or handler keys) | 4 |
| NORMAL looks up the preference | 3 |
| Missing preference → **specific** throw | 5 |
| Returns a set/collection of one | 3 |
- **Red flag −5:** URGENT returns `{preferred} + all` built as a list (duplicate), or looks up the preference first and throws for an URGENT with no preference.

### I3-4: dispatch · 22 pts
```
results = Map<Channel, DeliveryStatus>
for channel in channels
    try
        handlers[channel].send(notification)
        results[channel] = SENT
    catch error
        results[channel] = FAILED
return results
```
| Check | Pts |
|---|---|
| Loops over the given channels | 3 |
| Looks up the handler by channel | 3 |
| Calls send | 2 |
| Records SENT **only after** send returns | 3 |
| try/catch **inside** the loop, per channel | 6 |
| Records FAILED and **continues** | 3 |
| Returns the results | 2 |
- **Red flag −6:** try/catch around the whole loop (the first failure still stops the rest). **−3:** rethrows after recording FAILED.

### I3-5: Assumptions · 6 pts
- **Must (3 each, any two):** a map miss returns **null** in my pseudo-code (language-dependent; Python raises KeyError); a handler signals failure by **throwing** (not by returning false); `send` is **synchronous**; handlers are thread-safe / don't block forever (mention a timeout). Each should be "said out loud to the interviewer".

### I3-6: All handlers fail · 6 pts
- **Must:** **return** the map with every channel FAILED, don't throw (3). Reason: the caller learns **which** channels failed and can **retry per channel**; one result shape to handle (3).
- **Accept (4 max):** throw, if the error **carries the per-channel results** and they give a reason.

### I3-7: Trace of the teammate's version · 16 pts
- **(a) 5:** URGENT, SMS working → **SMS receives it twice** (preferred, then again in the loop); EMAIL and PUSH once. Breaks R4 "exactly once".
- **(b) 5:** SMS down → `handlers[SMS].send` throws on the first line; the exception escapes; **EMAIL and PUSH never get it**; the caller can't tell what went out, and retrying the whole route would re-send anything that had succeeded. Breaks R5.
- **(c) 3:** u9 has no preference → `handlers[null]` crashes **before** the fan-out, so an URGENT is blocked by a missing preference. Breaks R4.
- **Fix (3):** decide first as a **set** (URGENT check first), then dispatch with a **per-channel try/catch** and per-channel results.
- If they only say "looks fine", 0 for the item.

---
**Level calibration:** ≥85 senior · 70–84 mid · 50–69 junior. I3-7 (a) and (b) are the senior signal: without both, cap the level at "mid" whatever the score.
