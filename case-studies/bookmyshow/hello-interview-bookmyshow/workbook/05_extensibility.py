"""
WORKBOOK 05: EXTENSIBILITY                       BookMyShow (seat booking)
Time box: 3 minutes per question (~20 min total). Closed book.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the finished base design.
    BookingSystem (shows, bookings, clock) with holdSeats / confirmBooking /
    cancelBooking / getAvailableSeats. Show owns seats, seatHolders
    (seatId -> Booking) and one lock; hold() checks all seats then writes all
    under the lock; release() removes a seat only if this booking still holds
    it. Booking has status PENDING/CONFIRMED/CANCELLED and expiresAt; a seat is
    taken iff its holder isActive(now) (lazy expiry, no timer).

For EACH question, fill in all four parts:
  Pattern:  the reusable design move (name it)
  Lands in: exactly which class(es) and method(s) change; everything else stays the same
  Say:      what you'd say out loud, 2-3 sentences
  Code:     the minimal code delta (pseudo-code). Write "describe only" if you'd
            only talk it through, but you still need the Pattern and Lands-in.
"""

# X1. "Premium seats cost more, and prices may change at peak hours.
#      How do you add pricing?"
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X2. "When a hold expires or someone cancels, notify users waiting for that show."
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X3. "Reuse this for meeting rooms or hotel rooms, where people book a time range."
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X4. "We now run 20 servers behind a load balancer. Does your lock still work?"
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X5. "A blockbuster opens and 50k users hit one show. Is the per-show lock a
#      bottleneck?"
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X6. "Bots hold hundreds of seats and never pay. How do you stop that?"
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO
