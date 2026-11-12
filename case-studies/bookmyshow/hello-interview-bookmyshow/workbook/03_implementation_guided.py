"""
WORKBOOK 03: IMPLEMENTATION (GUIDED)             BookMyShow (seat booking)
Time box: 15 minutes. Closed book. Pseudo-code is fine; any consistent syntax works.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the class design
    HOLD_DURATION = 10 minutes
    class BookingSystem:
        - shows: Map<string, Show>
        - bookings: Map<string, Booking>
        - clock: Clock                                  # clock.now()
        + getAvailableSeats(showId) -> Seat[] | error
        + holdSeats(userId, showId, seatIds[]) -> string | error
        + confirmBooking(bookingId, paymentRef) -> void | error
        + cancelBooking(bookingId) -> void | error
    class Show:
        - id: string
        - seats: Map<string, Seat>                      # the layout
        - seatHolders: Map<string, Booking>             # seatId -> booking holding it
        - lock                                          # use as:  with show.lock: ...
        + getAvailableSeats(now)  + hold(booking, now)  + release(booking)
    class Seat:     id, type: SeatType                  (immutable)
    class Booking:
        - id, userId, show: Show, seatIds: string[], status: BookingStatus,
          expiresAt: timestamp, paymentRef
        + isActive(now)   # CONFIRMED, or PENDING and now < expiresAt
        + isExpired(now)  # PENDING and now >= expiresAt
        + confirm(paymentRef)  + cancel()
    enum BookingStatus: PENDING, CONFIRMED, CANCELLED
    You may call newId() and hasDuplicates(list) without implementing them.

The core logic and edge cases are PRE-FILLED as hints. Write the METHOD BODY
under each "# YOUR CODE". Each core-logic step should become one or two lines.
"""

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
        3. any seat unknown or taken -> Show.hold throws; booking must NOT be stored
        """
        # YOUR CODE                                        [I3-1]
        TODO


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
        3. a seat's holder expired -> it counts as free
        """
        # YOUR CODE                                        [I3-2]
        TODO


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
        3. already CONFIRMED -> ???  <- decide, and explain in a comment
        4. CANCELLED -> throw "booking cancelled"
        5. expired -> throw "hold expired" (must be a DIFFERENT message from 2)
        """
        # YOUR CODE                                        [I3-3]
        TODO


    cancelBooking(bookingId):
        """
        Core logic:
        1. Look up the booking
        2. Under the show's lock: release its seats (only the ones it still holds)
        3. Mark it CANCELLED

        Edge cases:
        1. unknown booking -> throw "booking not found"
        2. already CANCELLED -> return
        3. expired PENDING -> allowed; its seats may already belong to someone else
        """
        # YOUR CODE (cancelBooking AND Show.release)       [I3-4]
        TODO


# ---------------------------------------------------------------------------
# Q1. In holdSeats, why must clock.now() be read INSIDE the lock rather than
#     at the top of the method? Give the interleaving that breaks.     [I3-5]
# TODO
#
# Q2. Someone writes Show.hold with ONE loop that checks a seat and writes it
#     immediately. Hold [A1, A2, A3] where A3 is taken. What state is left
#     behind, and why does it matter?                                   [I3-6]
# TODO
#
# Q3. Trace it, writing seatHolders after each step. Show S1, seats A7, A8.
#     t=0:00  holdSeats(u1, S1, [A7])         -> booking "B1"
#     t=0:11  holdSeats(u2, S1, [A7])         -> booking "B2"
#     t=0:12  cancelBooking("B1")
#     t=0:13  holdSeats(u3, S1, [A7])
#     t=0:14  confirmBooking("B2", "p2") and then confirmBooking("B3", "p3")
#     What does each call return? Is anything wrong? If so, what is the
#     one-line fix?                                                     [I3-7]
# TODO
