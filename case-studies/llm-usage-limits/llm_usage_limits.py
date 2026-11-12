"""
================================================================================
 LLM USAGE LIMITS (5-hour + weekly quota): annotated interview solution
 Source: designed with the Hello Interview delivery framework (no video).
 Inspired by how AI chat products show "5-hour" and "weekly" usage limits.
 All numbers below (weights, limits, reply size) are ILLUSTRATIVE, not any
 vendor's real values.
================================================================================

Level: senior. Language: pseudo-code (the Java version is in ./code/).

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

💬 SAY (at the start): "Before I design anything I'd like to spend a few minutes
   on requirements. The interesting part here is that we don't know what a
   request costs until it's finished, so I want to pin down how usage is
   measured and what 'blocked' means before I draw any classes."
================================================================================


--------------------------------------------------------------------------------
STEP 1: REQUIREMENTS                                  ⏱ ~5 min   📸 00
--------------------------------------------------------------------------------

Design the usage limits for an AI chat product: every user gets a 5-hour limit and a weekly limit, and we only run inference while they have usage left.

# ⚠️ TRICKY: the prompt is vague ON PURPOSE. "Usage" is not defined, "limit" is
#    not defined, and the prompt hides the hard part: the cost of a request is
#    only known AFTER the model has answered. Starting with classes now fails
#    "problem analysis".
# 🎯 GRADED: Problem analysis, i.e. can you turn a vague prompt into clear,
#    agreed requirements?

1. primary capabilities
2. error handling
3. scope boundaries
# ✅ BEST: write the 3 bucket names first, then fill them in.

Primary capabilities:

- What does a request carry?
      -> userId, the model (e.g. HAIKU / SONNET / OPUS) and the message contents.
         Nothing about its cost.
- What is "usage" measured in? Requests? Tokens?
      -> Usage units = tokens (input + output) x a per-model weight. A bigger
         model burns the limit faster. Weights are config: HAIKU 1, SONNET 2, OPUS 5.
# 💬 SAY: "If I counted requests, a one-word question and a 50-page document
#    would cost the same. Tokens times a model weight is fair, and it's the
#    only definition that makes 'which model' matter."
- How do the windows work? Fixed clock times (every Monday 00:00) or from first use?
      -> From first use. The 5-hour window starts at the user's first request
         when no window is active and resets 5 hours later. The weekly window
         works the same way with 7 days.
- Are both limits checked on every request?
      -> Yes. Every request counts against both; the user is blocked if EITHER is used up.
- Does every user have the same limits?
      -> For now yes, one plan, limits are config (5h = 1000 units, weekly = 2500
         units in our examples). Plans are a follow-up.
- Can the user see how much they have left?
      -> Yes, remaining units per window.

Error handling:

- The request doesn't say how much it will cost. When do we decide to let it run?
      -> Your call. (This is the core of the problem; see Req 3.)
# 💬 SAY: "I can count the input tokens before running, roughly. The output is
#    unknown. So I'll estimate, hold that estimate while the request runs, and
#    correct it with the real number at the end."
- A user opens several chats and sends in all of them at once. Allowed?
      -> Yes. They run in parallel. The user is only blocked when the usage has
         actually run out, not because another chat is busy.
- A request is admitted with a little usage left but turns out to be expensive.
  Do we cut the answer off mid-way?
      -> No. An admitted request always finishes and is charged its real cost,
         even if that takes the user past the limit. The NEXT request is blocked.
# ⚠️ TRICKY: this one answer decides the whole admission rule. "Never exceed
#    the limit" and "never block while usage is left" can't both hold when the
#    cost is unknown. Ask which one wins. Here: the user-facing one wins, and
#    overshoot is accepted but must stay SMALL (bounded).
- The user is out of usage. What do they get?
      -> A specific error that says WHICH limit (5-hour or weekly) and WHEN it resets.
- The model call fails (timeout, server error). Is the user charged?
      -> No, charge nothing.
- Invalid request (no user id, no model, no messages)?
      -> Reject with "invalid request" before anything is reserved.

Scope boundaries:

- Do we run the model, or just decide whether it may run?
      -> We sit in front of it: decide, call the inference backend, charge. The
         backend itself is external.
- One server or many? Persistence?
      -> One process, in memory, many threads (parallel chats = concurrent calls).
         Multi-server is a follow-up.


Requirements:
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

# ✅ BEST: each requirement = ACTOR + ACTION + INPUT, with sub-bullets for the
#    system's reaction and error. Req 1 + 3 + 5 become handle(); Req 6 becomes
#    getRemaining(); Req 2 + 4 become the window class.
# ⚠️ TRICKY: Req 5's "used + reserved" is the whole point. Without "reserved",
#    ten chats started at the same instant all see the same remaining usage and
#    all get in. That's TRICKY #1 in Step 4.

Out of scope:
- Running the model (the inference backend is external; we call it)
- Plans / billing / payments (one plan, limits from config)
- Authentication (userId is trusted)
- Streaming the answer to the client, and cutting it off mid-stream
- Exact tokenization (a rough chars/4 count is fine for the estimate)
- Multiple servers / persistence
# 💬 SAY: "Everything not on this list I'll treat as out of scope."


--------------------------------------------------------------------------------
STEP 2: ENTITIES                                      ⏱ ~3 min   📸 —
--------------------------------------------------------------------------------
# ✅ BEST: write down EVERY noun, then prune each one out loud.
#
#   Noun               Keep?  Why
#   ---------------    -----  ---------------------------------------------------
#   User               NO     External identity. We only need its id as a KEY for
#                             its usage, so userId is a parameter / map key.
#   Chat/Conversation  NO     ⚠️ TRICKY: "parallel chats" tempts people to model a
#                             Chat. We don't care WHICH chat a request came from,
#                             only that requests run at the same time. Parallel
#                             chats = concurrent calls.
#   Message/contents   NO     Owned by the chat service. We only need their SIZE
#                             (for the estimate), so they stay a list of strings
#                             inside the request.
#   Model              ENUM   A fixed set with one rule each: its weight.
#   Request            YES    Immutable value object: userId + model + messages.
#                             It validates itself, so an invalid one can't exist.
#   Usage limit        YES →  ⚠️ TRICKY: "isn't the limit just a number?" No. A
#   (window)                  limit has a DURATION, a START, a RESET time and its
#                             own counters (used, reserved), plus a rule for
#                             rolling over. Something with its own rules is an
#                             entity: UsageWindow.
#   User's usage       YES    The two windows of ONE user, plus the place we lock
#                             so parallel chats can't race: UsageAccount.
#   Reservation        NO     Just a number held for the length of one call. It
#                             lives as a local variable in handle() (and as the
#                             "reserved" counter on each window).
#   Inference/backend  SEAM   External system. An interface we call.
#   Response/result    SEAM   What the backend returns: text + real token counts.
#   Limiter/gateway    YES    The orchestrator and entry point: UsageLimiter.
#
# 💬 SAY: "I'll cut User and Chat: we hold no state about them beyond a key. The
#    limit looks like a number but it has a start, a duration and a reset rule,
#    so it gets its own class."
# 🎯 GRADED: Class design (the right entities, and not too many).

Entities:
- UsageLimiter (orchestrator)
- UsageAccount (one per user)
- UsageWindow (one per limit type, per user)
- InferenceRequest (value object)
- (enum) Model: HAIKU(1), SONNET(2), OPUS(5)
- (enum) LimitType: FIVE_HOUR, WEEKLY
- (seam) InferenceBackend -> InferenceResult(text, inputTokens, outputTokens)


--------------------------------------------------------------------------------
STEP 3: CLASS DESIGN, TOP-DOWN                        ⏱ ~10-15 min
--------------------------------------------------------------------------------
# ✅ BEST: start with the ORCHESTRATOR and work down.
#       state    = "what must I REMEMBER to enforce the requirements?"
#       behavior = "what user ACTION needs this?"  -> public methods
"""

