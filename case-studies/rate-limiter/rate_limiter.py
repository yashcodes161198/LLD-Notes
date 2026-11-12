"""
================================================================================
 RATE LIMITER: annotated interview solution
 Source: designed with the Hello Interview delivery framework (Evan King's
         "Design Amazon Locker" method). No single source video; the design is
         original. Frames from the older study notes in this folder are reused
         where they help (📸 04, 05, 11, 13, 14).
================================================================================

This is the file as a candidate would type it in the editor, with notes added.
Level: senior. Language: pseudo-code.

LEGEND (search for these):
    # 💬 SAY:      a talking point to say out loud in the interview, near-verbatim
    # ⚠️ TRICKY:   where candidates lose points, or a subtle bug
    # ✅ BEST:     the strongest approach, and why it beats the alternatives
    # 🎯 GRADED:   which rubric dimension this moment is scored on
    # ⏱            time budget for the step
    # 📸           matching screenshot in ./screenshots/

THE 5-STEP DELIVERY FRAMEWORK (📸 00-delivery-framework.png)
    1. Requirements      ⏱ ~5 min
    2. Entities          ⏱ ~3 min
    3. Class design      ⏱ ~10-15 min
    4. Implementation    ⏱ ~10 min
    5. Extensibility     ⏱ ~5 min (senior+; juniors may never get here)

💬 SAY (at the start): "A rate limiter can mean a lot of things, from one
   in-process class to a global service. Before I design anything I'd like a few
   minutes on requirements: what it must do, what happens on failure, and where
   its boundaries are."
================================================================================


--------------------------------------------------------------------------------
STEP 1: REQUIREMENTS                                  ⏱ ~5 min   📸 04
--------------------------------------------------------------------------------

Design a rate limiter that controls how many requests each client can make to our API.

# ⚠️ TRICKY: "rate limiter" sounds like an algorithm question, so candidates
#    jump straight to "I'll use a token bucket" and start coding. The algorithm
#    is ONE decision inside the design, and you can't pick it until you know
#    whether bursts are allowed.
# 🎯 GRADED: Problem analysis, i.e. can you turn a vague prompt into clear,
#    agreed requirements?

1. primary capabilities
2. error handling
3. scope boundaries
# ✅ BEST: write the 3 bucket names first, then fill them in.

Primary capabilities:

- What do we limit BY: user, API key, IP address?
      -> Interviewer: by client id (the API key). The gateway passes it to us as a string.
- Is the limit the same for everyone?
      -> No. Two tiers: FREE = burst of 10, then 1 request/second.
         PREMIUM = burst of 100, then 10 requests/second.
- Should a client be allowed to BURST, or must requests be evenly spaced?
      -> Short bursts up to the tier's burst size are fine; over time they must
         average out to the tier's rate.
# ✅ BEST: the burst question is the one that picks the algorithm. "Burst up to
#    N, then an average rate" is exactly a token bucket (capacity = burst,
#    refill rate = average rate). Ask it; don't assume it.
- What does the caller get back?
      -> Just allowed / denied. The gateway turns "denied" into an HTTP 429.

Error handling:

- What happens when a client is over its limit?
      -> Deny the request. It is NOT an error; it's a normal answer.
- Does a denied request count against the limit?
      -> No. Only allowed requests use up capacity.
- Missing or empty client id?
      -> Programming error in the caller: throw.
- Will many request threads call the limiter at the same time, including for the same client?
      -> Yes. It runs inside one API server process with a thread per request.
         The limit must hold EXACTLY, even under concurrency.
# 💬 SAY: "Concurrency is the failure mode I most want pinned down, because if
#    two threads can both take the last token, the limit is just a suggestion."

Scope boundaries:

- One API server or many? Does the limit have to be global across servers?
      -> One process for now. Multi-server is a follow-up.
- Who decides a client's tier?
      -> The account service. The gateway already knows it and passes it in.


Requirements:
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

# ✅ BEST: each requirement = ACTOR + ACTION + INPUT, with sub-bullets for the
#    system's reaction and its error. Req 1 becomes the one public method; Reqs
#    2-5 become state and rules on the classes below it.
# ⚠️ TRICKY: Req 3's wording ("burst up to capacity, THEN the average rate")
#    rules out the simplest algorithm, a fixed-window counter. See Step 3, D4.
# ⚠️ TRICKY: "denied" is a RETURN VALUE, not an exception. Being rate-limited
#    is the expected, common outcome for a busy client; exceptions are for
#    caller bugs (an empty id).

Out of scope:
- Limiting across many API servers (one process only)
- Building the HTTP 429 response and headers (Retry-After, X-RateLimit-Remaining)
- How the client's tier is determined (account service)
- Different limits per endpoint
- Keeping limiter state across restarts
# 💬 SAY: "Everything not on this list I'll treat as out of scope. Several of
#    these are natural follow-ups, so I'll keep the design open to them."


--------------------------------------------------------------------------------
STEP 2: ENTITIES                                      ⏱ ~3 min
--------------------------------------------------------------------------------
# ✅ BEST: write down EVERY noun from the requirements, then prune each one out
#    loud. The pruning is where the signal is.
#
#   Noun             Keep?  Why
#   -------------    -----  -----------------------------------------------------
#   API gateway       NO    External caller. It calls our API; we store nothing about it.
#   Client            NO    We hold no profile for it. Its id is a string KEY, and its
#                           tier is owned by the account service, so both become
#                           PARAMETERS of the check method.
#   Request           NO    ⚠️ TRICKY: "most candidates add a Request class" with url,
#                           headers, timestamp. The gateway owns the request. All we
#                           need from it is "who sent it", i.e. the client id parameter.
#   Token             NO    ⚠️ TRICKY: the opposite trap. "Token" is a noun, but here it
#                           is just a NUMBER inside a bucket: no identity, no rules of
#                           its own. (Contrast Amazon Locker's AccessToken, which had
#                           an expiry and a compartment.)
#   Thread            NO    How we're called, not something we model.
#   Rate limiter      YES   The orchestrator / entry point. Finds the client's state,
#                           asks it for a decision.
#   Bucket            YES   ⚠️ TRICKY: "isn't it just an int in a Map<clientId, int>?"
#                           No. A plain counter can't REFILL: it needs the time it was
#                           last refilled, a cap, and a consume that is atomic. State +
#                           its own rules = an entity.
#   Limit             YES   A small immutable value: capacity + refill rate, one per
#                           tier. Validated once, shared by every bucket of that tier.
#   Tier              enum  FREE, PREMIUM.
#
# 💬 SAY: "I'll cut anything we don't hold state or rules for. Client and Request
#    look important, but another system owns them; I only need the client id and
#    tier, so they're parameters. If that changes, I'll come back and add them."
# 🎯 GRADED: Class design (the right entities, and not too many).

Entities:
- RateLimiter
- TokenBucket
- Limit
- (enum) Tier: FREE, PREMIUM


--------------------------------------------------------------------------------
STEP 3: CLASS DESIGN, TOP-DOWN                        ⏱ ~10-15 min  📸 05, 13
--------------------------------------------------------------------------------
# ✅ BEST: start with the ORCHESTRATOR and work down. Its one public method
#    tells you what the bucket must provide.
#    For each class, scan the requirements twice:
#       state    = "what must I REMEMBER to enforce the requirements?"
#       behavior = "what ACTION needs this?"  -> public methods
# (📸 05 is the class diagram from the older video notes in this folder: it has
#  an abstract limiter per algorithm plus a factory and a service from the
#  start. Compare it with the leaner design below; that video's pattern shows up
#  here only as Extension 1, when it's asked for.)
"""

class RateLimiter:
    - limits: Map<Tier, Limit>                  # Req 2: one limit per tier
    - buckets: ConcurrentMap<string, TokenBucket> # Req 4: one bucket per client id
    - clock: Clock                              # Req 3: refill depends on time (injected)

    + RateLimiter(limits, clock)                # throws if any Tier has no Limit
    + allowRequest(clientId: string, tier: Tier) -> boolean | error   # Req 1
    - getOrCreateBucket(clientId, tier) -> TokenBucket                # Req 4 (+ Req 5: atomic)

    # ✅ BEST: Map<clientId, TokenBucket>. Unlike Amazon Locker (50 compartments,
    #    where the map was for clarity), here Big-O REALLY matters: there can be
    #    millions of clients and this lookup is on the hot path of every API
    #    request. Say both reasons.
    # 💬 SAY: "This map is hit on every request, so O(1) lookup matters here,
    #    and it's a concurrent map because request threads share it."
    # ✅ BEST: inject the clock. It's one method, now(), but it makes the
    #    verification trace and unit tests deterministic. Not a domain entity.


class TokenBucket:
    - limit: Limit                  # Req 2: capacity + refill rate for this client's tier
    - tokens: number (fractional)   # Req 3: what's available right now
    - lastRefill: timestamp         # Req 3: refill is computed from elapsed time

    + TokenBucket(limit, now)       # Req 4: starts FULL (tokens = capacity)
    + tryConsume(now) -> boolean    # Req 1 + 3, and Req 5: one atomic step (locked per bucket)
    - refill(now) -> void           # Req 3


