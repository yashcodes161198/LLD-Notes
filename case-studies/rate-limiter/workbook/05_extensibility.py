"""
WORKBOOK 05: EXTENSIBILITY                       Rate Limiter
Time box: 3 minutes per question (~20 min total). Closed book.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the finished base design. RateLimiter (limits: Map<Tier, Limit>,
buckets: ConcurrentMap<clientId, TokenBucket>, clock) with
allowRequest(clientId, tier) -> boolean, which validates the id, gets the
bucket with buckets.computeIfAbsent(...) in a private getOrCreateBucket, and
returns bucket.tryConsume(clock.now()). TokenBucket (limit, decimal tokens,
lastRefill) has a synchronized tryConsume(now) that refills lazily
(min(capacity, tokens + elapsed * rate)) and then takes one token if
tokens >= 1. Limit = (capacity, refillPerSecond). Tier = FREE | PREMIUM.

For EACH question, fill in all four parts:
  Pattern:  the reusable design move (name it)
  Lands in: exactly which class(es) and method(s) change; everything else stays the same
  Say:      what you'd say out loud, 2-3 sentences
  Code:     the minimal code delta (pseudo-code). Write "describe only" if you'd
            only talk it through, but you still need the Pattern and Lands-in.
"""

# X1. "Another team wants a sliding-window limit instead of a token bucket.
#      How do you support both?"
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X2. "Login should be limited much harder than search. How do you add
#      per-endpoint limits?"
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X3. "Clients want to know WHEN they can retry. How do you return that?"
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X4. "Attackers rotate through thousands of API keys, or send no key at all.
#      How do you stop that?"
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X5. "A client upgrades from FREE to PREMIUM while its bucket is in memory.
#      What happens, and what do you change?"
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X6. "We now run 50 API servers behind a load balancer. What changes?"
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO
