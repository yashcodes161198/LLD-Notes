"""
WORKBOOK 02: ENTITIES + CLASS DESIGN             Rate Limiter
Time box: 15 minutes. Closed book.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the agreed requirements (don't change them)
1. The API gateway asks whether a request from a client may go through, giving the client id and tier
   - Returns true (allowed) or false (denied); the gateway sends 429 on false
   - Throws an error if the client id is missing or empty
2. Each tier has a limit: a burst capacity and a refill rate
   - FREE: capacity 10, 1 per second. PREMIUM: capacity 100, 10 per second
3. A client may burst up to its capacity, then is held to the refill rate on average
   - Capacity refills continuously over time and never goes above the tier's capacity
   - Denied requests do not use up capacity
4. Limits are per client
   - One client's traffic never affects another's
   - A client seen for the first time starts with its full burst available
5. The limiter is called concurrently by many request threads
   - It never allows more requests than the limit, even when calls race

Out of scope: many API servers; building the 429 response and headers; how the
tier is determined; per-endpoint limits; persistence across restarts.
"""

# ===========================================================================
# PART A: ENTITIES                                         [E-items]
# ===========================================================================

# A1 [E1]. List EVERY noun in the requirements (or the algorithm you'd use)
#     that could be a class (at least 6).
# - TODO

# A2 [E2]. For each noun: keep or drop, and why (one line each).
#     Format:  <Noun>: KEEP/DROP: <reason>
# - TODO

# A3 [E3]. Some nouns are DROPPED as classes, but what we need from them survives.
#     Which nouns, what survives, and where does it go instead?
# TODO

# A4 [E4]. One kept entity "looks like it's just a number in a map".
#     Which one, and why is it still a class?
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

# C1 [D1]. Which collection holds per-client state, and what's the REAL reason?
#     Is it the same reason as in Amazon Locker?
# TODO

# C2 [D2]. Where do the token count and last-refill time live? Name the principle,
#     and one extra benefit for concurrency.
# TODO

# C3 [D3]. Name one alternative way to refill tokens and why you reject it.
# TODO

# C4 [D4]. Name one algorithm that is actually WRONG for these requirements,
#     and the case that breaks it.
# TODO

# C5 [D5]. Which methods will you NOT implement in the next step, and why?
# TODO
