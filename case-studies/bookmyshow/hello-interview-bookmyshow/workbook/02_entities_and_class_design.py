"""
WORKBOOK 02: ENTITIES + CLASS DESIGN             BookMyShow (seat booking)
Time box: 15 minutes. Closed book.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the agreed requirements (don't change them)
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

Out of scope: search/browse; payment processing and refunds; notifications;
pricing; theatre/screen/show administration; user accounts; multiple servers.
"""

# ===========================================================================
# PART A: ENTITIES                                         [E-items]
# ===========================================================================

# A1 [E1]. List EVERY noun in the requirements (and the prompt) that could be a class (at least 7).
# - TODO

# A2 [E2]. For each noun: keep or drop, and why (one line each).
#     Format:  <Noun>: KEEP/DROP: <reason>
# - TODO

# A3 [E3]. One external thing is DROPPED as a class, but one of its values
#     survives. Which thing, which value, and where does it go instead?
# TODO

# A4 [E4]. One kept entity "looks like it's just a list of seat ids". Why is it
#     still a class? And why is there no separate Hold class?
# TODO

# A5 [E5]. Any enums?
# TODO


# ===========================================================================
# PART B: CLASS DESIGN                                     [C-items]
#   Notation:   - field: Type        + method(params) -> ReturnType
#   Go TOP-DOWN (start with the orchestrator). Next to each member, add a
#   comment naming the requirement it comes from, e.g.  # Req 3      [C5]
# ===========================================================================

# [C1] class TODO_Orchestrator:
#     - TODO
#     + TODO


# [C2] class TODO:
#     - TODO
#     + TODO


# [C3] class TODO:
#     - TODO
#     + TODO


# [C4] class TODO:
#     - TODO
#     + TODO


# ===========================================================================
# PART C: DESIGN DECISIONS (answer like you're talking to the interviewer)  [D-items]
# ===========================================================================

# C1 [D1]. Where does "is seat A7 taken?" live: on Seat, or somewhere else?
#     Why? Name the principle.
# TODO

# C2 [D2]. What exactly do you store to answer "is A7 taken?", and how does a
#     hold expiring free the seat WITHOUT any timer running?
# TODO

# C3 [D3]. In Amazon Locker, deriving "occupied" from token validity was WRONG.
#     Here you derive "taken" from the booking. Why is it right here?
# TODO

# C4 [D4]. Which lock(s) do you use, and why that granularity rather than one
#     global lock or one lock per seat?
# TODO

# C5 [D5]. Why does the orchestrator take a clock as a dependency?
# TODO
