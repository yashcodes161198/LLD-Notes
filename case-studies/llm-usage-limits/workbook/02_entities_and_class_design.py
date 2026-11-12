"""
WORKBOOK 02: ENTITIES + CLASS DESIGN             LLM Usage Limits
Time box: 15 minutes. Closed book.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the agreed requirements (don't change them)
1. A user sends a request with userId, model and messages
   - System runs it on the inference backend and returns the result if the user has usage left
   - Throws "invalid request" if userId, model or messages are missing
2. Every request is charged in usage units = (input + output tokens) x model weight
   - Charged against BOTH the 5-hour window and the weekly window
3. The cost is unknown before the request runs
   - System reserves an estimate when it admits the request (input tokens counted + a typical reply size, x weight)
   - When the request finishes it releases the estimate and charges the actual cost
   - If inference fails, it releases the estimate and charges nothing
4. Each window starts at the user's first request when no window is active and resets after its duration (5 hours / 7 days)
   - Each window has its own limit
   - If either window is used up, the request is rejected with an error naming the limit and its reset time
5. A user may have many requests in flight at once (parallel chats)
   - Admission counts what is used plus what is reserved by in-flight requests
   - The user is blocked only when (used + reserved) has reached a limit
   - An admitted request always completes and is charged in full, even past the limit
6. A user can check their remaining usage for each window

Out of scope: running the model (external backend); plans/billing; auth; streaming;
exact tokenization; multiple servers / persistence.
Weights: HAIKU 1, SONNET 2, OPUS 5.
"""

# ===========================================================================
# PART A: ENTITIES                                         [E-items]
# ===========================================================================

# A1 [E1]. List EVERY noun in the requirements that could be a class (at least 8).
# - TODO

# A2 [E2]. For each noun: keep / drop / enum / seam, and why (one line each).
#     Format:  <Noun>: KEEP/DROP/ENUM/SEAM: <reason>
# - TODO

# A3 [E3]. "Parallel chats" is in the requirements. Is Chat a class? Why or why not?
# TODO

# A4 [E4]. One kept entity "looks like it's just a number". Which, and why is it
#     still a class?
# TODO

# A5 [E5]. Any enums? With their values.
# TODO


# ===========================================================================
# PART B: CLASS DESIGN                                     [C-items]
#   Notation:   - field: Type        + method(params) -> ReturnType
#   Go TOP-DOWN (start with the orchestrator). Next to each member, add a
#   comment naming the requirement it comes from, e.g.  # Req 3      [C5]
#   The inference backend is an external interface; you may just name it.
# ===========================================================================

# [C1] class TODO_Orchestrator:
#     - TODO
#     + TODO


# [C2] class TODO:            (one per user)
#     - TODO
#     + TODO


# [C3] class TODO:            (one per limit)
#     - TODO
#     + TODO


# [C4] class TODO:            (the input)
#     - TODO


# ===========================================================================
# PART C: DESIGN DECISIONS (answer like you're talking to the interviewer)  [D-items]
# ===========================================================================

# C1 [D1]. Where does the lock live, and what does it cover? What must it NOT cover?
# TODO

# C2 [D2]. Who owns the "has this window expired, reset it" rule, and why?
# TODO

# C3 [D3]. Name one alternative way to track usage over 5 hours and why you
#     reject it.
# TODO

# C4 [D4]. Why is the clock injected instead of calling now() directly?
# TODO

# C5 [D5]. Two named fields (fiveHour, weekly) or a list of windows? Pick one and
#     justify it in one sentence.
# TODO
