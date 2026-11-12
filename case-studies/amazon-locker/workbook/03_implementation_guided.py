"""
WORKBOOK 03: IMPLEMENTATION (GUIDED)             Amazon Locker
Time box: 12 minutes. Closed book. Pseudo-code is fine; any consistent syntax works.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the class design
    class Locker:
        - compartments: Compartment[]
        - accessTokensMapping: Map<string, AccessToken>
        + depositPackage(size) -> string | error
        + pickup(code: string) -> void | error
        + openExpiredCompartments() -> void
    class AccessToken:
        - code: string   - expiration: timestamp   - compartment: Compartment
        + isExpired() -> boolean   + getCompartment() -> Compartment   + getCode() -> string
    class Compartment:
        - size: Size   - occupied: boolean
        + isOccupied()  + markOccupied()  + markFree()  + open()  + getSize()
    You may call a private helper  generateAccessToken(compartment) -> AccessToken
    without implementing it.

The core logic and edge cases are PRE-FILLED as hints. Write the METHOD BODY
under each "# YOUR CODE". Each core-logic step should become one or two lines.
"""

class Locker:

    depositPackage(size):
        """
        Core logic:
        1. Find compartment of the right size
        2. Open the compartment
        3. Mark this compartment as occupied
        4. Generate an accessToken
        5. Store the accessToken
        6. Return the access token code

        Edge cases:
        1. No compartment of right size -> throw error
        """
        # YOUR CODE                                        [I3-1]
        TODO


    private getAvailableCompartment(size):
        """
        Core logic:
        1. Scan all compartments
        2. For each, see if right size and free
        3. First that matches, return it

        Edge cases:
        1. none available -> return null
        """
        # YOUR CODE                                        [I3-2]
        TODO


    pickup(code: string):
        """
        Core logic:
        1. Look up the code to get accessToken
        2. Get the compartment
        3. Open the compartment
        4. Mark the compartment as free
        5. Remove access code from the map

        Edge cases:
        1. validation of the code itself, empty -> throw
        2. code is not in mapping -> throw
        3. accessToken is expired -> throw (must be a DIFFERENT message from 1 and 2)
        """
        # YOUR CODE                                        [I3-3]
        TODO


    openExpiredCompartments():
        """
        Core logic:
        1. Scan through all access tokens in mapping
        2. Find the expired ones
        3. Get the compartment of each
        4. Open those compartments
           --- staff remove the packages
        5. Mark the compartment available again
        6. Remove access code from map???  <- decide, and explain in a comment

        Edge cases:
        - empty map / nothing expired / everything expired / called twice
        """
        # YOUR CODE                                        [I3-4]
        TODO


# ---------------------------------------------------------------------------
# Q1. What physical assumption are you making in pickup()? What would you
#     say to the interviewer about it?                        [I3-5]
# TODO
#
# Q2. In pickup(), what does a map lookup return for a missing key, and why
#     do you say that out loud?                               [I3-6]
# TODO
#
# Q3. Trace it: compartments = [S1, M1]. deposit(MEDIUM) on day 0, pickup on
#     day 9, then openExpiredCompartments(), then deposit(MEDIUM) again, then
#     openExpiredCompartments() again. What happens to each compartment
#     at each step? Is anything wrong?                        [I3-7]
# TODO
