# Answer key 03: Implementation, guided (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Any pseudo-code syntax is fine. Judge **logic, order, locking and errors**, not syntax. Mentally run every body against the trace in I3-7, and against two threads calling at once.

### I3-1: holdSeats · 18 pts
```
if userId.isEmpty() or seatIds.isEmpty() or hasDuplicates(seatIds)
    throw Error("invalid request")
show = shows[showId]
if show == null
    throw Error("show not found")
with show.lock:
    now = clock.now()
    booking = Booking(newId(), userId, show, seatIds, PENDING, now + HOLD_DURATION)
    show.hold(booking, now)
bookings[booking.id] = booking
return booking.id
```
| Check | Pts |
|---|---|
| Rejects empty user / empty list | 2 |
| Rejects duplicate seat ids | 2 |
| Show lookup, "show not found" | 2 |
| Booking creation + `show.hold` inside **the show's lock** | 4 |
| `now` read **inside** the lock | 2 |
| Booking is PENDING with `expiresAt = now + 10 min` | 2 |
| Booking stored **only after** hold succeeds | 2 |
| Returns the booking id | 2 |
- **Red flag −4:** one global lock for every show (correct but serialises everything), unless they say why. **−6:** no lock at all.

### I3-2: Show.hold · 20 pts
```
for seatId in booking.seatIds
    if seatId not in seats
        throw Error("invalid seat: " + seatId)
for seatId in booking.seatIds
    holder = seatHolders[seatId]
    if holder != null and holder.isActive(now)
        throw Error("seat unavailable: " + seatId)
for seatId in booking.seatIds
    seatHolders[seatId] = booking
```
| Check | Pts |
|---|---|
| Unknown seat id → error | 3 |
| Free = no holder **or holder not active** (expired counts as free) | 5 |
| **All checks before any write** (two passes, or equivalent rollback) | 8 |
| Writes every seat to this booking | 2 |
| Error names the taken seat | 2 |
- **Red flag −8:** writes while checking with no rollback (partial holds). **−4:** treats any holder as taken, so an expired hold blocks the seat forever (breaks Req 4).

### I3-3: confirmBooking · 20 pts
```
if paymentRef.isEmpty()                 throw Error("invalid request")
booking = bookings[bookingId]
if booking == null                      throw Error("booking not found")
with booking.show.lock:
    now = clock.now()
    if booking.status == CONFIRMED      return      # idempotent: payment callbacks retry
    if booking.status == CANCELLED      throw Error("booking cancelled")
    if booking.isExpired(now)           throw Error("hold expired")
    booking.confirm(paymentRef)
```
| Check | Pts |
|---|---|
| Empty paymentRef rejected | 2 |
| Lookup, "booking not found" | 3 |
| Status check + confirm inside the **same show lock** as hold | 4 |
| `now` read inside the lock | 2 |
| Already CONFIRMED: a **decision with a reason** (idempotent no-op preferred; an error is accepted if justified) | 3 |
| CANCELLED → error | 2 |
| Expired → a **distinct** "hold expired" error, before any mutation | 3 |
| Marks CONFIRMED with the payment ref | 1 |
- **Red flag −6:** no expiry check (an expired hold whose seat was re-sold gets confirmed → double sale).

### I3-4: cancelBooking + Show.release · 20 pts
```
booking = bookings[bookingId]
if booking == null                      throw Error("booking not found")
with booking.show.lock:
    if booking.status == CANCELLED      return
    booking.show.release(booking)
    booking.cancel()

Show.release(booking):
    for seatId in booking.seatIds
        if seatHolders[seatId] == booking
            seatHolders.remove(seatId)
```
| Check | Pts |
|---|---|
| Lookup, "booking not found" | 3 |
| Inside the show's lock | 3 |
| Already CANCELLED → return | 2 |
| **Release by owner**: removes a seat only if this booking still holds it | 8 |
| Marks CANCELLED | 2 |
| Release and status change together, under the lock | 2 |
- **Accept (full points):** no removal at all, only `booking.cancel()`, **if** they say the lazy `isActive` check frees the seats.
- **AS WRITTEN version** (`seatHolders.remove(seatId)` for every seat in `booking.seatIds`): lose the 8-pt criterion; max 12.

### I3-5: Clock inside the lock · 4 pts
- **Must:** A reads now = 9:59.999, then waits for the lock (1). B's hold gets the lock first at 10:00.001, sees A expired and takes the seat (1). A then gets the lock and checks with its **stale** time: "not expired" → confirms (1). Both own the seat (1).
- **Accept:** the same interleaving told for hold vs hold.

### I3-6: Single-loop hold · 6 pts
- **Must:** A1 and A2 stay pointed at a booking that was **never stored or returned** (3); they look taken for 10 minutes and nobody can cancel them; all-or-nothing (Req 2) is broken (3).

### I3-7: Trace · 12 pts
- B1 returned at 0:00 (1). At 0:11 B1 is expired, so B2 gets A7: holders {A7: B2} (2).
- **Must (6):** with the obvious release (remove by seat), cancelling B1 at 0:12 **removes B2's entry**; at 0:13 B3 gets A7; both B2 and B3 confirm → **A7 sold twice**.
- **Must (3):** fix = release only if `seatHolders[seatId] == booking` (compare-and-remove, `map.remove(seatId, booking)`), or don't remove at all.
- If they wrote the BEST release and say: cancel B1 leaves {A7: B2}, u3 gets "seat unavailable: A7", B2 confirms, "B3" → "booking not found", **and** name why the naive version would fail: full points.

---
**Level calibration:** ≥85 senior · 70–84 mid · 50–69 junior. I3-7's double-sale spot is the senior signal: without it, cap the level at "mid" whatever the score.
