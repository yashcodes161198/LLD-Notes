"""
WORKBOOK 03: IMPLEMENTATION (GUIDED)             Rate Limiter
Time box: 12 minutes. Closed book. Pseudo-code is fine; any consistent syntax works.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the class design
    class RateLimiter:
        - limits: Map<Tier, Limit>                      # constructor already checked every Tier has one
        - buckets: ConcurrentMap<string, TokenBucket>
        - clock: Clock                                  # clock.now() -> seconds, as a decimal
        + allowRequest(clientId: string, tier: Tier) -> boolean | error
        - getOrCreateBucket(clientId, tier) -> TokenBucket
    class TokenBucket:
        - limit: Limit   - tokens: number   - lastRefill: timestamp
        + TokenBucket(limit, now)          # starts full: tokens = limit.capacity
        + tryConsume(now) -> boolean
        - refill(now)
    class Limit:  capacity, refillPerSecond   (immutable, both > 0)
    enum Tier: FREE, PREMIUM

The core logic and edge cases are PRE-FILLED as hints. Write the METHOD BODY
under each "# YOUR CODE". Each core-logic step should become one or two lines.
"""

class RateLimiter:

    allowRequest(clientId: string, tier: Tier):
        """
        Core logic:
        1. Validate the client id
        2. Get this client's bucket (create it on first sight)
        3. Ask the bucket to consume one token at the current time
        4. Return its answer

        Edge cases:
        1. clientId null or empty -> throw
        2. over the limit -> return false (think: error or not?)
        """
        # YOUR CODE                                        [I3-1]
        TODO


    private getOrCreateBucket(clientId, tier):
        """
        Core logic:
        1. Look up the limit for this tier
        2. Return the client's bucket, or create a full one and store it

        Edge cases:
        1. two threads see a brand-new client at the same moment -> both must
           get the SAME bucket
        2. remove idle buckets???  <- decide, and explain in a comment
        """
        # YOUR CODE                                        [I3-2]
        TODO


class TokenBucket:

    tryConsume(now):
        """
        Core logic:
        1. Refill for the time since the last refill
        2. If at least one whole token is available, take it and return true
        3. Otherwise return false

        Edge cases:
        1. two threads call this on the same bucket at the same moment
        2. denied request -> consumes nothing
        """
        # YOUR CODE                                        [I3-3]
        TODO


    private refill(now):
        """
        Core logic:
        1. elapsed = time since lastRefill
        2. add elapsed x refillPerSecond tokens, never above capacity
        3. lastRefill = now

        Edge cases:
        1. no time has passed, or the clock went backwards
        2. idle for a long time
        3. calls arrive closer together than one token's worth of time
           <- decide how you store tokens, and explain in a comment
        """
        # YOUR CODE                                        [I3-4]
        TODO


# ---------------------------------------------------------------------------
# Q1. Name the two assumptions you state out loud: one about TIME, one about
#     the MAP in getOrCreateBucket. Why does each matter?        [I3-5]
# TODO
#
# Q2. Suppose tryConsume had no lock. A bucket holds tokens = 1.0 and two
#     threads call it at the same instant. Write the interleaving that breaks
#     Req 5, then say where your lock goes and why it isn't one global lock.
#                                                                [I3-6]
# TODO
#
# Q3. Trace it: Limit(capacity 1, refillPerSecond 1). One client calls at
#     t = 0, 0.5, 1.0, 1.5, 2.0.
#     (a) With YOUR refill: tokens, lastRefill and allow/deny after each call.
#     (b) With this common version instead:
#             tokensToAdd = floor(elapsed * refillPerSecond)
#             tokens = min(capacity, tokens + tokensToAdd)
#             lastRefill = now
#     Same table. Is anything wrong? What would a real client see?  [I3-7]
# TODO
