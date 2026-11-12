"""
================================================================================
 BOOKMYSHOW (seat booking with concurrency): annotated interview solution
 Method: Hello Interview delivery framework (Evan King, "Design Amazon Locker")
         https://www.youtube.com/watch?v=s6nGkoGJhXk
 Family: BookMyShow, event ticketing, hotel booking, meeting-room scheduler.
         Commonly reported as one of the most-repeated LLD prompts.
================================================================================

No video for this one: the design below is built with the framework from
scratch. It's written the way a candidate would type it into one editor file,
with notes added. The obvious version has one real bug, a stale-reference
release; see TRICKY #4 for the AS WRITTEN and BEST versions.

LEGEND (search for these):
    # 💬 SAY:      a talking point to say out loud in the interview, near-verbatim
    # ⚠️ TRICKY:   where candidates lose points, or a subtle bug
    # ✅ BEST:     the strongest approach, and why it beats the alternatives
    # 🎯 GRADED:   which rubric dimension this moment is scored on
    # ⏱            time budget for the step
    # 📸           matching screenshot in ./screenshots/

THE 5-STEP DELIVERY FRAMEWORK (📸 00-delivery-framework.png)
    1. Requirements      ⏱ ~5 min
    2. Entities          ⏱ ~3 min
    3. Class design      ⏱ ~10-15 min
    4. Implementation    ⏱ ~10 min
    5. Extensibility     ⏱ ~5 min (senior+; juniors may never get here)

💬 SAY (at the start): "Before I design anything I'd like a few minutes on
   requirements. I'll go through primary capabilities, error handling and
   scope, then write down what we agree on."
================================================================================


--------------------------------------------------------------------------------
STEP 1: REQUIREMENTS                                  ⏱ ~5 min   📸 00
--------------------------------------------------------------------------------

Design a movie ticket booking system like BookMyShow where users can pick seats for a show and book them.

# ⚠️ TRICKY: the prompt says nothing about concurrency, holds, payment or
#    cancellation. Every one of those is a question you are expected to ASK.
#    Jumping to "class Movie, class Theatre, class City" is the classic
#    failure: you model a catalogue and never reach the hard part.
# 🎯 GRADED: Problem analysis.

1. primary capabilities
2. error handling
3. scope boundaries
# ✅ BEST: write the 3 bucket names first, then fill them in.

Primary capabilities:

- What does the user do: browse movies, or start from a chosen show and pick seats?
      -> Interviewer: start from a show. Search and browsing are out of scope.
- Can a user book several seats in one go?
      -> Yes, one booking can hold several seats of the same show.
- Is there a "hold" while the user pays, like the 10-minute timer on the real site?
      -> Yes. Selected seats are held for 10 minutes; payment confirms them.
- Do we process the payment?
      -> No. A payment service charges the card and calls us with a payment reference.
# ✅ BEST: "do we process payment?" lets the interviewer cut a whole subsystem.
#    Once they do, confirm takes a paymentRef parameter and nothing else.
- Do seats have types and prices?
      -> They have a type (REGULAR, PREMIUM), shown to the user. Pricing is out of scope for now.

Error handling:

- Two users pick the same seat at the same moment?
      -> Exactly one gets it; the other gets "seat unavailable".
# 💬 SAY: "This is the heart of the problem, so I want it written down as a
#    requirement, not left as an implementation detail."
- User picks 3 seats and one of them is already taken?
      -> All-or-nothing: hold none of them and say which seat is taken.
- User never pays?
      -> The hold expires after 10 minutes and the seats go back on sale.
- Payment arrives after the hold expired?
      -> Reject with "hold expired" (refund is the payment service's job).
- Can a user cancel?
      -> Yes, a hold or a confirmed booking. The seats go back on sale. Refunds are out of scope.

Scope boundaries:

- Are we modelling theatres, screens and show scheduling, or only booking seats of an existing show?
      -> Only booking. Shows arrive pre-built with their seat layout.
- One process, or many servers?
      -> One process with many concurrent requests. Multi-server is a follow-up.
# 💬 SAY: "So concurrency means many threads in one process. I'll make the
#    critical section correct there, and talk about the distributed version later."


Requirements:
1. User views the available seats of a show
   - Returns the seats that are neither booked nor held by an unexpired hold, with their type
   - Unknown show -> error
2. User holds one or more seats of a show
   - All-or-nothing: every seat is held, or none is, with "seat unavailable" naming the seat
   - The hold lasts 10 minutes; returns a booking id (status PENDING)
   - Two users racing for the same seat: exactly one wins
3. User confirms the booking with a payment reference (payment done elsewhere)
   - Booking becomes CONFIRMED; its seats stay taken for good
   - Throws specific errors: booking not found, hold expired
4. Holds expire 10 minutes after they are created
   - Expired seats are available to other users again
   - Confirming an expired hold fails with "hold expired"
5. User cancels a booking (held or confirmed)
   - Its seats become available again
   - Booking not found -> error

# ✅ BEST: each requirement = ACTOR + ACTION + INPUT, with sub-bullets for the
#    reaction and the error. Reqs 1, 2, 3, 5 become the 4 public methods; Req 4
#    is a rule enforced inside them.
# ⚠️ TRICKY: "exactly one wins" (Req 2) and "expired seats are available again"
#    (Req 4) look independent. They aren't: together they mean a seat can pass
#    from user A (expired) to user B while A's booking object still lists it.
#    That is where the bug in Step 4 lives.

Out of scope:
- Search / browse movies, theatres, cities
- Payment processing and refunds
- Notifications (email/SMS tickets)
- Pricing
- Theatre, screen and show administration
- User accounts and auth
- Multiple servers (single process for now)
# 💬 SAY: "Everything not on this list I'll treat as out of scope."


--------------------------------------------------------------------------------
STEP 2: ENTITIES                                      ⏱ ~3 min   📸 00
--------------------------------------------------------------------------------
# ✅ BEST: write down EVERY noun, then prune each one out loud.
#
#   Noun            Keep?  Why
#   ------------    -----  -----------------------------------------------------
#   User            NO     External actor. We only need userId as a parameter.
#   Movie           NO     Catalogue data, owned elsewhere; search is out of scope.
#   Theatre/Screen  NO     ⚠️ TRICKY: most candidates start here. Administration is
#                          out of scope; the screen layout arrives as the Show's
#                          seat list. If we managed theatres, Screen would hold the
#                          layout and Show would point at it.
#   Payment         NO     Another system. Survives as a paymentRef PARAMETER of confirm.
#   Show            YES    The thing being sold: one movie, one screen, one time. It
#                          owns WHO HOLDS WHICH SEAT and the lock that guards it.
#   Seat            YES    Physical seat with an id and a type. ⚠️ TRICKY: it does
#                          NOT have a "booked" flag (see Step 3).
#   Booking         YES    ⚠️ TRICKY: "isn't it just a list of seat ids?" No. It has
#                          its own rules: status, a 10-minute expiry, an owner, a
#                          payment reference. It's the hold AND the ticket.
#   Hold            NO     Not a separate class: a hold is a Booking in PENDING.
#                          Two classes for one lifecycle would duplicate the seat list.
#   BookingSystem   YES    Orchestrator / entry point.
#
# 💬 SAY: "I'll cut actors and anything another system owns. If I find I need
#    Theatre later, I'll come back and add it."
# 🎯 GRADED: Class design (the right entities, not too many).

Entities:
- BookingSystem
- Show
- Seat
- Booking
- (enum) SeatType: REGULAR, PREMIUM
- (enum) BookingStatus: PENDING, CONFIRMED, CANCELLED


--------------------------------------------------------------------------------
STEP 3: CLASS DESIGN, TOP-DOWN                        ⏱ ~10-15 min  📸 00
--------------------------------------------------------------------------------
# ✅ BEST: start with the ORCHESTRATOR and work down.
#       state    = "what must I REMEMBER to enforce the requirements?"
#       behavior = "what user ACTION needs this?"  -> public methods
"""

