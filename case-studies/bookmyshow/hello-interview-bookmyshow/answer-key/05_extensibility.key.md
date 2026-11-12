# Answer key 05: Extensibility (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Each question is scored on **Pattern / Lands in / Say / Code**. "Describe only" for Code is acceptable for X4–X6 (the code points go to the description instead). For X1–X3 a code delta is expected.

General red flag for any question, **−5:** the answer rewrites the design or touches many classes when one place would do.

---

### X1: Pricing · 20 pts
- **Pattern (4):** **Strategy**: `PricingStrategy.priceOf(seat)`; seat-type pricing and peak/surge pricing are implementations.
- **Lands in (5):** `holdSeats` computes the total once; `Booking` gains `amount`. Show/Seat logic unchanged.
- **Say (5):** price is **fixed at hold time** so the user pays what they saw (3); money as integer minor units, not float (2).
- **Code (6):**
```
interface PricingStrategy:  + priceOf(seat) -> long
class SeatTypePricing:      - prices: Map<SeatType, long>   + priceOf(seat): return prices[seat.type]
# in holdSeats, after show.hold succeeds:
booking.amount = sum(pricing.priceOf(show.seats[id]) for id in seatIds)
```
- **Red flag −4:** prices at confirm time (can differ from what was charged). **−3:** `if seat.type == PREMIUM` branches inside BookingSystem.

### X2: Notify waiting users · 20 pts
- **Pattern (5):** **Observer** (`SeatReleaseListener.onSeatsReleased(showId, seatIds)`) **plus an expiry sweeper**.
- **Lands in (4):** `Show.release` returns the seats it actually freed; `cancelBooking` and a new `releaseExpiredHolds()` notify listeners.
- **Say (5):** lazy expiry means **nothing happens** at expiry, so there's no event to fire without a sweeper (3); listeners called **outside the lock** (2).
- **Code (4):** sweeper loops shows, under each lock removes holders that are expired, then notifies with the freed seats.
- **Must mention (2):** the sweeper iterates the **seat map**, not the bookings' seat lists (the release-by-owner trap again).

### X3: Meeting / hotel rooms · 20 pts
- **Pattern (5):** same skeleton, the **conflict check changes**: key (room, time interval), seat-free test becomes **interval overlap**.
- **Lands in (4):** a per-room schedule replaces Show's seat map; lock per resource, check-then-write, release by owner, hold/confirm stay.
- **Say (5):** overlap iff `a.start < b.end && b.start < a.end` (3); **half-open** intervals so back-to-back slots don't clash (2).
- **Code (6):** `TimeSlot(start, end)` validated `start < end` with `overlaps()`; `RoomSchedule.reserve(userId, slot)` scans active reservations for overlap under the lock, then adds.
- **Bonus +2 (cap 20):** hotel = nights × room **type** with a count (inventory per night), not individual rooms.

### X4: Many servers · 15 pts
- **Pattern (5):** move the atomic check-and-write into the **database** (conditional update / optimistic concurrency) or a distributed lock with TTL.
- **Lands in (3):** `Show.hold` / `release` become storage calls; the in-process lock goes.
- **Say (7):** a JVM lock protects one process only (2); conditional `UPDATE … WHERE holder IS NULL OR expires_at < now` in a transaction, checking the **row count** equals the seat count, else roll back (3); a **unique constraint** on (show, seat) as the final guard for confirmed tickets (2).
- **Red flag −4:** "use a distributed lock" with no TTL and nothing about what happens if the holder dies.

### X5: Hot show · 13 pts
- **Pattern (5):** **measure first**; then finer-grained locks (per seat, acquired in **sorted order**) or optimistic per-seat `putIfAbsent` with rollback.
- **Lands in (3):** `Show.hold` only.
- **Say (5):** names **deadlock** risk for multi-seat holds and the sorted-order fix (3); most load is reads of the seat map, served by a snapshot/cache (2).

### X6: Bots hoarding seats · 12 pts
- **Pattern (4):** **limits at the boundary**: max seats per booking, max active holds per user per show.
- **Lands in (4):** top of `holdSeats` (a counter of the user's active holds per show).
- **Say (4):** a new hold from the same user can replace the old one; rate limiting / CAPTCHA belongs at the gateway, out of this class.

---
**Level calibration:** ≥80 senior/staff · 60–79 senior-ish mid · 40–59 mid · <40 junior (juniors often never reach this step, so don't be discouraged).
