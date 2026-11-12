"""
WORKBOOK 03: IMPLEMENTATION (GUIDED)             LLM Usage Limits
Time box: 15 minutes. Closed book. Pseudo-code is fine; any consistent syntax works.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the class design
    class UsageLimiter:
        - accounts: Map<userId, UsageAccount>
        - backend: InferenceBackend       # generate(model, messages) -> result(text, inputTokens, outputTokens)
        - clock: Clock
        + handle(request) -> InferenceResult | UsageLimitExceeded
        + getRemaining(userId, limitType) -> long
        - estimateUnits(request) -> long          # given, don't implement
        - actualUnits(model, result) -> long      # given, don't implement
    class UsageAccount:                           # one per user; its methods are synchronized
        - windows: List<UsageWindow>              # [FIVE_HOUR, WEEKLY]
        + tryReserve(units, now) -> UsageWindow | null    # returns the BLOCKING window, or null if reserved
        + settle(reservedUnits, actualUnits, now)
    class UsageWindow:
        - type, duration, limit, used, reserved, resetAt (null = no active window)
        + rollIfExpired(now)  + startIfIdle(now)  + hasRoom()      # hasRoom = used + reserved < limit
        + reserve(u)  + release(u)  + charge(u)  + getResetAt()  + getType()
    class InferenceRequest:  userId, model, messages (validates itself)

The core logic and edge cases are PRE-FILLED as hints. Write the METHOD BODY
under each "# YOUR CODE". Each core-logic step should become one or two lines.
"""

class UsageLimiter:

    handle(request):
        """
        Core logic:
        1. Validate the request
        2. Get (or create) the user's account
        3. Estimate the cost
        4. Reserve the estimate (atomic check + reserve on the account)
        5. Call the inference backend
        6. Compute the actual cost from the real token counts
        7. Settle: release the estimate, charge the actual
        8. Return the result

        Edge cases:
        1. invalid request -> throw "invalid request" (before anything is reserved)
        2. a window is used up -> throw UsageLimitExceeded(type, resetAt); nothing reserved
        3. backend throws -> release the estimate, charge 0, re-throw
        4. actual > estimate and it pushes the user over the limit -> allowed
        """
        # YOUR CODE                                        [I3-1]
        TODO


class UsageAccount:

    synchronized tryReserve(units, now):
        """
        Core logic:
        1. Roll every window that has expired, and start any idle one
        2. Find every window with no room (used + reserved >= limit)
        3. If any: return the one that resets LAST (reserve nothing)
        4. Otherwise reserve the units in EVERY window, return null

        Edge cases:
        1. both windows full -> report the one with the later reset
        2. 5h has room but weekly doesn't -> reserve NOTHING
        3. used + reserved is 999 of 1000 -> admit
        """
        # YOUR CODE                                        [I3-2]
        TODO


    synchronized settle(reservedUnits, actualUnits, now):
        """
        Core logic:
        1. For each window: roll it if expired
        2. Release the reservation
        3. If actual > 0: start the window if idle, then charge the actual

        Edge cases:
        1. actual = 0 (failure) -> release only
        2. the window reset while the request was running
        """
        # YOUR CODE                                        [I3-3]
        TODO


class UsageWindow:

    rollIfExpired(now):
        """
        Core logic:
        1. If there is an active window and now >= resetAt: used = 0, resetAt = null
        2. Decide what happens to "reserved", and explain in a comment

        Edge cases:
        1. no active window -> nothing to do
        2. requests in flight across the reset
        """
        # YOUR CODE                                        [I3-4]
        TODO


# ---------------------------------------------------------------------------
# Q1. Your admission check is "used + reserved < limit". Why not
#     "used + reserved + estimate <= limit"? What do you give up?     [I3-5]
# TODO
#
# Q2. In handle(), which lines run while the account's lock is held, and which
#     line must NOT? What breaks if it does?                          [I3-6]
# TODO
#
# Q3. Trace it. Limit 5h = 1000; u2 has used 600. Three chats send at the same
#     instant; each estimate is 206, each actual is 200.
#     (a) A teammate's version of handle() is:
#             if account.remaining(now) <= 0: throw
#             result = backend.generate(...)
#             account.charge(actual, now)
#         What is u2's "used" after all three finish? With 20 chats?
#     (b) Your version: which chats are admitted, which are rejected, and what
#         are used and reserved at the end?
#     (c) Name the bug in (a) in two words.                           [I3-7]
# TODO
