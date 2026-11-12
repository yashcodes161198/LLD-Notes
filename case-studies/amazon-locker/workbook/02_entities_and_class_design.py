"""
WORKBOOK 02: ENTITIES + CLASS DESIGN             Amazon Locker
Time box: 15 minutes. Closed book.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the agreed requirements (don't change them)
1. Carrier deposits a package by specifying size (small, medium, large)
   - System assigns an available compartment of matching size
   - Opens compartment and returns access token, or error if no space
2. Upon successful deposit, an access token is generated and returned
   - One access token per package
3. User retrieves package by entering access token
   - System validates code and opens compartment
   - Throws specific error if code is invalid or expired
4. Access tokens expire after 7 days
   - Expired codes are rejected if used for pickup
   - Package remains in compartment until staff removes it
5. Staff can open all expired compartments to manually handle packages
   - System opens all compartments with expired tokens
   - Staff physically removes packages and returns them to sender

Out of scope: delivery logistics; how the code reaches the customer (SMS/email).
"""

# ===========================================================================
# PART A: ENTITIES                                         [E-items]
# ===========================================================================

# A1 [E1]. List EVERY noun in the requirements that could be a class (at least 6).
# - TODO

# A2 [E2]. For each noun: keep or drop, and why (one line each).
#     Format:  <Noun>: KEEP/DROP: <reason>
# - TODO

# A3 [E3]. One noun is DROPPED as a class, but one of its attributes survives.
#     Which noun, which attribute, and where does the attribute go instead?
# TODO

# A4 [E4]. One kept entity "looks like it's just a string". Why is it still a class?
# TODO

# A5 [E5]. Any enums?
# TODO


# ===========================================================================
# PART B: CLASS DESIGN                                     [C-items]
#   Notation:   - field: Type        + method(params) -> ReturnType
#   Go TOP-DOWN (start with the orchestrator). Next to each member, add a
#   comment naming the requirement it comes from, e.g.  # Req 3      [C4]
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


# ===========================================================================
# PART C: DESIGN DECISIONS (answer like you're talking to the interviewer)  [D-items]
# ===========================================================================

# C1 [D1]. Which collection do you use to store issued tokens, and what's the REAL
#     reason? (Hint: the answer isn't only Big-O.)
# TODO

# C2 [D2]. Where does "is this compartment occupied?" live: on the orchestrator or
#     on the compartment? Name the principle and the general rule.
# TODO

# C3 [D3]. Name one alternative way to track free compartments and why you reject it.
# TODO

# C4 [D4]. Name one approach for deciding occupancy that is actually WRONG, and the
#     case that breaks it.
# TODO

# C5 [D5]. Which methods will you NOT implement in the next step, and why?
# TODO
