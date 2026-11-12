"""
WORKBOOK 04: IMPLEMENTATION (NO HINTS)           BookMyShow (seat booking)
Time box: 15 minutes. Closed book. Do this on a DIFFERENT day from workbook 03.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the class design (same as workbook 03)
    HOLD_DURATION = 10 minutes
    class BookingSystem:
        - shows: Map<string, Show>   - bookings: Map<string, Booking>   - clock: Clock
        + getAvailableSeats(showId)  + holdSeats(userId, showId, seatIds[]) -> bookingId
        + confirmBooking(bookingId, paymentRef)  + cancelBooking(bookingId)
    class Show:     id, seats: Map<string, Seat>, seatHolders: Map<string, Booking>, lock
                    | getAvailableSeats(now), hold(booking, now), release(booking)
    class Seat:     id, type (immutable)
    class Booking:  id, userId, show, seatIds, status, expiresAt, paymentRef
                    | isActive(now), isExpired(now), confirm(paymentRef), cancel()
    enum BookingStatus: PENDING, CONFIRMED, CANCELLED
    Two users may call any method at the same time.

For EACH method:
  1. Write the docstring yourself: "Core logic:" (numbered steps) and
     "Edge cases:" (condition -> what happens).
  2. Then write the body so it follows your steps in order.
Add any private helper you need.
"""

class BookingSystem:

    holdSeats(userId, showId, seatIds):
        """
        Core logic:
        TODO
        Edge cases:
        TODO
        """
        TODO                                               # [I4-1]


class Show:

    hold(booking, now):
        """
        Core logic:
        TODO
        Edge cases:
        TODO
        """
        TODO                                               # [I4-2]


class BookingSystem:

    confirmBooking(bookingId, paymentRef):
        """
        Core logic:
        TODO
        Edge cases:
        TODO
        """
        TODO                                               # [I4-3]


    cancelBooking(bookingId):                              # plus Show.release
        """
        Core logic:
        TODO
        Edge cases:
        TODO
        """
        TODO                                               # [I4-4]


# ---------------------------------------------------------------------------
# Q1. Write the 2-3 sentence product trade-off you'd raise with the
#     interviewer about confirming a booking that is already CONFIRMED,
#     and about payment arriving just after the hold expired.  [I4-5]
# TODO
#
# Q2. Prove in 1-2 lines that your cancelBooking can never free a seat that
#     another, newer booking holds, even when the cancelled booking expired
#     long ago and its seats were re-held.                       [I4-6]
# TODO
#
# Q3. Write a 4-line verification trace (calls, then the resulting state) that
#     covers one happy path, one error, and one race.            [I4-7]
# TODO