class Limit:                        # immutable value object
    - capacity: number              # max burst
    - refillPerSecond: number       # average rate
    + Limit(capacity, refillPerSecond)   # throws unless both > 0

enum Tier:
    FREE
    PREMIUM

    # ✅ BEST (Information Expert): TOKENS AND lastRefill LIVE ON THE BUCKET.
    # 💬 SAY: "The token count and the last refill time are one client's state,
    #    and they always change together, so they live in one object. That also
    #    gives me exactly one thing to lock per client."
    # ⚠️ TRICKY (alternatives):
    #    BAD:   two parallel maps on RateLimiter, tokensByClient and
    #           lastRefillByClient. They can drift apart, and there's no single
    #           object to lock, so a refill and a consume can interleave.
    #    GOOD:  a background thread that adds tokens to every bucket every tick.
    #           Rejected: O(all clients) work per tick, mostly for idle clients,
    #           and the timer drifts.
    #    GREAT: LAZY refill: compute what the bucket WOULD have gained since
    #           lastRefill, at the moment a request arrives. O(1), exact, no thread.
    # ⚠️ TRICKY (a WRONG algorithm for these requirements): the fixed-window
    #    counter (📸 11). "10 per minute, reset at :00" lets 10 through at 0:59 and
    #    10 more at 1:00: 20 in two seconds, twice the agreed burst (Req 3).
    # 🎯 GRADED: Class design (clear ownership and boundaries).


"""
--------------------------------------------------------------------------------
STEP 4: IMPLEMENTATION                                ⏱ ~10 min   📸 13, 14
--------------------------------------------------------------------------------
# 💬 SAY: "Pseudo-code or a real language?" Then: "The interesting logic is
#    allowRequest and getOrCreateBucket on the limiter, and tryConsume and
#    refill on the bucket. Limit is a constructor with a check, Tier is an enum,
#    and the clock is a dependency, so I'll implement those four."
#
# ✅ BEST: for EVERY method, write the docstring FIRST:
#        Core logic: numbered steps
#        Edge cases: walk each step asking "what can go wrong here?"
#    Then write the code, one or two lines per step.
# 🎯 GRADED: Code quality plus Communication (the interviewer can follow your plan).
"""

# Implementation
# 1. Define the core logic
# 2. Consider the edge cases