class UsageLimiter:
    - accounts: Map<userId, UsageAccount>          # Req 4-5: per-user windows
    - backend: InferenceBackend                    # Req 1: we call it, we don't own it
    - clock: Clock                                 # Req 4: windows need "now"; injected so tests control time
    - fiveHourLimit, weeklyLimit: long             # Req 4: config

    + handle(request: InferenceRequest) -> InferenceResult | UsageLimitExceeded   # Req 1, 3, 5
    + getRemaining(userId, limitType) -> long                                     # Req 6

    - estimateUnits(request) -> long               # Req 3: (input tokens + typical reply) x weight
    - actualUnits(model, result) -> long           # Req 2

    # ✅ BEST: Map<userId, UsageAccount>, created on first request.
    # 💬 SAY: "Accounts are created lazily on the first request. A read for a
    #    user we've never seen just returns the full limit; it doesn't create
    #    anything."
    # ✅ BEST: inject the clock. Every window rule is about time, and a test that
    #    has to wait 5 hours is not a test.


class UsageAccount:                                # one per user
    - windows: List<UsageWindow>                   # [FIVE_HOUR, WEEKLY]  Req 4

    + tryReserve(units, now) -> UsageWindow | null # Req 3, 5: atomic check + reserve; returns the BLOCKING window
    + settle(reservedUnits, actualUnits, now)      # Req 3: release estimate, charge actual
    + remaining(limitType, now) -> long            # Req 6

    # ✅ BEST: the account is the LOCK UNIT. tryReserve and settle are each one
    #    synchronized block per user. Two different users never wait for each
    #    other; two chats of the same user are serialized only for the few
    #    microseconds of the check, never for the model call.
    # ✅ BEST: a LIST of windows, not two named fields. Every rule is "for each
    #    window", and a third window (daily, or an OPUS-only weekly limit) is
    #    then a config change. (Two named fields is fine too; say why you chose.)
    # ✅ BEST: tryReserve RETURNS the blocking window instead of throwing, like
    #    getAvailableCompartment returns null in Amazon Locker. The orchestrator
    #    turns it into the requirement-named error.