class BookingSystem:
    - shows: Map<string, Show>                          # Req 1-2: look up a show by id
    - bookings: Map<string, Booking>                    # Req 3, 5: look up a booking by id
    - clock: Clock                                      # Req 4: injected so expiry is testable

    + getAvailableSeats(showId) -> Seat[] | error       # Req 1
    + holdSeats(userId, showId, seatIds[]) -> string | error   # Req 2 (returns bookingId)
    + confirmBooking(bookingId, paymentRef) -> void | error    # Req 3
    + cancelBooking(bookingId) -> void | error          # Req 5

    # 💬 SAY: "Maps keyed by id, not for the O(1) but because every call comes
    #    in with an id. A show has a few hundred seats, so scans inside a show
    #    are fine."
    # ✅ BEST: inject the clock. "Expires after 10 minutes" is untestable if the
    #    code calls now() directly.


class Show:
    - id: string
    - seats: Map<string, Seat>                          # the layout: Req 1 lists it, Req 2 validates ids
    - seatHolders: Map<string, Booking>                 # Req 2, 4: seatId -> booking that holds it
    - lock                                              # Req 2: "exactly one wins"

    + getAvailableSeats(now) -> Seat[]                  # Req 1
    + hold(booking, now) -> void | error                # Req 2: atomic, all-or-nothing
    + release(booking) -> void                          # Req 5

    # ✅ BEST (Information Expert): SEAT AVAILABILITY LIVES ON THE SHOW, not on Seat.
    # 💬 SAY: "A seat is physical and the same for every show on that screen.
    #    Whether it's TAKEN is per show: seat A7 is free at 6 pm and booked at
    #    9 pm. So taken-ness is a relationship between a show and a booking, and
    #    the Show owns it. Seat stays immutable."
    # ⚠️ TRICKY (alternatives):
    #    BAD:   Seat.isBooked boolean. Seats are shared across shows of a screen,
    #           so booking 6 pm marks 9 pm too. Or you copy Seat per show and now
    #           have a mutable seat object with two jobs.
    #    GOOD:  Map<seatId, SeatStatus> plus a separate Map<seatId, holdExpiry>.
    #           Works, but status and expiry live in two maps that can drift.
    #    GREAT: Map<seatId, Booking>. The seat is taken iff its holder booking is
    #           ACTIVE (confirmed, or pending and not expired). One source of truth,
    #           and expiry is free: no timer has to flip anything.
    # 💬 SAY: "Contrast with Amazon Locker: there, deriving 'occupied' from the
    #    token was wrong because an expired token's compartment is still
    #    physically full. Here an expired hold really does free the seat, so
    #    deriving it is correct."
    # 🎯 GRADED: Class design (clear ownership).