class RateLimiter:
    RateLimiter(limits, clock):
        for tier in Tier.values()
            if limits[tier] == null
                throw Error("missing limit for tier " + tier)
        this.limits = limits
        this.clock = clock
        this.buckets = new ConcurrentMap()
        # ✅ BEST: validate the config ONCE at the boundary, so the lookup in
        #    getOrCreateBucket can never miss at request time. A config typo
        #    fails at startup, not as a 500 on every PREMIUM request.


    allowRequest(clientId: string, tier: Tier) -> boolean:
        """
        Core logic:
        1. Validate the client id
        2. Get this client's bucket (create it on first sight)
        3. Ask the bucket to consume one token at the current time
        4. Return its answer

        Edge cases:
        1. clientId null or empty -> throw "invalid client id"
        2. client never seen before -> bucket starts full (Req 4); handled in step 2
        3. over the limit -> return false. NOT an exception.
        # 💬 SAY while walking the steps: "Step 3 is where every concurrency
        #    question lives, so I'll make it one locked operation on the bucket."
        """
        if clientId == null || clientId.isEmpty()
            throw Error("invalid client id")

        bucket = getOrCreateBucket(clientId, tier)
        return bucket.tryConsume(clock.now())

        # ✅ BEST: read the clock ONCE per request and pass it down. The bucket
        #    never reads time itself, so the trace below is deterministic.


    private getOrCreateBucket(clientId, tier) -> TokenBucket:
        """
        Core logic:
        1. Look up the limit for this tier
        2. Atomically: return the client's bucket, or create a full one and store it

        Edge cases:
        1. tier has no limit -> can't happen; the constructor checked every tier
        2. two threads see a brand-new client at the same moment -> both must get
           the SAME bucket (atomic get-or-create)
        3. remove idle buckets???  decision: not now (see below)
        """

        # ---- AS WRITTEN BY MOST CANDIDATES ----
        bucket = buckets[clientId]
        if bucket == null
            bucket = new TokenBucket(limits[tier], clock.now())
            buckets[clientId] = bucket
        return bucket

        # ⚠️ TRICKY #1 (check-then-act on the map): threads A and B both see
        #    null for a new client, both create a bucket, and B's put overwrites
        #    A's. A consumes from a bucket nobody can see any more, so that
        #    client gets one free extra request. Small, but it's the same bug
        #    class as TRICKY #2, and interviewers look for it.

    # ---- BEST VERSION ----
    private getOrCreateBucket(clientId, tier) -> TokenBucket:
        limit = limits[tier]
        return buckets.computeIfAbsent(clientId, id -> new TokenBucket(limit, clock.now()))
        # 💬 SAY: "In Java, ConcurrentHashMap.computeIfAbsent is atomic per key.
        #    In my pseudo-code I'll assume the map has that operation."

        # 💬 SAY (the product trade-off, the best moment in the interview):
        #    "I never remove buckets. Is that OK? A bucket is three fields, call
        #    it ~100 bytes with map overhead. A million active clients is ~100 MB,
        #    fine for one server. If it grows, there's a neat trick: a bucket
        #    that's been idle longer than capacity / refillPerSecond is FULL
        #    again, which is exactly what a fresh bucket looks like. So evicting
        #    it changes nothing the client can observe. A periodic sweep can drop
        #    those."
        # 🎯 GRADED: Communication (you raise a decision instead of quietly
        #    picking one, and you do the memory maths out loud).


