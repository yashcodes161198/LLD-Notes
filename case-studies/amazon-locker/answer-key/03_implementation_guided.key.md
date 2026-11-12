# Answer key 03: Implementation, guided (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Any pseudo-code syntax is fine. Judge **logic, order and errors**, not syntax. Mentally run every body against the trace in I3-7.

### I3-1: depositPackage · 22 pts
```
compartment = getAvailableCompartment(size)
if compartment == null
    throw Error("No available compartment of that size")
compartment.open()
compartment.markOccupied()
accessToken = generateAccessToken(compartment)
code = accessToken.getCode()
accessTokensMapping[code] = accessToken
return code
```
| Check | Pts |
|---|---|
| Uses the helper to find a compartment | 3 |
| Null check → throw **before** any mutation | 5 |
| Opens the compartment | 2 |
| Marks it occupied | 4 |
| Generates the token **for that compartment** | 3 |
| Stores it in the map **keyed by code** | 3 |
| Returns the code (string), not the token object | 2 |
- **Red flag −3:** calls `getCode()` twice or otherwise duplicates work (minor). **−5:** marks occupied before the null check, or stores the token before the compartment exists.

### I3-2: getAvailableCompartment · 12 pts
```
for c in compartments
    if c.getSize() == size && !c.isOccupied()
        return c
return null
```
- Loop (3), size match (3), free check (3), returns the first match (1), returns null when none match (2).
- **Red flag −4:** throws inside the helper (the docstring says return null; the caller decides).

### I3-3: pickup · 28 pts
```
if code.isEmpty()                  throw Error("invalid code")
accessToken = accessTokensMapping[code]
if accessToken == null             throw Error("invalid code")
if accessToken.isExpired()         throw Error("access code expired")
compartment = accessToken.getCompartment()
compartment.open()
compartment.markFree()
accessTokensMapping.remove(code)
```
| Check | Pts |
|---|---|
| Empty-code check | 3 |
| Lookup in the map | 3 |
| Missing → "invalid" | 4 |
| Expired → a **different** message | 6 |
| All checks happen **before** any state change | 4 |
| Opens + marks free | 4 |
| Removes the code from the map | 4 |
- **Red flag −6:** frees the compartment or removes the token when the code is expired (the package must stay for staff, Req 4).

### I3-4: openExpiredCompartments · 20 pts
Two answers are accepted. **A** (video) earns up to 16. **B** (best) earns up to 20.

**A, as in the video (max 16):**
```
for code, token in accessTokensMapping
    if token.isExpired()
        c = token.getCompartment(); c.open(); c.markFree()
        # not removed: so the customer still gets "expired" instead of "invalid"
```
- Iterates the map (3), filters expired (3), opens (2), marks free (3), **decision comment with the reason** (5).

**B, best (max 20):** same as A, but the expired code is moved out of the live map into an `expiredCodes` set (or the token is marked *cleared*), and pickup checks that set to keep the "expired" message (+4 over A).
- **Red flag −4:** removes expired tokens with no comment about the trade-off (silently gives worse errors). **−3:** mutates the map while iterating it without copying (only if they remove entries).

### I3-5: Physical assumption · 6 pts
- **Must:** the door auto-closes and the customer actually takes the package (3). Says they'd **confirm with the interviewer** and that real lockers use sensors (3).

### I3-6: Map-miss behaviour · 4 pts
- **Must:** it's language-dependent (e.g. Python raises KeyError); assume null in pseudo-code (2); say it out loud so the interviewer knows it's a deliberate assumption (2).

### I3-7: Trace · 8 pts
- deposit → M1 occupied; pickup on day 9 → "expired", M1 stays occupied (2); openExpired → M1 opens and is freed (2); new deposit → M1 again (1).
- **Must (3):** spots that with video version A, the second `openExpiredCompartments` sees the **old expired token, which still points at M1**, and **reopens M1 on the new customer's package**. That's a bug. Fix: drop the token from the live map / move it to an expired set.
- If they wrote version B and say "nothing is wrong, because I moved the code out", give full points.

---
**Level calibration:** ≥85 senior · 70–84 mid · 50–69 junior. I3-7's bug-spot is the senior signal: without it, cap the level at "mid" whatever the score.
