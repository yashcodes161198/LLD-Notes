# Answer key 05: Extensibility (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Each question is scored on **Pattern / Lands in / Say / Code**. "Describe only" for Code is acceptable for X4–X6 (the code points go to the description instead). For X1–X3 a code delta is expected.

General red flag for any question, **−5:** the answer rewrites the design or touches many classes when one place would do.

---

### X1: Size fallback · 20 pts
- **Pattern (4):** add one outer loop over an **ordered** list of sizes, starting from the requested size and going **up**.
- **Lands in (5):** `getAvailableCompartment` only; `depositPackage` unchanged.
- **Say (4):** names the "only one method changes" point, and **never smaller**.
- **Code (7):**
```
private getAvailableCompartment(requestedSize)
    sizesInOrder = [SMALL, MEDIUM, LARGE]
    startIndex = sizesInOrder.indexOf(requestedSize)
    for i from startIndex to sizesInOrder.length - 1
        for c in compartments
            if c.getSize() == sizesInOrder[i] && !c.isOccupied()
                return c
    return null
```
- **Bonus +2 (cap 20):** raises the product question "should small packages be allowed to use up the large compartments?"

### X2: Broken / maintenance · 20 pts
- **Pattern (5):** **boolean → enum** once there are more than 2 states; the entity owns its status (**Information Expert**).
- **Lands in (5):** Compartment (`status: CompartmentStatus`), plus the helper's check changes to `isAvailable()`.
- **Say (4):** "This is why occupancy lives on Compartment; the change stays inside it."
- **Code (6):** `enum CompartmentStatus {AVAILABLE, OCCUPIED, OUT_OF_SERVICE}`; methods `isAvailable()`, `markOccupied()`, `markAvailable()`, `markOutOfService()`.
- **Bonus +2:** mentions a broken compartment that **still holds a package** (don't orphan the live token).
- **Red flag −4:** adds a separate `brokenCompartments` set on Locker (duplicated state).

### X3: Confirm the deposit · 25 pts
- **Pattern (6):** **two-phase commit**: reserve → confirm, plus cancel/timeout.
- **Lands in (5):** Locker's deposit API is split into `reserveCompartment(size)`, `confirmDeposit(reservationId)` and `cancelReservation(reservationId)`; Compartment gains a `RESERVED` status; a `reservationMapping` is added.
- **Say (5):** the problem today is a code issued for an **empty compartment** and a compartment lost forever. Only **confirm** issues the token. In production, confirm comes from a **weight or door sensor**; here the driver presses confirm.
- **Code (6):** reserve (find → markReserved → open → id → store), and confirm (lookup → markOccupied → generate token → store → remove reservation → return code).
- **Must mention (3):** a **timeout or cancel** so abandoned reservations don't leak compartments.

### X4: Brute-force code guessing · 12 pts
- **Pattern (4):** rate limiting / lockout after N failed attempts within a time window, plus high-entropy codes.
- **Lands in (4):** a small new attempt tracker, checked at the **top of `pickup`**; nothing else changes.
- **Say (4):** both parts: longer random codes **and** a lockout. Mentions that the written breakdown treats this as out of scope in the base version.

### X5: Concurrent deposits · 13 pts
- **Pattern (5):** make "find + mark" **atomic**: a lock around the find-and-mark section of deposit, or compare-and-set on the compartment status.
- **Lands in (4):** `depositPackage` / `getAvailableCompartment` (or `Compartment.tryReserve()` returning a boolean).
- **Say (4):** names the race (both drivers see M1 as free, and both get M1) and why the check-then-act gap is the bug.
- **Red flag −4:** "use a database transaction" with no explanation of what's atomic.

### X6: 500 locations · 10 pts
- **Pattern (4):** add a layer **above** Locker (`LockerStation` / `LockerRegistry` / service). Locker itself is **unchanged**.
- **Lands in (3):** a new registry class; codes become unique per locker, or are namespaced (locker id + code).
- **Say (3):** choosing *which* locker is the out-of-scope delivery logistics; our class still models one locker.
- **Red flag −3:** turns Locker into a giant class that holds every location's compartments.

---
**Level calibration:** ≥80 senior/staff · 60–79 senior-ish mid · 40–59 mid · <40 junior (juniors often never reach this step, so don't be discouraged).