class TokenBucket:
    TokenBucket(limit, now):
        this.limit = limit
        this.tokens = limit.capacity      # Req 4: a new client starts with its full burst
        this.lastRefill = now


    synchronized tryConsume(now) -> boolean:
        """
        Core logic:
        1. Refill for the time since the last refill
        2. If at least one whole token is available, take it and return true
        3. Otherwise return false

        Edge cases:
        1. two threads on the same bucket -> steps 1-3 are ONE critical section
           (the lock is per bucket, so different clients never wait on each other)
        2. denied request -> consumes nothing (Req 3)
        3. tokens between 0 and 1 (e.g. 0.6) -> denied; a request costs a whole token
        """
        refill(now)
        if tokens >= 1
            tokens = tokens - 1
            return true
        return false

        # ⚠️ TRICKY #2 (check-then-act on the count): without the lock, threads A
        #    and B both read tokens = 1, both pass the check, and both subtract:
        #    two requests admitted, tokens = -1. Req 5 is broken. The refill is a
        #    read-modify-write too, so it must be inside the same lock.
        # ✅ BEST: lock per BUCKET, not one global lock on allowRequest. A
        #    global lock makes every client in the system queue behind every
        #    other client; per-bucket locks only serialise one client's own
        #    racing requests, which is exactly the contention that matters.
        #    (Lock-free alternative: compare-and-set on an immutable
        #    (tokens, lastRefill) pair. Mention it; don't code it.)


    private refill(now):
        """
        Core logic:
        1. elapsed = now - lastRefill
        2. tokens = min(capacity, tokens + elapsed * refillPerSecond)
        3. lastRefill = now

        Edge cases:
        1. elapsed <= 0 (two calls in the same instant, or the clock stepped
           back) -> add nothing. Assume a MONOTONIC clock and say so.
        2. idle for an hour -> capped at capacity; idle time is not banked
        3. calls closer together than one token's worth of time -> keep the
           FRACTION.  store tokens as int or decimal???  decision: decimal (see below)
        """

        # ---- AS WRITTEN BY MOST CANDIDATES ----
        elapsed = now - lastRefill
        tokensToAdd = floor(elapsed * limit.refillPerSecond)    # whole tokens only
        tokens = min(limit.capacity, tokens + tokensToAdd)
        lastRefill = now

        # ⚠️ TRICKY #3 (the real bug; spotting it is a SENIOR signal):
        #    floor() throws away the fraction, AND lastRefill = now throws away
        #    the time that fraction came from. If a client calls more often than
        #    once per token-interval, every call adds 0 and resets the clock, so
        #    the bucket NEVER refills.
        #    FREE (1 token/s), bucket empty, client polls every 0.5 s:
        #        t=10.0  elapsed 0.5 -> floor(0.5) = 0, lastRefill = 10.0 -> deny
        #        t=10.5  elapsed 0.5 -> 0 again,         lastRefill = 10.5 -> deny
        #        ... denied forever, although 2 req/s for 1 req/s allowed should
        #        let every second request through.
        #    The client is STARVED: the most active clients (pollers, retry
        #    loops) are exactly the ones that hit it, and the tests usually
        #    don't, because tests tend to sleep in whole seconds.
        #    (A milder form is in this folder's phases/Phase-5-Thread-Safety,
        #    TokenBucketRateLimiter.refillTokens, 📸 14: it only moves lastRefill
        #    when it added a token, so it doesn't starve, but it still drops the
        #    leftover fraction on every refill, so clients get slightly less than
        #    their rate.)
        #
        # ✅ BEST FIX: keep the fraction. tokens is a decimal number, and a
        #    request needs tokens >= 1. (Equivalent fix if tokens must stay an
        #    int: advance lastRefill only by tokensToAdd / refillPerSecond, so the
        #    leftover time carries over.)

    # ---- BEST VERSION ----
    private refill(now):
        elapsed = now - lastRefill
        if elapsed <= 0
            return
        tokens = min(limit.capacity, tokens + elapsed * limit.refillPerSecond)   # keeps the fraction
        lastRefill = now
        # 💬 SAY: "tokens is a decimal on purpose. If I floored it and reset
        #    lastRefill, a client calling every half second would never get a
        #    token back. Keeping the fraction means the time is never lost."
        # ⚠️ TRICKY: the min() is what stops an idle client banking an hour of
        #    tokens and then firing 3,600 requests at once. Cap BEFORE consuming.


"""
VERIFICATION TRACE (spend 1-2 min on this; it catches bugs out loud)
    For readability, FREE = Limit(capacity 2, 1 per second).
    t=0.0  allowRequest("a", FREE) -> new bucket(2.0); consume -> TRUE,  a.tokens 1.0
    t=0.0  allowRequest("a", FREE) -> refill +0 -> 1.0; consume -> TRUE,  a.tokens 0.0
    t=0.0  allowRequest("a", FREE) -> 0.0 < 1 -> FALSE, a.tokens 0.0 (nothing consumed)
    t=0.0  allowRequest("b", FREE) -> b's own new bucket -> TRUE, b.tokens 1.0 (Req 4)
    t=0.5  allowRequest("a", FREE) -> refill +0.5 -> 0.5 -> FALSE, a.tokens 0.5
    t=1.0  allowRequest("a", FREE) -> refill +0.5 -> 1.0 -> TRUE,  a.tokens 0.0   <- the floor() version says FALSE here, forever
    t=60   allowRequest("a", FREE) -> refill +59 -> capped at 2.0 -> TRUE, a.tokens 1.0
    allowRequest("", FREE)         -> throw "invalid client id"
    two threads, a.tokens 1.0, same instant -> exactly one TRUE (bucket lock)
"""


"""
--------------------------------------------------------------------------------
STEP 5: EXTENSIBILITY                                 ⏱ ~5 min
--------------------------------------------------------------------------------
# ✅ BEST: say WHERE the change lands (one method or one class), describe it,
#    and code it only if asked. A good design absorbs each change in ONE place.
# 🎯 GRADED: Extensibility (small changes, no rewrite).
"""

# Extensions
"""
1. "Another team wants a sliding-window limit instead of a token bucket. How do you support both?"
2. "Login should be limited much harder than search. How do you add per-endpoint limits?"
3. "Clients want to know WHEN they can retry. How do you return that?"
"""

# ---------------------------------------------------------------------------
# EXT 1: a second algorithm                                       📸 12, 13
# Pattern: STRATEGY. Extract the one method the limiter calls into an
#    interface; each algorithm implements it.
# Lands in: a new interface + a new class, and the ONE "new TokenBucket(...)"
#    line in getOrCreateBucket. allowRequest is unchanged.
# 💬 SAY: "allowRequest only ever calls tryConsume(now). So I make that an
#    interface, TokenBucket implements it, and the Limit says which algorithm to
#    build. That's the only place that knows there's more than one."
# ---------------------------------------------------------------------------
interface ClientLimiter:
    + tryConsume(now) -> boolean

class TokenBucket implements ClientLimiter       # unchanged

class SlidingWindowLog implements ClientLimiter:
    - limit: Limit                     # capacity = max requests per window
    - windowSeconds: number
    - timestamps: Queue<timestamp>     # allowed requests, oldest first

    synchronized tryConsume(now) -> boolean:
        while timestamps not empty && timestamps.peek() <= now - windowSeconds
            timestamps.poll()          # forget requests that left the window
        if timestamps.size() < limit.capacity
            timestamps.add(now)
            return true
        return false

class Limit:
    - algorithm: Algorithm             # TOKEN_BUCKET | SLIDING_WINDOW_LOG
    ...

# in getOrCreateBucket, the only changed line:
#    buckets.computeIfAbsent(clientId, id -> createLimiter(limit, clock.now()))
private createLimiter(limit, now) -> ClientLimiter:     # a simple factory
    if limit.algorithm == SLIDING_WINDOW_LOG
        return new SlidingWindowLog(limit, now)
    return new TokenBucket(limit, now)
# ⚠️ TRICKY: don't build this interface up front "in case". With one
#    algorithm it's speculative. The design was ready for it anyway because
#    the limiter only depended on tryConsume(now).
# ⚠️ TRICKY: the sliding log costs memory per REQUEST (up to capacity
#    timestamps per client), the token bucket costs O(1) per client. Say it.


# ---------------------------------------------------------------------------
# EXT 2: per-endpoint limits
# Pattern: COMPOSITE KEY. The bucket key becomes (client, endpoint), and the
#    limit lookup becomes (tier, endpoint) with a fallback to the tier default.
# Lands in: allowRequest's signature and getOrCreateBucket's key + lookup.
#    TokenBucket is unchanged.
# 💬 SAY: "A bucket doesn't care what it's counting. I just key it by client
#    AND endpoint, and look the limit up by tier and endpoint, falling back to
#    the tier's default."
# ---------------------------------------------------------------------------
class RateLimiter:
    - limits: Map<Tier, Limit>                            # defaults, as before
    - endpointLimits: Map<(Tier, endpoint), Limit>        # overrides, e.g. (FREE, "/login") -> Limit(3, 0.1)

    allowRequest(clientId, tier, endpoint) -> boolean:
        if clientId == null || clientId.isEmpty()
            throw Error("invalid client id")
        limit = endpointLimits[(tier, endpoint)] ?? limits[tier]
        key = clientId + "|" + endpoint
        bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(limit, clock.now()))
        return bucket.tryConsume(clock.now())
