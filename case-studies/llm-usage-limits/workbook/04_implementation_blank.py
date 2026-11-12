"""
WORKBOOK 04: IMPLEMENTATION (NO HINTS)           LLM Usage Limits
Time box: 15 minutes. Closed book. Do this on a DIFFERENT day from workbook 03.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the class design (same as workbook 03)
    class UsageLimiter:  accounts: Map<userId, UsageAccount>, backend, clock
                         + handle(request) -> InferenceResult | UsageLimitExceeded
                         - estimateUnits(request), - actualUnits(model, result)   (given)
    class UsageAccount:  windows: List<UsageWindow>   (methods synchronized)
                         + tryReserve(units, now) -> UsageWindow | null
                         + settle(reservedUnits, actualUnits, now)
    class UsageWindow:   type, duration, limit, used, reserved, resetAt
                         + rollIfExpired(now) + startIfIdle(now) + hasRoom()
                         + reserve(u) + release(u) + charge(u) + getResetAt() + getType()
    backend.generate(model, messages) -> result(text, inputTokens, outputTokens)

For EACH method:
  1. Write the docstring yourself: "Core logic:" (numbered steps) and
     "Edge cases:" (condition -> what happens).
  2. Then write the body so it follows your steps in order.
"""

class UsageLimiter:

    handle(request):
        """
        Core logic:
        TODO
        Edge cases:
        TODO
        """
        TODO                                               # [I4-1]


class UsageAccount:

    synchronized tryReserve(units, now):
        """
        Core logic:
        TODO
        Edge cases:
        TODO
        """
        TODO                                               # [I4-2]


    synchronized settle(reservedUnits, actualUnits, now):
        """
        Core logic:
        TODO
        Edge cases:
        TODO
        """
        TODO                                               # [I4-3]


class UsageWindow:

    rollIfExpired(now):
        """
        Core logic:
        TODO
        Edge cases:
        TODO
        """
        TODO                                               # [I4-4]


# ---------------------------------------------------------------------------
# Q1. Write the 2-3 sentence product trade-off you'd raise with the
#     interviewer about how big the reserved estimate should be.      [I4-5]
# TODO
#
# Q2. Prove two safety properties, one line each:
#     (a) 20 chats sent at the same instant can't all get in.
#     (b) a backend timeout never leaves the user with less usage than before.
#                                                                     [I4-6]
# TODO
#
# Q3. Write a 4-line verification trace (calls, then the resulting state) that
#     covers one happy path, one limit error, and one window reset.   [I4-7]
# TODO
