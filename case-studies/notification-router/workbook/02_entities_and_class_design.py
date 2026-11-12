"""
WORKBOOK 02: ENTITIES + CLASS DESIGN             Notification Router
Time box: 15 minutes. Closed book.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the agreed requirements (don't change them)
1. A user sets or updates their single preferred channel (EMAIL, SMS, PUSH)
   - The newest value replaces the old one
   - Missing user id or channel -> error
2. A calling service submits a notification (id, recipient user id, priority, content)
   - Invalid notification (missing id, user or priority) -> error
3. NORMAL notifications go only to the user's preferred channel
   - No preference on file -> specific error "no preferred channel"
4. URGENT notifications go to every channel, exactly once each
   - Works even when the user has no preference
5. The router hands each (notification, channel) pair to that channel's handler
   - One handler failing does not stop the others
   - The router returns a per-channel result (SENT / FAILED)
   - Every channel must have a handler when the router is created, else error

Out of scope: handler implementations (SMTP, SMS gateway, push); contact lookup;
retries, queues, rate limits, opt-outs; templates.
"""

# ===========================================================================
# PART A: ENTITIES                                         [E-items]
# ===========================================================================

# A1 [E1]. List EVERY noun in the requirements that could be a class (at least 6).
# - TODO

# A2 [E2]. For each noun: keep, drop, or enum, and why (one line each).
#     Format:  <Noun>: KEEP/DROP/ENUM: <reason>
# - TODO

# A3 [E3]. One noun is DROPPED as a class, but one fact about it survives.
#     Which noun, which fact, and where does that fact live instead?
# TODO

# A4 [E4]. Handlers are out of scope, yet something handler-shaped belongs in
#     the design. What is it, and why is it in scope?
# TODO

# A5 [E5]. Which enums do you need?
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


# [C3] interface TODO:
#     + TODO


# ===========================================================================
# PART C: DESIGN DECISIONS (answer like you're talking to the interviewer)  [D-items]
# ===========================================================================

# C1 [D1]. Where does "user u1 prefers SMS" live, and why there? Name the rule.
# TODO

# C2 [D2]. Why split route() into a "decide" step and a "send" step? Give two
#     concrete benefits.
# TODO

# C3 [D3]. What collection type does the "decide" step return, and which
#     requirement makes that choice matter?
# TODO

# C4 [D4]. Name one way of calling the senders that you REJECT, and why.
# TODO

# C5 [D5]. Which methods will you NOT implement in the next step, and why?
# TODO