# ⚠️ TRICKY: endpoints without an override now get a bucket EACH, so a client
#    gets its full default limit on every endpoint. If the default is meant to
#    be one shared budget, key those as clientId + "|*". Ask which they want.


# ---------------------------------------------------------------------------
# EXT 3: tell the client when to retry (Retry-After)
# Pattern: RETURN A RESULT OBJECT instead of a boolean, computed under the
#    SAME lock as the decision.
# Lands in: TokenBucket.tryConsume's return value, passed straight through
#    allowRequest. The gateway sets the header (still out of scope).
# 💬 SAY: "The bucket already knows how far it is from one token: (1 - tokens)
#    divided by the rate. I return that with the decision, from inside the same
#    lock, so the answer can't be stale."
# ---------------------------------------------------------------------------
class Decision:
    - allowed: boolean
    - retryAfterSeconds: number        # 0 when allowed

class TokenBucket:
    synchronized tryConsume(now) -> Decision:
        refill(now)
        if tokens >= 1
            tokens = tokens - 1
            return Decision(true, 0)
        return Decision(false, (1 - tokens) / limit.refillPerSecond)
# ⚠️ TRICKY: a separate getRetryAfter() method called after a false would
#    race with other threads' consumes. Same answer, same lock.


"""
================================================================================
 MORE FOLLOW-UPS SENIORS GET (short answers)
================================================================================
- Attackers rotate API keys, or send with no key at all? -> The client id is
  just a string, so call the SAME limiter twice with different keys:
  allowRequest("ip:" + ip, FREE) and allowRequest("key:" + apiKey, tier). Deny
  if either says no. RateLimiter doesn't change. (An IP token is spent even
  when the key check then fails; acceptable, say it.)
- A client is upgraded FREE -> PREMIUM while its bucket is alive? -> The old
  bucket still holds the FREE Limit (a stale reference). In getOrCreateBucket,
  use compute instead of computeIfAbsent and replace the bucket when
  bucket.limit != limits[tier]. One method changes.
- We now run 50 API servers? -> Per-process buckets let a client do 50x its
  limit. Move (tokens, lastRefill) per key into a shared store such as Redis,
  and do refill + consume in ONE atomic server-side script, so the
  check-then-act problem doesn't come back over the network. Cheaper,
  approximate option: give each server limit / N. Then decide FAIL OPEN vs
  FAIL CLOSED when the store is down (usually fail open, with an alarm: an
  outage of the limiter shouldn't become an outage of the API).
- Memory keeps growing? -> The eviction trick from Step 4: drop buckets idle
  longer than capacity / refillPerSecond, since they're full again anyway.
  Remove with remove(key, bucket), so a sweep never deletes a bucket a request
  thread just replaced.
- The clock jumps (NTP adjusts wall time)? -> Use a monotonic clock
  (System.nanoTime, not currentTimeMillis). The elapsed <= 0 guard is a
  backstop, not the fix.

================================================================================
 60-SECOND RECAP (say this at the end)
================================================================================
- 3 classes + an enum: RateLimiter (orchestrator), TokenBucket (one client's
  state and rules), Limit (capacity + rate per Tier). Client, Request and
  "token" are not classes: an id, a parameter and a number.
- Token bucket because the requirement is "burst up to N, then an average
  rate". Fixed window fails it at the boundary (2x burst).
- Lazy refill on each request: O(1), exact, no background thread. Tokens are a
  decimal, so frequent callers are never starved (TRICKY #3).
- Concurrency: computeIfAbsent for the bucket, and one lock per bucket around
  refill + check + consume. Never a global lock.
- Denied is a return value, not an exception. Errors are for an empty client
  id and a bad config at startup.
- Extensions land in one place each: new algorithm = Strategy behind
  tryConsume, per-endpoint = composite key, Retry-After = result object.
================================================================================
"""
