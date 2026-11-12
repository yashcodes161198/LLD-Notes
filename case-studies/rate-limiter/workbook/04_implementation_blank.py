"""
WORKBOOK 04: IMPLEMENTATION (NO HINTS)           Rate Limiter
Time box: 12 minutes. Closed book. Do this on a DIFFERENT day from workbook 03.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the class design (same as workbook 03)
    class RateLimiter:
        - limits: Map<Tier, Limit>        - buckets: ConcurrentMap<string, TokenBucket>
        - clock: Clock (now() -> seconds)
        + allowRequest(clientId: string, tier: Tier) -> boolean | error
        - getOrCreateBucket(clientId, tier) -> TokenBucket
    class TokenBucket:  limit, tokens, lastRefill | TokenBucket(limit, now) starts full,
                        tryConsume(now) -> boolean, refill(now)
    class Limit:  capacity, refillPerSecond (immutable, both > 0)
    enum Tier: FREE, PREMIUM

For EACH method:
  1. Write the docstring yourself: "Core logic:" (numbered steps) and
     "Edge cases:" (condition -> what happens).
  2. Then write the body so it follows your steps in order.
Add any private helper you need. Add extra state if you need it, and say why.
"""

class RateLimiter:
    # Extra state you added, if any (and why):
    # TODO

    allowRequest(clientId: string, tier: Tier):
        """
        Core logic:
        TODO
        Edge cases:
        TODO
        """
        TODO                                               # [I4-1]


    private getOrCreateBucket(clientId, tier):
        """
        Core logic:
        TODO
        Edge cases:
        TODO
        """
        TODO                                               # [I4-2]


class TokenBucket:

    tryConsume(now):
        """
        Core logic:
        TODO
        Edge cases:
        TODO
        """
        TODO                                               # [I4-3]


    private refill(now):
        """
        Core logic:
        TODO
        Edge cases:
        TODO
        """
        TODO                                               # [I4-4]


# ---------------------------------------------------------------------------
# Q1. Your buckets map never shrinks. Write the 2-3 sentence trade-off you'd
#     raise with the interviewer about that.                  [I4-5]
# TODO
#
# Q2. A FREE client (1 token/second) has emptied its bucket and now sends
#     exactly one request every 0.5 seconds, forever. Prove in one or two
#     lines that your refill still lets about one request per second through.
#                                                             [I4-6]
# TODO
#
# Q3. Write a 4-line verification trace (calls, then the resulting state) that
#     covers one happy path and one error.                    [I4-7]
# TODO