class Seat:
    - id: string                      # "A7"
    - type: SeatType                  # REGULAR, PREMIUM (shown in Req 1; priced in EXT 1)

    + getId() -> string
    + getType() -> SeatType
    # Immutable. No status here, on purpose.


class Booking:
    - id: string
    - userId: string
    - show: Show
    - seatIds: string[]
    - status: BookingStatus           # PENDING, CONFIRMED, CANCELLED
    - expiresAt: timestamp            # Req 4: createdAt + 10 min
    - paymentRef: string | null       # Req 3

    + isActive(now) -> boolean        # CONFIRMED, or PENDING and now < expiresAt
    + isExpired(now) -> boolean       # PENDING and now >= expiresAt
    + confirm(paymentRef) -> void
    + cancel() -> void

    # ✅ BEST: no EXPIRED status that a timer must set. Expiry is DERIVED from
    #    (status, expiresAt, now) = lazy expiry. Nothing to schedule, nothing to
    #    forget. (A sweeper comes back in EXT 2, for notifications.)
    # ⚠️ TRICKY: Booking.seatIds is a list of what this booking ONCE held. After
    #    expiry another booking can take those seats. The truth about who holds a
    #    seat NOW is Show.seatHolders, never Booking.seatIds. Remember this for
    #    cancelBooking.


"""
--------------------------------------------------------------------------------
STEP 4: IMPLEMENTATION                                ⏱ ~10 min   📸 00
--------------------------------------------------------------------------------
# 💬 SAY: "Pseudo-code or a real language?" Then: "Seat and Booking are mostly
#    getters and one-line comparisons. The interesting parts are holdSeats,
#    the atomic Show.hold, confirmBooking and cancelBooking, so I'll do those."
# ✅ BEST: for EVERY method, docstring FIRST (Core logic + Edge cases), then code.
# 🎯 GRADED: Code quality plus Communication.
"""

# Implementation
# 1. Define the core logic
# 2. Consider the edge cases

HOLD_DURATION = 10 minutes

