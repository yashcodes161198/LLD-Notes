"""
WORKBOOK 04: IMPLEMENTATION (NO HINTS)           Amazon Locker
Time box: 12 minutes. Closed book. Do this on a DIFFERENT day from workbook 03.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the class design (same as workbook 03)
    class Locker:
        - compartments: Compartment[]
        - accessTokensMapping: Map<string, AccessToken>
        + depositPackage(size) -> string | error
        + pickup(code: string) -> void | error
        + openExpiredCompartments() -> void
    class AccessToken:  code, expiration, compartment | isExpired(), getCompartment(), getCode()
    class Compartment:  size, occupied | isOccupied(), markOccupied(), markFree(), open(), getSize()

For EACH method:
  1. Write the docstring yourself: "Core logic:" (numbered steps) and
     "Edge cases:" (condition -> what happens).
  2. Then write the body so it follows your steps in order.
Add any private helper you need. Add extra state to Locker if you need it,
and say why.
"""

class Locker:
    # Extra state you added, if any (and why):
    # TODO

    depositPackage(size):
        """
        Core logic:
        TODO
        Edge cases:
        TODO
        """
        TODO                                               # [I4-1]


    # helper(s) TODO                                       # [I4-2]


    pickup(code: string):
        """
        Core logic:
        TODO
        Edge cases:
        TODO
        """
        TODO                                               # [I4-3]


    openExpiredCompartments():
        """
        Core logic:
        TODO
        Edge cases:
        TODO
        """
        TODO                                               # [I4-4]


# ---------------------------------------------------------------------------
# Q1. Write the 2-3 sentence product trade-off you'd raise with the
#     interviewer about expired tokens and the map.           [I4-5]
# TODO
#
# Q2. Is your openExpiredCompartments() safe to run twice, a day apart, with
#     a fresh deposit in between? Prove it in one line.       [I4-6]
# TODO
#
# Q3. Write a 4-line verification trace (calls, then the resulting state) that
#     covers one happy path and one error.                    [I4-7]
# TODO
