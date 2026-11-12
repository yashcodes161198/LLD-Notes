# Answer key 02: Entities + class design (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Names don't have to match exactly; responsibilities do.

## Part A: Entities (35 pts)

### E1: Noun list · 5 pts
- **Must:** at least 7 of: user, movie, theatre, screen, show, seat, seat type, booking, hold, payment / payment reference, system.
- 1 pt per noun beyond the second, up to 5.

### E2: Keep/drop decisions · 15 pts
| Noun | Expected | Reason that must appear | Pts |
|---|---|---|---|
| User | DROP | external actor; only `userId` is needed | 1 |
| Movie | DROP | catalogue data owned elsewhere; search out of scope | 2 |
| Theatre / Screen | DROP | administration out of scope; the layout arrives as the show's seats | 3 |
| Payment | DROP | another system; only a reference survives | 2 |
| Show | KEEP | the unit being sold; owns who holds which seat | 2 |
| Seat | KEEP | physical seat with id and type | 2 |
| Booking | KEEP | carries rules: status, expiry, owner, payment ref | 2 |
| BookingSystem | KEEP | orchestrator / entry point | 1 |
- **Red flag −4:** keeps Theatre/Screen/City/Movie as classes with no reason tied to the requirements. If kept with a defensible reason (e.g. "Screen holds the layout shared by shows"), −1 only.
- **Red flag −4:** keeps User or Payment as classes.

### E3: Dropped thing whose value survives · 6 pts
- **Must:** Payment → `paymentRef` → becomes a **parameter** of `confirmBooking(bookingId, paymentRef)` (and is stored on Booking).
- **Accept (full points):** User → `userId` → parameter of `holdSeats(userId, …)`, stored on Booking.

### E4: The "just a list of seat ids" entity · 6 pts
- **Must:** Booking is a class because it has its **own rules**: status lifecycle, 10-minute **expiry**, owner, payment reference (4). There's no Hold class because a hold **is a Booking in PENDING**; two classes for one lifecycle would duplicate the seat list (2).

### E5: Enums · 3 pts
- **Must:** `BookingStatus {PENDING, CONFIRMED, CANCELLED}` (2), `SeatType {REGULAR, PREMIUM}` (1).
- **Accept:** an `EXPIRED` status if they say who sets it (a sweeper). No deduction.

## Part B: Class design (40 pts)

### C1: BookingSystem (orchestrator) · 12 pts
| Member | Pts | Accept |
|---|---|---|
| `- shows: Map<string, Show>` | 2 | any collection keyed by id |
| `- bookings: Map<string, Booking>` | 2 | |
| `- clock` | 2 | any injected time source |
| `+ holdSeats(userId, showId, seatIds[]) -> bookingId` | 2 | must take a **list** of seats and **return an id** |
| `+ confirmBooking(bookingId, paymentRef)` | 2 | −1 if it takes a Payment object |
| `+ cancelBooking(bookingId)` and `+ getAvailableSeats(showId)` | 2 | 1 each |

### C2: Show · 12 pts
- `seats` (layout) (2), **`seatHolders: Map<seatId, Booking>`** or equivalent per-show seat state (4), a **lock** (2), `hold(...)` (2), `release(...)` / `getAvailableSeats(...)` (2).
- **Partial:** `Map<seatId, SeatStatus>` without a link to the booking/expiry: 2 of the 4.

### C3: Booking · 8 pts
- `id/userId/seatIds` (2), `status` (2), `expiresAt` (2), `isActive(now)` / `isExpired(now)` (2).
- **Red flag −3:** expiry checked by the orchestrator comparing timestamps instead of on Booking (breaks Information Expert).

### C4: Seat · 4 pts
- `id`, `type` (1). **No booked/held flag on Seat** (3).
- **Red flag −3 (on top):** `Seat.isBooked` / `Seat.status` while seats are shared by shows: booking the 6 pm show marks the 9 pm one too.

### C5: Top-down and traceability · 4 pts
- Orchestrator written first (2), and members annotated with the requirement they come from (2).

## Part C: Design decisions (25 pts)

### D1: Where "is A7 taken?" lives · 7 pts
- **Must:** on **Show** (or a per-show structure), not Seat (3); because taken-ness is **per show**, a relationship between a show and a booking, while the seat is physical and shared (2); **Information Expert** (2).

### D2: What is stored, and expiry without a timer · 6 pts
- **Must:** `seatId → Booking` (3); "taken" = holder is **active** (CONFIRMED, or PENDING and now < expiresAt), so an expired hold is simply not active: **lazy expiry**, nothing scheduled (3).
- **Partial 3:** a timer/sweeper that flips status, with no mention of the lazy alternative.

### D3: Why derivation is right here · 4 pts
- **Must:** in the Locker, an expired token's compartment is still **physically full**; here an expired hold **really frees** the seat, so the derived value equals reality.

### D4: Lock granularity · 5 pts
- **Must:** **one lock per show** (2). Not global: different shows never contend (1). Not per seat: a multi-seat hold needs several locks, ordering to avoid **deadlock**, more complexity for no measured gain (2).

### D5: Injected clock · 3 pts
- **Must:** expiry must be **testable** (advance time in tests) (2), and one consistent time source (1).

---
**Level calibration:** ≥85 senior · 70–84 mid · 50–69 junior · <50 redo Step 2–3 of `../bookmyshow.py`.