class BookingSystem:
    holdSeats(userId, showId, seatIds):
        """
        Core logic:
        1. Validate the input
        2. Find the show
        3. Under the show's lock: read now, create a PENDING booking expiring at now + 10 min
        4. Ask the show to hold every seat for it (all-or-nothing)
        5. Store the booking, return its id

        Edge cases:
        1. empty userId / empty seat list / duplicate seat ids -> throw "invalid request"
        2. unknown show -> throw "show not found"
        3. unknown seat id -> throw "invalid seat"          (raised inside Show.hold)
        4. any seat taken -> throw "seat unavailable: A7"   (nothing held, booking NOT stored)
        """
        if userId.isEmpty() or seatIds.isEmpty() or hasDuplicates(seatIds)
            throw Error("invalid request")
        show = shows[showId]
        if show == null
            throw Error("show not found")

        with show.lock:
            now = clock.now()                      # ⚠️ TRICKY #3: read the clock INSIDE the lock
            booking = Booking(newId(), userId, show, seatIds, PENDING, now + HOLD_DURATION)
            show.hold(booking, now)                # throws -> we never reach the next line
        bookings[booking.id] = booking
        return booking.id

        # ⚠️ TRICKY: duplicates. ["A7", "A7"] passes a naive check (A7 is free
        #    the first time) and the booking lists one seat twice. Reject it at
        #    the boundary.
        # ✅ BEST: the booking is stored only AFTER the hold succeeded, so a failed
        #    hold leaves no PENDING ghost in the map.


class Show:
    hold(booking, now):
        """
        Caller holds this show's lock.
        Core logic:
        1. Check every seat id exists
        2. Check every seat is free (no holder, or holder no longer active)
        3. Only then, point every seat at this booking

        Edge cases:
        1. unknown seat -> throw "invalid seat"
        2. a seat is taken -> throw "seat unavailable: <id>", NOTHING changed
        3. a seat's holder expired -> it counts as free (lazy expiry, Req 4)
        """
        for seatId in booking.seatIds
            if seatId not in seats
                throw Error("invalid seat: " + seatId)
        for seatId in booking.seatIds
            holder = seatHolders[seatId]
            if holder != null and holder.isActive(now)
                throw Error("seat unavailable: " + seatId)
        for seatId in booking.seatIds
            seatHolders[seatId] = booking

        # ✅ BEST: CHECK ALL, THEN WRITE ALL. Two loops, not one.
        # ⚠️ TRICKY #1: one loop that writes as it checks leaves A1, A2 held when
        #    A3 turns out to be taken. All-or-nothing breaks, and those seats are
        #    stuck for 10 minutes on a booking that was never returned.
        # ⚠️ TRICKY #2 (THE concurrency point): check-then-act. Without the lock,
        #    two threads both see A7 free and both write it. Holding the SHOW's lock
        #    around check + write makes it atomic.
        # 💬 SAY: "One lock per show, not one global lock: different shows never
        #    contend. Not one lock per seat either: a multi-seat hold would need
        #    several locks, taken in sorted order to avoid deadlock. A show has a
        #    few hundred seats and a hold takes microseconds, so per-show is the
        #    simple, correct choice. I'd only go finer if profiling said so."
        # 🎯 GRADED: Code quality (atomicity, no partial state).

    getAvailableSeats(now):
        with lock:
            return [s for s in seats.values()
                    if seatHolders[s.id] == null or !seatHolders[s.id].isActive(now)]
        # 💬 SAY: "This is a snapshot. A seat shown as free can be gone a second
        #    later; hold() is the real check."


