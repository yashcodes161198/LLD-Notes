# BookMyShow (Hello Interview version): Java code

Runnable Java for the design in [`../bookmyshow.py`](../bookmyshow.py). Run either phase with `./run.sh` (JDK 17+). `Main` is a set of self-checks: it prints each step and throws if anything is wrong. In Phase 2, a deliberately failing listener prints `[warn]` lines to stderr; that's expected.

```
Phase-N/src/bookmyshow/
  model/      SeatType, BookingStatus, Seat, Booking, Show
  service/    BookingSystem (orchestrator) + SeatUnavailableException, HoldExpiredException,
              BookingNotFoundException, ShowNotFoundException
  pricing/    PricingStrategy + SeatTypePricing, FlatPricing                (Phase 2)
  notifier/   SeatReleaseListener + WaitlistNotifier (stub, prints)         (Phase 2)
  room/       TimeSlot, Reservation, RoomSchedule, SlotUnavailableException (Phase 2)
  Main.java   self-checks
```

Phase 1 has no out-of-scope seam to stub: payment arrives as a `paymentRef` parameter. Phase 2's `WaitlistNotifier` is the stub for the out-of-scope notification service.

## Phase 1: Core Requirements

Requirements 1-5 from Step 1, using the BEST versions from Step 4 (release by owner, not the TRICKY #4 trap).

| Class | Role |
|---|---|
| `BookingSystem` | `getAvailableSeats`, `holdSeats -> bookingId`, `confirmBooking(bookingId, paymentRef)`, `cancelBooking`. Every read-then-write on a show runs in `synchronized (show)` with the clock read inside. Stores a booking only after its hold succeeded. Fails at construction on duplicate shows. |
| `Show` | Owns `seatHolders: seatId -> Booking` and the lock (its monitor). `hold` checks every seat, then writes every seat (all-or-nothing). `release` uses `map.remove(seatId, booking)`, so a seat re-held by someone else is left alone. |
| `Booking` | The hold and the ticket. `isActive(now)` / `isExpired(now)` derive expiry from `PENDING + expiresAt` (lazy expiry, no timer). |
| `Seat` | Immutable id + type. No booked flag: that's per show. |
| Exceptions | One per requirement-named error: seat unavailable, hold expired, booking not found, show not found. Bad input is `IllegalArgumentException`. |

Checks in `Main`: the verification trace from the design, the trap (a late cancel of an expired hold doesn't free the seat a newer hold took), input validation, 200 rounds of 20 threads on one seat (exactly one winner), 200 rounds of overlapping multi-seat holds (one winner, no partial holds), bad config at startup, and a demo with the system clock showing the same seat free at 9 pm while booked at 6 pm.

## Phase 2: Extensions

Each extension lands where the design says. Phase 1's `new BookingSystem(shows, clock)` still works (free pricing, no listeners).

| Extension | Pattern | What changed |
|---|---|---|
| 1. Pricing | Strategy | `PricingStrategy.priceOf(seat)` with `SeatTypePricing` (fails at construction if a type has no price) and `FlatPricing`. `holdSeats` prices once, at hold time; `Booking.amount` in minor units. |
| 2. Notify waiting users | Observer + expiry sweeper | `SeatReleaseListener`; `Show.release` now returns the seats it actually freed; new `Show.removeExpiredHolders(now)` walks the seat map (not booking lists). `cancelBooking` and new `releaseExpiredHolds()` notify outside the lock; a failing listener is logged, never thrown. |
| 3. Meeting / hotel rooms | Same skeleton, overlap check | `RoomSchedule` per room: `reserve(userId, TimeSlot)` scans for overlap and adds under the room's lock; `cancel` by reservation id. `TimeSlot` is half-open and validates `start < end`. |

Not coded (answered in words in the design's Step 5 and "More follow-ups"): multi-server holds via a conditional database write (EXT 4), finer-grained per-seat locking (EXT 5), per-user hold limits against bots (EXT 6), cancellation cutoff before show time, websocket seat-map updates, payment reconciliation.