class UsageWindow:
    - type: LimitType
    - duration: Duration                           # 5h or 7d
    - limit: long
    - used: long                                   # Req 2: actual charges in this window
    - reserved: long                               # Req 5: estimates of in-flight requests
    - resetAt: timestamp | null                    # Req 4: null = no active window

    + rollIfExpired(now)                           # Req 4: past resetAt -> used = 0, resetAt = null
    + startIfIdle(now)                             # Req 4: no active window -> resetAt = now + duration
    + hasRoom() -> boolean                         # Req 5: used + reserved < limit
    + reserve(units) / release(units) / charge(units)
    + remaining() -> long                          # Req 6: max(0, limit - used)
    + getType(), getResetAt()

    # ✅ BEST (Information Expert): the window owns its counters AND its reset
    #    rule. The account never does timestamp maths; it asks the window.
    # ⚠️ TRICKY (alternatives):
    #    BAD:   a log of every request per user, summed over the last 5h on each
    #           call. Correct, but grows with traffic and re-sums every time,
    #           and it still has the parallel race.
    #    GOOD:  a sliding window (usage "drips" out 5h after it was spent). Fair,
    #           but it's NOT what was asked: the agreed rule is "starts at first
    #           use, resets after 5h".
    #    GREAT: two counters + a reset time per window (this design). O(1) state.
    # 🎯 GRADED: Class design (clear ownership and boundaries).


class InferenceRequest:                            # immutable value object, Req 1
    - userId: string
    - model: Model
    - messages: List<string>
    # constructor throws "invalid request" if any is missing / empty


enum Model:     HAIKU(weight 1), SONNET(weight 2), OPUS(weight 5)        # Req 2
enum LimitType: FIVE_HOUR, WEEKLY                                       # Req 4