class BookingSystem:
    confirmBooking(bookingId, paymentRef):
        """
        Core logic:
        1. Look up the booking
        2. Under the show's lock: read now
        3. Check it's still PENDING and not expired
        4. Mark it CONFIRMED with the payment reference

        Edge cases:
        1. empty paymentRef -> throw "invalid request"
        2. unknown booking -> throw "booking not found"
        3. already CONFIRMED -> return (idempotent)       ??? decision: yes, see below
        4. CANCELLED -> throw "booking cancelled"
        5. expired -> throw "hold expired"                 (a DIFFERENT error, per Req 3)
        """
        if paymentRef.isEmpty()
            throw Error("invalid request")
        booking = bookings[bookingId]
        # 💬 SAY: "In my pseudo-code a map miss returns null."
        if booking == null
            throw Error("booking not found")

        with booking.show.lock:
            now = clock.now()
            if booking.status == CONFIRMED
                return
            if booking.status == CANCELLED
                throw Error("booking cancelled")
            if booking.isExpired(now)
                throw Error("hold expired")
            booking.confirm(paymentRef)

        # 💬 SAY (the product trade-off): "Payment services retry their callbacks.
        #    If a retry of the same confirm threw, the user would see an error
        #    after paying. So confirming twice is a no-op. Should a different
        #    paymentRef on an already confirmed booking be an error? I'd log it
        #    for the payment team; I won't block the user."
        # 💬 SAY: "Payment landing at 10:01 is rejected even though the card was
        #    charged. That's the agreed rule; the payment service refunds. Some
        #    products add a short grace period. I'd raise it, not decide it alone."
        # 🎯 GRADED: Communication (product decisions raised, not hidden).
        #
        # ⚠️ TRICKY #3 (why confirm needs the SAME lock as hold): user A confirms
        #    at 9:59.999 and user B holds A's seat at 10:00.001.
        #      - Without the lock: A checks "not expired" -> B's hold sees A
        #        expired and takes the seat -> A sets CONFIRMED. Both own A7.
        #      - Lock taken, but now read BEFORE it: A reads 9:59.999, waits for the
        #        lock, B goes first and takes A7, then A checks with its stale time
        #        and confirms. Same double sale.
        #    Fix: the expiry check and the status change are one atomic step under
        #    the show's lock, and now is read inside it.


    cancelBooking(bookingId):
        """
        Core logic:
        1. Look up the booking
        2. Under the show's lock: release its seats (only the ones it still holds)
        3. Mark it CANCELLED

        Edge cases:
        1. unknown booking -> throw "booking not found"
        2. already CANCELLED -> return (idempotent)
        3. expired PENDING -> allowed; its seats may already belong to someone else
        """
        booking = bookings[bookingId]
        if booking == null
            throw Error("booking not found")
        with booking.show.lock:
            if booking.status == CANCELLED
                return
            booking.show.release(booking)
            booking.cancel()


class Show:
    # ---- AS WRITTEN (the obvious version) ----
    release(booking):
        for seatId in booking.seatIds
            seatHolders.remove(seatId)

    # ⚠️ TRICKY #4 (the real bug; spotting it is the SENIOR signal):
    #    booking.seatIds is a STALE REFERENCE. Timeline:
    #      t=0     A holds A7                     seatHolders {A7: A}
    #      t=11    A's hold expired; B holds A7   seatHolders {A7: B}   (lazy expiry lets B in)
    #      t=12    A taps "cancel" on the old page
    #              -> remove(A7) removes B's entry  seatHolders {}
    #      t=13    C holds A7 (looks free), B and C both confirm -> A7 SOLD TWICE.
    #    Same shape as Amazon Locker's TRICKY #6: an old object still points at a
    #    resource that has since been reused.
    #
    # ✅ BEST FIX: release BY OWNER, not by seat (compare-and-remove).

    # ---- BEST VERSION ----
    release(booking):
        for seatId in booking.seatIds
            if seatHolders[seatId] == booking          # only if this booking still holds it
                seatHolders.remove(seatId)
    # 💬 SAY: "The seat map is the source of truth; the booking's seat list is
    #    history. So release only removes an entry that still points at this
    #    booking. In Java that's literally map.remove(seatId, booking)."
    # Also correct: don't remove anything; just set CANCELLED, and the lazy
    # isActive check frees the seats. Removing is tidier and makes EXT 2 easier.


"""
VERIFICATION TRACE (show S1, seats A1 A2 A3 REGULAR, P1 PREMIUM; clock at 0:00)
    getAvailableSeats(S1)                 -> [A1, A2, A3, P1]
    holdSeats(u1, S1, [A1, A2])   at 0:00 -> "B1" PENDING until 0:10; holders {A1:B1, A2:B1}
    holdSeats(u2, S1, [A2, A3])   at 0:01 -> throw "seat unavailable: A2"; A3 still free; no B2 stored
    holdSeats(u2, S1, [])                 -> throw "invalid request"
    holdSeats(u2, S1, [Z9])               -> throw "invalid seat: Z9"
    confirmBooking("B1", "pay_1") at 0:05 -> B1 CONFIRMED
    confirmBooking("B1", "pay_1") again   -> no-op (idempotent)
    holdSeats(u3, S1, [A3])       at 0:05 -> "B2", until 0:15
    confirmBooking("B2", "pay_2") at 0:16 -> throw "hold expired"; A3 available again
    holdSeats(u4, S1, [A3])       at 0:16 -> "B3"  (took A3 from expired B2)
    cancelBooking("B2")           at 0:17 -> B2 CANCELLED; A3 STILL held by B3   <- the trap, avoided
    cancelBooking("B1")                   -> A1, A2 free again
    confirmBooking("B9", "x")             -> throw "booking not found"
    20 threads holdSeats(.., [P1]) at once -> exactly 1 booking id, 19 "seat unavailable"
"""


