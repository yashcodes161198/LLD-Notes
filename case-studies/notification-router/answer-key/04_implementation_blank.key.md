# Answer key 04: Implementation, no hints (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. The **reference bodies are the same as key 03** (I3-1 to I3-4); use them for the code checks. This key adds points for the student writing their own **Core logic / Edge cases** docstrings, for the constructor, and for planning before coding.

### I4-1: constructor · 12 pts
- **Docstring (4):** core logic "check every channel has a handler → store" (2); edge case "missing handler → throw" (2).
- **Body (8):** loops over **all Channel values** (not just the keys passed in) (3); throws a **specific** error for a missing one (3); stores the handlers and initialises an empty preferences map (2).
```
for channel in Channel.values()
    if handlers[channel] == null  throw Error("no handler for channel " + channel)
this.handlers = handlers
this.preferences = {}
```

### I4-2: setPreference · 10 pts
- **Docstring (3):** validate → store/overwrite (1); edge cases empty user, null channel, overwrite (2).
- **Body (7):** the I3-1 table, scaled to 7 (multiply by 7/12 and round).

### I4-3: route · 22 pts
- **Docstring (8):** core logic validate → resolve channels → dispatch → return results (4); edge cases invalid notification, NORMAL with no preference, a handler failing (4).
- **Body (14):** the I3-2 table, scaled to 14.
- **−3** if the body doesn't follow the order of the docstring.

### I4-4: Helper(s) · 28 pts
- **resolveChannels (13):** docstring with the URGENT-first rule and the no-preference error (3); body per the I3-3 table scaled to 10.
- **dispatch (15):** docstring with "a failure is recorded and the loop continues" (3); body per the I3-4 table scaled to 12.
- **Inlined in route, no penalty for logic:** score the same checks against route's body. Deduct 2 for readability (the helpers should be named after core-logic steps). Docstring points then come from route's docstring if it covers these steps.

### I4-5: Product trade-off · 8 pts
- **Must:** an URGENT message (fraud alert, payment failed) shouldn't be **blocked by a missing preference** (4); so the URGENT check comes **before** the preference lookup (2); NORMAL with no preference still throws, and they'd **confirm the choice with the interviewer** (2).

### I4-6: Exactly-once and isolation proof · 10 pts
- **Exactly once (5):** the targets are a **set** computed once, and each element is dispatched once.
- **Isolation (5):** the try/catch is **per channel inside the loop**, so a throw marks that channel FAILED and the loop continues.
- A claim that their own code doesn't support (e.g. they claim isolation but the try wraps the loop): 0 for that half.

### I4-7: Verification trace · 10 pts
- Real calls with the result map after each (4); covers one happy path (2), one partial failure (e.g. URGENT with SMS down → `{EMAIL: SENT, SMS: FAILED, PUSH: SENT}`) (2), and one error (invalid notification or NORMAL without a preference) (2).

---
**Comparison with workbook 03:** the evaluator must report the score delta between 03 and 04 (see RULEBOOK §4). If 04 is more than 15 points below 03, the hints are carrying the student.
**Level calibration:** ≥85 senior · 70–84 mid · 50–69 junior. Without a correct I4-6 (both halves), cap at "mid".