interface InferenceBackend:                        # out of scope: the real model servers
    + generate(model, messages) -> InferenceResult(text, inputTokens, outputTokens)


"""
--------------------------------------------------------------------------------
STEP 4: IMPLEMENTATION                                ⏱ ~10 min
--------------------------------------------------------------------------------
# 💬 SAY: "Pseudo-code or a real language?" Then: "The interesting logic is
#    handle() on the limiter, tryReserve and settle on the account, and the
#    window's roll-over. Getters, the request constructor and the backend are
#    trivial or external, so I'll skip them."
#
# ✅ BEST: for EVERY method, write the docstring FIRST:
#        Core logic: numbered steps
#        Edge cases: walk each step asking "what can go wrong here?"
# 🎯 GRADED: Code quality plus Communication.
"""

# Implementation
# 1. Define the core logic
# 2. Consider the edge cases

EXPECTED_REPLY_TOKENS = 100      # "typical reply size" config; small so the trace numbers stay small

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
        7. Settle: release the estimate, charge the actual   <- ALWAYS, even on failure
        8. Return the result

        Edge cases:
        1. invalid request -> throw "invalid request" (before anything is reserved)
        2. a window is used up -> throw UsageLimitExceeded(type, resetAt); nothing reserved
        3. backend throws -> release the estimate, charge 0, re-throw
        4. actual > estimate and it pushes the user over the limit -> allowed (Req 5)
        5. a window resets while the request is in flight -> see TRICKY #3
        """

        # ---- AS WRITTEN (what most candidates write first) ----
        #   account = getAccount(request.userId)
        #   if account.remaining(now) <= 0
        #       throw UsageLimitExceeded(...)
        #   result = backend.generate(request.model, request.messages)   # takes SECONDS
        #   account.charge(actualUnits(request.model, result), now)
        #   return result
        #
        # ⚠️ TRICKY #1 (THE trap; spotting it is the senior signal):
        #    This is CHECK-THEN-ACT with a multi-second gap. A user with 400 units
        #    left opens 10 chats and hits send in all of them. All 10 checks run
        #    before ANY charge lands, so all 10 see "400 left" and all 10 run.
        #    At 200 units each the user ends at 2000 used against a 1000 limit,
        #    and it's unbounded: 100 chats = 100 free requests.
        #    Putting a lock around the whole method "fixes" it by running the
        #    user's chats one at a time, which breaks Req 5 (parallel chats).
        #
        # ✅ BEST FIX: RESERVE at admission, SETTLE at the end.
        #    - The check and the reservation are ONE atomic step on the account,
        #      so each new chat sees the estimates of the chats already running.
        #    - The slow model call runs OUTSIDE the lock, so chats still run in
        #      parallel.
        #    - Overshoot is now bounded by roughly ONE estimate per window edge,
        #      not by the number of chats.

        # ---- BEST VERSION ----
        if request == null
            throw Error("invalid request")              # the request validated its own fields when it was built
        account = accounts.computeIfAbsent(request.userId, -> new UsageAccount(fiveHourLimit, weeklyLimit))
        estimate = estimateUnits(request)
        now = clock.now()

        blocking = account.tryReserve(estimate, now)
        if blocking != null
            throw UsageLimitExceeded(blocking.getType(), blocking.getResetAt())

        actual = 0
        try
            result = backend.generate(request.model, request.messages)   # NOT under any lock
            actual = actualUnits(request.model, result)
            return result
        finally
            account.settle(estimate, actual, clock.now())   # clock.now() again: time has passed

        # ⚠️ TRICKY #2: the settle MUST be in a finally. Without it, a backend
        #    timeout leaves the estimate reserved forever: "phantom usage". After
        #    a few timeouts the user is blocked with nothing used, and no reset
        #    clears it (reset only clears "used", see TRICKY #3).
        # 💬 SAY: "Settle is in a finally, so a failed call gives the estimate
        #    back. The user isn't charged for our outage."


    private estimateUnits(request)
        """
        Core logic:
        1. inputTokens = total characters of the messages / 4 (rounded up)
        2. return (inputTokens + EXPECTED_REPLY_TOKENS) x model weight
        """
        chars = sum(len(m) for m in request.messages)
        inputTokens = ceil(chars / 4)
        return (inputTokens + EXPECTED_REPLY_TOKENS) * request.model.weight

        # ??? decision: reserve a TYPICAL reply, or the MAX reply?
        # 💬 SAY: "If I reserve the worst case (say 8k output tokens on OPUS), a
        #    user with half their limit left can open maybe two chats before the
        #    reservations alone block them. That breaks 'block only when usage
        #    has run out'. So I reserve a typical reply. The estimate only has to
        #    be good enough to stop a burst of parallel chats; settle fixes the
        #    real number a few seconds later."
        # ✅ BEST: estimate quality is a knob, not a correctness issue. EXT 2
        #    makes it smarter (per-user moving average) without touching handle().


    private actualUnits(model, result)
        return (result.inputTokens + result.outputTokens) * model.weight       # Req 2


    getRemaining(userId, limitType)
        account = accounts.get(userId)
        if account == null
            return limitFor(limitType)                  # never seen: full limit; don't create on a read
        return account.remaining(limitType, clock.now())


class UsageAccount:

    synchronized tryReserve(units, now)
        """
        Core logic:
        1. Roll every window that has expired, and start any idle one
        2. Find every window with no room (used + reserved >= limit)
        3. If any: return the one that resets LAST (reserve nothing)
        4. Otherwise reserve the units in EVERY window, return null

        Edge cases:
        1. both windows full -> report the one with the later reset (TRICKY #4)
        2. 5h has room but weekly doesn't -> reserve NOTHING (TRICKY #5)
        3. used + reserved is 999 of 1000 -> admit (Req 5: not run out yet)
        """
        for w in windows
            w.rollIfExpired(now)
            w.startIfIdle(now)                          # a request attempt starts the window

        blocking = null
        for w in windows
            if !w.hasRoom()
                if blocking == null || w.getResetAt() > blocking.getResetAt()
                    blocking = w
        if blocking != null
            return blocking

        for w in windows
            w.reserve(units)
        return null

        # ⚠️ TRICKY #4: if BOTH windows are full, report the later reset. Saying
        #    "5-hour limit, resets in 2h" when the weekly limit blocks them for 3
        #    more days sends the user back in 2h to be rejected again.
        # ⚠️ TRICKY #5: CHECK ALL windows, THEN reserve in all. Reserving in the
        #    5h window and then finding the weekly window full leaves a leaked
        #    reservation in the 5h window. Two loops, not one.
        # ??? decision: the admission rule is "used + reserved < limit", NOT
        #    "used + reserved + estimate <= limit".
        # 💬 SAY: "With the second rule a user with 150 units left is blocked
        #    because I GUESSED their next answer costs 206. The requirement says
        #    block only when it's run out, so I admit while anything is left and
        #    accept a bounded overshoot."
        # 🎯 GRADED: Communication (you raise the product decision instead of
        #    quietly picking one).


    synchronized settle(reservedUnits, actualUnits, now)
        """
        Core logic:
        1. For each window: roll it if expired
        2. Release the reservation
        3. If actual > 0: start the window if idle, then charge the actual

        Edge cases:
        1. actual = 0 (failure) -> release only
        2. the window reset while the request was running -> release still works
           because the reset kept "reserved"; the actual lands in the new window
        """
        for w in windows
            w.rollIfExpired(now)
            w.release(reservedUnits)
            if actualUnits > 0
                w.startIfIdle(now)
                w.charge(actualUnits)


    synchronized remaining(limitType, now)
        w = window(limitType)
        w.rollIfExpired(now)
        return w.remaining()                            # max(0, limit - used)


class UsageWindow:

    rollIfExpired(now)
        """
        Core logic:
        1. If there is an active window and now >= resetAt:
           used = 0, resetAt = null
        2. Do NOT touch reserved

        Edge cases:
        1. no active window -> nothing to do
        2. requests in flight across the reset -> their reservations carry over
        """
        if resetAt != null && now >= resetAt
            used = 0
            resetAt = null
            # reserved is NOT reset

        # ⚠️ TRICKY #3: the obvious reset is "used = 0; reserved = 0". Then a
        #    request admitted at 4h59m settles at 5h01m and subtracts its 206 from
        #    a reserved counter that is already 0: reserved = -206. Every later
        #    admission sees 206 units of FREE headroom, forever. A reservation
        #    belongs to a request that is still running, not to a window, so it
        #    survives the reset.
        # 💬 SAY: "Reset clears what was spent, not what is still running."

    startIfIdle(now)
        if resetAt == null
            resetAt = now + duration

    hasRoom()         return used + reserved < limit
    reserve(units)    reserved += units
    release(units)    reserved -= units
    charge(units)     used += units
    remaining()       return max(0, limit - used)      # overshoot shows as 0, not negative

    # ??? decision: overshoot is NOT carried into the next window ("debt").
    # 💬 SAY: "If a user ends at 1200 of 1000, the next window still starts at
    #    0. Carrying debt is possible, but it punishes the user for our estimate,
    #    and the overshoot is small. I'd confirm with product."


"""
VERIFICATION TRACE (spend 1-2 min on this; it catches bugs out loud)
    limits: FIVE_HOUR 1000, WEEKLY 2500 units. Typical reply 100 tokens.
    weights: HAIKU 1, SONNET 2, OPUS 5. "hello world!" = 12 chars = 3 tokens.

    t=0h   handle(u1, SONNET, ["hello world!"])  estimate (3+100)*2 = 206, admitted
           backend: 3 in / 97 out -> actual 200
           -> 5h: used 200, resets 5h · weekly: used 200, resets 7d · reserved 0 / 0
    t=1h   handle(u1, OPUS)  estimate 515; 200 + 0 < 1000 -> admitted
           backend: 3 in / 197 out -> actual 1000 -> 5h used 1200 (over by 200: allowed, Req 5)
           getRemaining(u1, FIVE_HOUR) -> 0 (not -200)
    t=2h   handle(u1, HAIKU) -> UsageLimitExceeded(FIVE_HOUR, resets 5h)
    t=5h   handle(u1, HAIKU) -> 5h window rolls (used 0); admitted; actual 100
           -> 5h used 100 (resets 10h) · weekly used 1300
    t=10h  handle(u1, OPUS)  -> 5h rolls; admitted; actual 1000 -> 5h 1000 · weekly 2300
    t=15h  handle(u1, OPUS)  -> 5h rolls; weekly 2300 < 2500 -> admitted -> weekly 3300
    t=16h  handle(u1, HAIKU) -> BOTH full -> UsageLimitExceeded(WEEKLY, resets 7d)   (TRICKY #4)
    t=7d   handle(u1, HAIKU) -> both windows roll -> admitted
    handle(u1, SONNET, [])          -> "invalid request", nothing reserved
    backend throws for u3           -> error re-thrown; u3 charged 0; reserved back to 0 (TRICKY #2)

    THE TRAP (parallel chats, TRICKY #1): u2 has used 600 of 1000 in the 5h window.
    Three chats send SONNET "hello world!" at the same instant (estimate 206, actual 200 each).
      AS WRITTEN: all three see 400 left -> all run -> used 1200. With 20 chats: 4600.
      BEST:  A: 600 + 0   < 1000 -> admitted, reserved 206
             B: 600 + 206 < 1000 -> admitted, reserved 412
             C: 600 + 412 = 1012 -> UsageLimitExceeded(FIVE_HOUR)
             A, B settle -> used 1000, reserved 0. Overshoot bounded no matter how many chats.
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
1. "Free, Pro and Max users get different limits. And a user can upgrade in the middle of a window."
2. "Your estimate is a fixed 100-token reply. Can you do better?"
3. "OPUS is expensive. Add a separate weekly limit that only OPUS requests count against."
4. "A user opens 500 chats at once to squeeze out extra usage. How do you stop it?"
5. "We now run 50 gateway servers behind a load balancer."
6. "Answers are streamed. What if the user hits the limit mid-answer?"
"""

# ---------------------------------------------------------------------------
# EXT 1: plan tiers + upgrade mid-window
# Pattern: CONFIG BECOMES A LOOKUP. A Plan enum carries a limit multiplier; the
#    window keeps its base limit and the account re-applies the plan.
# Lands in: UsageAccount (+ applyPlan) and UsageWindow (+ setLimit). handle() unchanged.
# 💬 SAY: "Limits stop being two config numbers and come from the user's plan.
#    On upgrade I change each window's LIMIT and keep its USED. The user
#    immediately has more headroom in the same window; we don't reset their
#    window, which would be a free refill they could game by toggling plans."
# ---------------------------------------------------------------------------
enum Plan: FREE(x1), PRO(x5), MAX(x20)

class UsageLimiter:
    + setPlan(userId, plan)
        account = accounts.computeIfAbsent(userId, ...)
        account.applyPlan(plan)

class UsageAccount:
    synchronized applyPlan(plan)
        for w in windows
            w.setLimit(w.baseLimit * plan.multiplier)     # used stays; reserved stays
# ⚠️ TRICKY: DOWNGRADE mid-window can leave used > limit. That's fine: the
#    user is simply blocked until reset, same as an overshoot.


# ---------------------------------------------------------------------------
# EXT 2: better estimates
# Pattern: STRATEGY. estimateUnits() asks a UsageEstimator for the expected
#    reply size; the actual size is fed back after each request.
# Lands in: estimateUnits() + one record() call in handle()'s success path.
# 💬 SAY: "A fixed 100 tokens is wrong for a user who always asks for code.
#    I'd keep a moving average of output tokens per user and model, seeded at
#    the typical value. It's a Strategy so we can swap in a model-based
#    predictor later. Better estimates mean smaller overshoot; correctness
#    never depended on them."
# ---------------------------------------------------------------------------
interface UsageEstimator:
    + expectedReplyTokens(userId, model) -> long
    + record(userId, model, actualOutputTokens)

class FixedEstimator implements UsageEstimator          # today's behaviour: always 100
class MovingAverageEstimator implements UsageEstimator  # avg = 0.8 * avg + 0.2 * actual, per (user, model)


# ---------------------------------------------------------------------------
# EXT 3: an OPUS-only weekly limit
# Pattern: a window gets a SCOPE (the set of models it counts). The account
#    only touches windows whose scope contains the request's model.
# Lands in: UsageWindow (+ appliesTo(model)), UsageAccount's loops filter on it,
#    LimitType gains OPUS_WEEKLY. handle() passes the model through.
# 💬 SAY: "This is why the account holds a LIST of windows. A third window is
#    config: OPUS_WEEKLY, 7 days, its own limit, scope = {OPUS}. A HAIKU request
#    skips it; an OPUS request must have room in all three."
# ---------------------------------------------------------------------------
class UsageWindow:
    - models: Set<Model>                                 # all models for FIVE_HOUR / WEEKLY
    + appliesTo(model) -> models.contains(model)

class UsageAccount:
    synchronized tryReserve(model, units, now)
        relevant = [w for w in windows if w.appliesTo(model)]
        ... same body as before, over "relevant" ...
# ⚠️ TRICKY: settle must filter with the SAME model, or an OPUS reservation is
#    released from a window it was never reserved in.


# ---------------------------------------------------------------------------
# EXT 4: abuse with hundreds of parallel chats
# Pattern: CAP CONCURRENCY per user (a counter on the account, checked in tryReserve).
# Lands in: UsageAccount (inFlight counter + maxInFlight), one new error.
# 💬 SAY: "Reservations already bound the overshoot to about one estimate,
#    because each chat sees the others' reservations. What's left is load: 500
#    in-flight requests from one user. So I cap in-flight requests per user,
#    say 10, and reject the 11th with 'too many concurrent requests'. That
#    isn't a usage limit, so it's a different error."
# ---------------------------------------------------------------------------
class UsageAccount:
    - inFlight: int
    - maxInFlight: int
    # tryReserve: if inFlight >= maxInFlight -> TooManyConcurrentRequests (check BEFORE reserving)
    #             on success inFlight++ ; settle: inFlight--


# ---------------------------------------------------------------------------
# EXT 5: 50 gateway servers (describe only)
# Pattern: move the account to SHARED STATE with an ATOMIC reserve, and give
#    every reservation a TTL.
# Lands in: UsageAccount's storage (in-memory -> Redis/DB). handle() unchanged.
# 💬 SAY: "The synchronized block becomes one atomic operation in a shared
#    store, e.g. a Redis Lua script that rolls the windows, checks all of
#    them and reserves in one round trip. A server can crash between reserve
#    and settle, and there's no finally across a crash, so each reservation is
#    stored with a TTL a bit longer than the longest request. If it never
#    settles, it expires. To cut latency, a server can lease a block of units
#    per user locally and refill it, trading a slightly larger overshoot for
#    fewer round trips."
# ---------------------------------------------------------------------------


# ---------------------------------------------------------------------------
# EXT 6: streaming and mid-answer limits (describe only)
# Pattern: INCREMENTAL SETTLE. Charge per chunk instead of once at the end.
# Lands in: handle()'s settle step (called per chunk with the delta).
# 💬 SAY: "Today we finish the answer and charge it, which the interviewer
#    agreed to. If product wants a hard stop, the backend streams chunks and
#    I settle each chunk's delta. When a window runs out mid-stream I finish
#    the current sentence, stop, and mark the answer as truncated. I'd push
#    back though: cutting answers off is a worse experience than a small
#    overshoot."
# ---------------------------------------------------------------------------


"""
================================================================================
 MORE FOLLOW-UPS SENIORS GET (short answers)
================================================================================
- Reads of getRemaining are hot (a usage bar on every page)? -> It's one
  synchronized read per user; fine. At scale, serve it from the shared store
  with a short cache; it's display-only, admission never reads the cache.
- Show "% used" instead of units? -> remaining / limit in the UI layer. Units
  stay the internal currency so weights can change without migrating data.
- Change model weights while requests are in flight? -> The estimate and the
  actual both use the weight read at admission (store it with the reservation),
  or a re-weighted request settles with a mismatched estimate.
- Idempotent retries (client retries the same request after a timeout)? ->
  Request id + a short-lived "already settled" set, so a retry isn't charged twice.
- Prompt caching makes some input tokens cheaper? -> actualUnits() takes the
  cached-token count from the result and weighs it lower. One method changes.
- Admin grants a one-off bonus? -> a negative charge (credit) on the window, or
  a temporary limit bump that ends at resetAt.

================================================================================
 60-SECOND RECAP (say this at the end)
================================================================================
- Usage = (input + output tokens) x model weight, charged to a 5-hour and a
  weekly window. Each window starts at first use and resets after its duration.
- Entities: UsageLimiter (orchestrator), UsageAccount (one user's windows and
  the lock), UsageWindow (counters + reset rule). User, Chat and Message are not
  classes.
- The cost is unknown up front, so: ESTIMATE, RESERVE atomically, run the model
  outside the lock, SETTLE in a finally with the real cost.
- Admission counts used + reserved, so parallel chats see each other and the
  overshoot is bounded. Users are blocked only when it has actually run out.
- Traps: check-then-act across the model call; no finally (phantom usage);
  reset wiping reservations; partial reservation across windows; reporting the
  wrong reset time.
- Extensions land in one place each: plans = limit multiplier, estimates =
  Strategy, OPUS limit = window scope, abuse = in-flight cap.
================================================================================
"""