"""
--------------------------------------------------------------------------------
STEP 5: EXTENSIBILITY                                 ⏱ ~5 min   📸 00
--------------------------------------------------------------------------------
# ✅ BEST: say WHERE the change lands, describe it, code it if asked.
# 🎯 GRADED: Extensibility (small changes, no rewrite).
"""

# Extensions
"""
1. "Premium seats cost more, and prices may change at peak hours. How do you add pricing?"
2. "When a hold expires or someone cancels, notify users waiting for that show."
3. "Reuse this for meeting rooms or hotel rooms, where people book a time range."
4. "We now run 20 servers behind a load balancer. Does your lock still work?"
5. "A blockbuster opens and 50k users hit one show. Is the per-show lock a bottleneck?"
6. "Bots hold hundreds of seats and never pay. How do you stop that?"
"""

# ---------------------------------------------------------------------------
# EXT 1: pricing
# Pattern: STRATEGY. PricingStrategy.priceOf(seat) -> amount. SeatTypePricing
#    maps type -> price; peak-hour or surge pricing is another implementation.
# Lands in: holdSeats (compute the total once, at hold time) + Booking.amount.
# 💬 SAY: "The price is fixed when the hold is created and stored on the
#    booking, so the user pays what they saw even if the price changes during
#    their 10 minutes. Money is an integer in the smallest unit, never a float."
# ---------------------------------------------------------------------------
interface PricingStrategy:
    + priceOf(seat) -> long

class SeatTypePricing implements PricingStrategy:
    - prices: Map<SeatType, long>          # fail at construction if a type is missing
    + priceOf(seat): return prices[seat.type]

# in holdSeats, after show.hold(booking, now) succeeds:
#     booking.amount = sum(pricing.priceOf(show.seats[id]) for id in seatIds)
# ⚠️ TRICKY: don't price at confirm time. The price may have moved, and the user
#    would be charged something different from what the payment service charged.


# ---------------------------------------------------------------------------
# EXT 2: notify waiting users when seats free up
# Pattern: OBSERVER (SeatReleaseListener) + an EXPIRY SWEEPER.
# Lands in: Show.release returns the seats it actually freed; cancelBooking and
#    a new releaseExpiredHolds() call listeners. The hold/confirm logic is unchanged.
# 💬 SAY: "Lazy expiry has a catch: nothing HAPPENS when a hold expires, so
#    there's no moment to fire an event. I add a sweeper that runs every few
#    seconds, removes expired holders, and tells the listeners which seats came
#    back. Listeners are called outside the lock."
# ---------------------------------------------------------------------------
interface SeatReleaseListener:
    + onSeatsReleased(showId, seatIds[])

releaseExpiredHolds():                       # BookingSystem, run by a scheduler
    for show in shows.values()
        with show.lock:
            now = clock.now()
            freed = show.removeExpiredHolders(now)    # iterates seatHolders, NOT bookings
        if freed not empty
            for l in listeners: l.onSeatsReleased(show.id, freed)
# ⚠️ TRICKY: iterate the SEAT MAP (truth), not the bookings. Iterating expired
#    bookings and removing their seatIds is TRICKY #4 again: it would free a
#    seat that has since been re-held by someone else.
# ⚠️ TRICKY: call listeners OUTSIDE the lock. A slow email call inside it would
#    block every hold on that show.


# ---------------------------------------------------------------------------
# EXT 3: meeting rooms / hotel rooms (time-range booking)
# Pattern: same skeleton, different CONFLICT CHECK. The key changes from
#    (show, seat) to (room, time interval); "is the seat free?" becomes
#    "does any active booking OVERLAP [start, end)?"
# Lands in: a RoomSchedule per room replaces Show's seat map. Lock per room,
#    check-all-then-write, release by owner and hold/confirm/expire stay the same.
# 💬 SAY: "Everything about concurrency carries over. Only the conflict test
#    changes: two intervals overlap iff a.start < b.end and b.start < a.end.
#    Half-open intervals so a 10:00-11:00 and 11:00-12:00 don't clash. For a
#    hotel, the unit is a night and the resource is a room TYPE with a count."
# ---------------------------------------------------------------------------
class TimeSlot:
    - start, end                    # validated start < end
    + overlaps(other) -> boolean    # start < other.end and other.start < end

class RoomSchedule:
    - roomId
    - reservations: Reservation[]
    + reserve(userId, slot) -> reservationId | error     # with lock: scan active ones for overlap, then add
    + cancel(reservationId)
# ⚠️ TRICKY: a linear scan is fine for one room's day (tens of entries). A
#    sorted tree (TreeMap by start) is the follow-up if the interviewer pushes on n.


# ---------------------------------------------------------------------------
# EXT 4: many servers                                       (describe only)
# Pattern: move the atomic check-and-write INTO THE DATABASE.
# Lands in: Show.hold becomes one conditional write; the in-process lock goes.
# 💬 SAY: "A Java lock only protects one JVM. With 20 servers the database
#    must decide. One row per (show, seat) with holder and expiresAt. Hold is
#      UPDATE show_seat SET holder=?, expires_at=?
#      WHERE show_id=? AND seat_id IN (...) AND (holder IS NULL OR expires_at < now)
#    in a transaction, and I check the row count equals the seat count; if not,
#    roll back. That's optimistic, all-or-nothing, and needs no lock service.
#    Redis SET NX PX with a TTL is the other common answer, but then the
#    database still needs a unique constraint on (show, seat) for confirmed
#    tickets as the final guard."
# ---------------------------------------------------------------------------


# ---------------------------------------------------------------------------
# EXT 5: hot show contention                                (describe only)
# Pattern: FINER-GRAINED LOCKING, ordered acquisition.
# Lands in: Show.hold only.
# 💬 SAY: "Measure first: a hold is a few map operations. If the per-show lock
#    really is hot, lock per seat, always taking locks in sorted seat-id order
#    so two multi-seat holds can't deadlock. Or go optimistic: putIfAbsent per
#    seat, and on the first conflict roll back the ones I took. Most of the real
#    load is reads of the seat map, which a cached snapshot can serve."
# ---------------------------------------------------------------------------


# ---------------------------------------------------------------------------
# EXT 6: bots hoarding seats                                (describe only)
# Pattern: LIMITS at the boundary.
# Lands in: the top of holdSeats.
# 💬 SAY: "Cap seats per booking (say 10) and active holds per user per show
#    (say 1). A new hold from the same user replaces the old one. Shorter holds
#    for suspicious traffic, and CAPTCHA/rate limiting at the API gateway."
# ---------------------------------------------------------------------------


"""
================================================================================
 MORE FOLLOW-UPS SENIORS GET (short answers)
================================================================================
- Cancel after the show started? -> Show gets startTime; cancelBooking throws
  "cancellation closed" after startTime minus a cutoff. Product decision.
- Seat map UI shows stale "free" seats? -> Expected. getAvailableSeats is a
  snapshot; hold is the real check. Push seat updates over websockets (EXT 2's
  listener) to cut the stale window.
- Payment succeeded but our confirm call was lost? -> The payment service
  retries with the same paymentRef; confirm is idempotent, so retries are safe.
  A reconciliation job refunds payments whose booking expired.
- Memory: bookings never leave the map? -> Archive bookings of shows that ended.
  A few hundred thousand per day is fine in memory short-term.
- Group booking must sit together? -> A seat-picking helper on Show; hold
  stays all-or-nothing, so the result is still atomic.

================================================================================
 60-SECOND RECAP (say this at the end)
================================================================================
- 4 entities: BookingSystem (orchestrator), Show (who holds which seat + the
  lock), Seat (immutable, no status), Booking (hold + ticket, with expiry).
  Movie, Theatre, User and Payment are external.
- Seat availability is per show, so it lives on Show as seatId -> Booking.
  "Taken" is derived from the booking's status and expiry: lazy expiry, one
  source of truth.
- Concurrency: one lock per show. hold = check all, then write all, under the
  lock. Confirm uses the same lock and reads the clock inside it.
- The trap: a booking's seat list is history. Release by owner
  (compare-and-remove), or a late cancel frees someone else's seat.
- Extensions: pricing = Strategy fixed at hold time; notifications = Observer
  plus a sweeper over the seat map; rooms/hotels = same skeleton with an
  overlap check; many servers = conditional write in the database.
================================================================================
"""
