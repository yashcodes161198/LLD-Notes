    """
================================================================================
 AMAZON LOCKER: annotated interview solution
 Source: Hello Interview, Evan King (ex-Meta Staff), "Design Amazon Locker"
         https://www.youtube.com/watch?v=s6nGkoGJhXk   (editor file: pastebin.com/uMD5XNU7)
================================================================================

This is the file as typed in the video, with notes added. The interviewer's
code is kept, typos are fixed, and one real bug is fixed in the BEST version
(see TRICKY #6).

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
   on requirements. I'll go through primary capabilities, error handling and
   scope, then write down what we agree on."
================================================================================


--------------------------------------------------------------------------------
STEP 1: REQUIREMENTS                                  ⏱ ~5 min   📸 01, 02, 03
--------------------------------------------------------------------------------

Design a locker system like Amazon Locker where delivery drivers can deposit packages and customers can pick them up using a code.

# ⚠️ TRICKY: the prompt is vague ON PURPOSE. Starting to code now is the
#    number-one way to fail "problem analysis".
# 🎯 GRADED: Problem analysis, i.e. can you turn a vague prompt into clear,
#    agreed requirements?

1. primary capabilities
2. error handling
3. scope boundaries
# ✅ BEST: these 3 buckets are how you always know what to ask. Write the
#    bucket names first, then fill them in. (For games, add a 4th bucket:
#    "rules & completion".)

Primary capabilities:

- Are there different sized compartments?
      -> Interviewer: yes, S/M/L. A package goes in a compartment of its size.
- How does the customer get their code? Do we need to send an SMS or email?
      -> Interviewer: out of scope. Just RETURN the code; a parent service sends it.
# ✅ BEST: asking "do we send the SMS?" lets the interviewer cut scope. When
#    they do, your API gets simpler: depositPackage RETURNS the code.

Error handling:

- Can one customer have multiple packages in the system at once? Are access tokens unique per package?
      -> Yes; one token per package, one compartment per package.
- How long do the codes last? What happens to a package if it's never picked up?
      -> Expire after 7 days; staff come and clear expired compartments.
- What if all compartments of a given size are full when a driver tries to deposit?
      -> Throw an error.
# 💬 SAY: "Expiry and 'locker full' are the two failure modes I want pinned
#    down, because they decide what errors my API throws."

Scope boundaries:

- What's in scope for this system? Are we modeling the whole delivery flow, or just the piece from when the driver arrives at the locker until the customer picks up?
      -> Just driver-arrives → customer-picks-up.


Requirements:
1. Carrier deposits a package by specifying size (small, medium, large)
   - System assigns an available compartment of matching size
   - Opens compartment and returns access token, or error if no space
2. Upon successful deposit, an access token is generated and returned
   - One access token per package
3. User retrieves package by entering access token
   - System validates code and opens compartment
   - Throws specific error if code is invalid or expired
4. Access tokens expire after 7 days
   - Expired codes are rejected if used for pickup
   - Package remains in compartment until staff removes it
5. Staff can open all expired compartments to manually handle packages
   - System opens all compartments with expired tokens
   - Staff physically removes packages and returns them to sender

# ✅ BEST: each requirement = ACTOR + ACTION + INPUT, with sub-bullets for the
#    system's reaction and its error. Each top-level item later becomes
#    (part of) a public method. Reqs 1, 3 and 5 map to the 3 Locker methods.
# ⚠️ TRICKY: "Throws SPECIFIC error if invalid OR expired" means you need TWO
#    different errors later. One generic "invalid" loses points.

Out of scope:
- How the package gets to the locker (delivery logistics)
- How the access token reaches the customer (SMS/email notification)
# 💬 SAY: "Everything not on this list I'll treat as out of scope."
#    (Hello Interview's write-up also lists: lockout after N failed attempts,
#    multiple locker stations, payments. These are good to name.)


--------------------------------------------------------------------------------
STEP 2: ENTITIES                                      ⏱ ~3 min   📸 04, 05
--------------------------------------------------------------------------------
# ✅ BEST: write down EVERY noun from the requirements, then prune each one out
#    loud. The pruning is where the signal is.
#
#   Noun            Keep?  Why
#   ------------    -----  -----------------------------------------------------
#   Carrier/Driver   NO    External actor. It calls our API; we store nothing about it.
#   User/Customer    NO    Same. They just type a code.
#   Package          NO    ⚠️ TRICKY: "most candidates add a Package class".
#                          Another system owns its id, address and contents. We only
#                          need its SIZE, so size becomes a PARAMETER of depositPackage.
#   Locker           YES   The orchestrator / entry point. Finds compartments, issues codes.
#   Compartment      YES   A physical slot with its own state (size, occupied, door).
#   AccessToken      YES   ⚠️ TRICKY: "isn't it just a string?" No. It's a BEARER
#                          token: it has an EXPIRY and grants the right to ONE
#                          compartment. Something with its own rules is an entity.
#
# 💬 SAY: "I'll list all the nouns, then cut anything we don't hold state or
#    rules for. If I find I need Package later, I'll come back and add it."
# 🎯 GRADED: Class design (the right entities, and not too many).

Entities:
- Compartment
- Locker
- AccessToken
- (enum) Size: SMALL, MEDIUM, LARGE


--------------------------------------------------------------------------------
STEP 3: CLASS DESIGN, TOP-DOWN                        ⏱ ~10-15 min  📸 06, 07
--------------------------------------------------------------------------------
# ✅ BEST: start with the ORCHESTRATOR and work down. Its high-level operations
#    tell you what the lower classes must provide.
#    For each class, scan the requirements twice:
#       state    = "what must I REMEMBER to enforce the requirements?"
#       behavior = "what user ACTION needs this?"  -> public methods
"""

class Locker:
    - compartments: Compartment[]                       # Req 1: "assigns an available compartment"
    - accessTokensMapping: Map<string, AccessToken>     # Req 3: "validates code" -> look up by code

    + depositPackage(size) -> string | error            # Req 1 + 2 (returns the code)
    + pickup(code: string) -> void | error              # Req 3
    + openExpiredCompartments() -> void                 # Req 5

    # ✅ BEST: Map<code, AccessToken> instead of a list of tokens.
    # 💬 SAY: "I'm using a map, but NOT for the O(1). With 50-100 compartments a
    #    scan is instant. I'm using it because it makes the intent clear: a code
    #    maps to a token."
    # ⚠️ TRICKY: LeetCode habits push people to O(1)-everything. The interviewer
    #    rewards clarity when n is small. Say out loud that you KNOW n is small.


class AccessToken:
    - code: string                    # what the customer types
    - expiration: timestamp           # Req 4: 7 days
    - compartment: Compartment        # the right it grants

    + isExpired() -> boolean          # now > expiration
    + getCompartment() -> Compartment
    + getCode() -> string


class Compartment:
    - size: Size # SMALL, MEDIUM, LARGE
    - occupied: boolean

    + isOccupied() -> boolean
    + markOccupied() -> void
    + markFree() -> void
    + open() -> void                  # hardware hook: declared, never implemented
    + getSize() -> Size

    # ✅ BEST (Information Expert): OCCUPANCY LIVES ON THE COMPARTMENT.
    # 💬 SAY: "Occupancy is a PHYSICAL property of the compartment, not a
    #    relationship between objects, so the compartment owns it. The Locker
    #    could keep an occupied-set for O(1) lookups, but then the same fact is
    #    stored twice and can drift."
    # ⚠️ TRICKY (alternatives the write-up rejects):
    #    BAD:   derive "occupied" from whether a valid token exists. An expired
    #           token's compartment is STILL physically full, so validity and
    #           occupancy diverge.
    #    GOOD:  size-indexed free-queues on the Locker, O(1). Rejected: duplicated
    #           state that has to be kept in sync.
    #    GREAT: a flag on Compartment plus a linear scan (this design).
    # 🎯 GRADED: Class design (clear ownership and boundaries).


"""
--------------------------------------------------------------------------------
STEP 4: IMPLEMENTATION                                ⏱ ~10 min   📸 08-12
--------------------------------------------------------------------------------
# 💬 SAY: "Pseudo-code or a real language?" (~65-70% of interviews want
#    pseudo-code.) Then: "The interesting logic is all in Locker. AccessToken
#    and Compartment are getters and setters, isExpired is one comparison, and
#    open() is hardware, so I'll implement the three Locker methods."
#    (Seniors propose this themselves. For juniors the interviewer usually picks.)
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

class Locker:
    depositPackage(size):
        """
        Core logic:
        1. Find compartment of the right size
        2. Open the compartment
        3. Mark this compartment as occupied
        4. Generate an accessToken
        5. Store the accessToken
        6. Return the access token code

        Edge cases:
        1. No compartment of right size -> throw error
        # 💬 SAY while walking the steps: "Open could fail mechanically, but I'll
        #    treat hardware as out of scope. Storing in the map can't realistically
        #    run out of memory with ~100 compartments."
        """
        compartment = getAvailableCompartment(size)
        if compartment == null
            throw Error("No available compartment of that size")
        compartment.open()
        compartment.markOccupied()
        accessToken = generateAccessToken(compartment)
        code = accessToken.getCode()          # ✅ BEST: read it once and reuse; don't call getCode() twice
        accessTokensMapping[code] = accessToken

        return code

        # ⚠️ TRICKY: this assumes the driver really puts the package in. That's
        #    fine for now; it's the 3rd extensibility question (two-phase commit).


    private getAvailableCompartment(size)
        """
        Core logic:
        1. Scan all compartments
        2. For each, see if right size and free
        3. First that matches, return it

        Edge cases:
        1. none available -> return null   (the CALLER decides to throw)
        """
        for c in compartments
            if c.getSize() == size && !c.isOccupied()
                return c
        return null

        # ✅ BEST: private helpers NAMED AFTER A CORE-LOGIC STEP. depositPackage
        #    then reads 1:1 against its docstring.
        # 💬 SAY: "Want me to implement the helpers, or is the intent clear?"
        #    (Implement this one: it's the one that changes in Extension 1.)


    private generateAccessToken(compartment)
        # 💬 SAY (don't implement unless asked): "a cryptographically secure random
        #    code, expiry = now + 7 days, then call the AccessToken constructor."
        # ⚠️ TRICKY (senior bonus): codes must be UNIQUE among live tokens, so
        #    regenerate on collision with an existing key in the map.


    pickup(code: string)
        """
        Core logic:
        1. Look up the code to get accessToken
        2. Get the compartment
        3. Open the compartment
        4. Mark the compartment as free
        5. Remove access code from the map

        Edge cases:
        1. validation of the code itself, empty -> throw "invalid code"
        2. code is not in mapping -> throw "invalid code"
        3. accessToken is expired -> throw "access code expired"   (a DIFFERENT error, per Req 3)
        """
        if code.isEmpty()
            throw Error("invalid code")

        accessToken = accessTokensMapping[code]
        # 💬 SAY: "Map-miss behaviour depends on the language (Python raises
        #    KeyError). In my pseudo-code I'll assume it returns null."

        if accessToken == null
            throw Error("invalid code")

        if accessToken.isExpired()
            throw Error("access code expired")
            # ⚠️ TRICKY: do NOT free the compartment here. The package is still
            #    inside, waiting for staff (Req 4).

        compartment = accessToken.getCompartment()
        compartment.open()
        compartment.markFree()
        accessTokensMapping.remove(code)

        # ⚠️ TRICKY: physical simplification. We assume the door auto-closes after
        #    ~30s and the customer took the package. 💬 SAY: "Real lockers use
        #    sensors. Can I assume the door auto-closes and the package is taken?"
        #    Interviewers almost always say yes.
        # ⚠️ TRICKY: guard order matters. Validate first, look up, THEN expiry,
        #    and only then mutate state. No state changes before all checks pass.


    openExpiredCompartments()
        """
        Core logic:
        1. Scan through all access tokens in mapping
        2. Find the expired ones
        3. Get the compartment of each
        4. Open those compartments
           --- staff physically remove the packages (outside our system)
        5. Mark each compartment available again
        6. Remove access code from map???  decision: see below

        Edge cases:
        - empty map / nothing expired -> loop does nothing, fine
        - everything expired -> opens all, fine
        - called twice -> see TRICKY #6 below
        """

        # ---- AS WRITTEN IN THE VIDEO ----
        for code, accessToken in accessTokensMapping
            if accessToken.isExpired()
                compartment = accessToken.getCompartment()
                compartment.open()
                # assume the employee removed them and doors auto-close
                compartment.markFree()
                # not removed from accessTokensMapping so they still get the correct error
        # Loop to clear out the accessTokens from the map that are 3 months+ old.

        # 💬 SAY (the product trade-off, the best moment in the interview):
        #    "If I delete expired tokens, a customer who comes on day 10 gets
        #    'invalid code' instead of 'code expired'. That's a worse experience,
        #    so I'll keep them. Memory: ~50 compartments turning over daily is
        #    ~5k entries in 100 days, which is nothing. A periodic cleanup can drop
        #    tokens older than ~3 months."
        # 🎯 GRADED: Communication (you raise a product decision instead of quietly
        #    picking one).

        # ⚠️ TRICKY #6 (a real bug in the video version; spotting it is a SENIOR signal):
        #    The video calls this method idempotent. It is not, once a compartment
        #    is REUSED. The expired token stays in the map and still points at its
        #    compartment. After markFree(), a driver can deposit a NEW package into
        #    that compartment. The next staff run sees the OLD expired token again
        #    and opens the door on the NEW customer's package.
        #
        # ✅ BEST FIX: keep the nice error message without keeping the pointer.
        #    Move expired codes out of the live map into a separate set.

    # ---- BEST VERSION ----
    # extra state on Locker:   - expiredCodes: Set<string>
    openExpiredCompartments()
        for code, accessToken in copy(accessTokensMapping)     # copy: we remove while iterating
            if accessToken.isExpired()
                compartment = accessToken.getCompartment()
                compartment.open()
                compartment.markFree()
                accessTokensMapping.remove(code)
                expiredCodes.add(code)        # remembered ONLY for the error message

    # ...and pickup gains one line, after the null check:
    #    if accessToken == null
    #        if expiredCodes.contains(code)  throw Error("access code expired")
    #        throw Error("invalid code")
    # Now it really is idempotent, and the late customer still gets "expired".
    # (Cleanup of expiredCodes after ~3 months is the same as before.)
    # 💬 SAY: "Keeping the token in the live map means a reused compartment could
    #    be reopened by a later staff run, so I'll keep just the code in an
    #    expired-set for messaging."


"""
VERIFICATION TRACE (spend 1-2 min on this; it catches bugs out loud)
    compartments = [S1, M1, M2, L1]
    depositPackage(MEDIUM)  -> M1 opens, M1.occupied = true, map {"K7Q2": tok(M1, +7d)}, returns "K7Q2"
    pickup("")              -> throw "invalid code"
    pickup("ZZZZ")          -> throw "invalid code"
    pickup("K7Q2") on day 3 -> M1 opens, M1 free, map {}
    deposit(M) day 0, pickup on day 9 -> throw "access code expired"; M1 stays occupied
    openExpiredCompartments() -> M1 opens + freed; code moved to expiredCodes
    pickup(that code) day 10 -> throw "access code expired" (not "invalid")
"""


"""
--------------------------------------------------------------------------------
STEP 5: EXTENSIBILITY                                 ⏱ ~5 min   📸 13-16
--------------------------------------------------------------------------------
# ✅ BEST: say WHERE the change lands (one method or one class), describe it,
#    and code it only if asked. A good design absorbs each change in ONE place.
# 🎯 GRADED: Extensibility (small changes, no rewrite).
"""

# Extensions
"""
1. "What if we want to allow a smaller package to use a larger compartment as a fallback when all exact-size compartments are full?"
2. "How would you handle compartments that are broken or under maintenance?"
3. "How would you ensure packages are actually deposited before generating access tokens?"
"""

# ---------------------------------------------------------------------------
# EXT 1: size fallback                                            📸 14
# Pattern: ONE OUTER LOOP over an ORDERED enum list, starting at the requested size.
# Lands in: getAvailableCompartment only. depositPackage is unchanged.
# 💬 SAY: "Only getAvailableCompartment changes. I add an outer loop over sizes
#    in ascending order, starting at the requested one. Never go smaller."
# ---------------------------------------------------------------------------
private getAvailableCompartment(requestedSize)
    sizesInOrder = [SMALL, MEDIUM, LARGE]
    startIndex = sizesInOrder.indexOf(requestedSize)

    for i from startIndex to sizesInOrder.length - 1
        size = sizesInOrder[i]
        for c in compartments
            if c.getSize() == size && !c.isOccupied()
                return c

    return null
# ⚠️ TRICKY: the order of the sizes matters, so don't rely on how the enum
#    happens to be declared. Keep it in an explicit list.
#    Senior bonus: "Do we want small packages to eat the large compartments?
#    Maybe only fall back when exact-size occupancy is above X%." That's a
#    product question.


# ---------------------------------------------------------------------------
# EXT 2: broken / under-maintenance compartments                  📸 15
# Pattern: BOOLEAN → ENUM once there are more than 2 states. The entity owns
#    it (Information Expert).
# Lands in: Compartment (plus the one isOccupied check in the helper becomes isAvailable).
# 💬 SAY: "This is why occupancy lives on Compartment. It now has 3 states, so
#    the boolean becomes a status enum, and the helper checks isAvailable()."
# ---------------------------------------------------------------------------
enum CompartmentStatus:
    AVAILABLE
    OCCUPIED
    OUT_OF_SERVICE

class Compartment:
    - size: Size
    - status: CompartmentStatus

    + isAvailable() -> boolean        # status == AVAILABLE
    + markOccupied()
    + markAvailable()
    + markOutOfService()
# ⚠️ TRICKY: what happens if a compartment holding a package breaks? The
#    package is still inside. Mention that OUT_OF_SERVICE must not orphan a
#    live token, so staff need to open it via openExpiredCompartments or a
#    similar method.


# ---------------------------------------------------------------------------
# EXT 3: confirm the package was really deposited                  📸 16
# Pattern: TWO-PHASE COMMIT. Reserve → confirm (+ cancel / timeout). Add a
#    RESERVED state.
# Lands in: Locker's deposit API (split into 3 methods) plus one new enum value.
# 💬 SAY: "Today we mark OCCUPIED and issue the code before we know anything is
#    inside. The worst case is a code for an empty box and a compartment we
#    lose. So I'd split deposit into reserve and confirm. Only confirm issues
#    the token. In production, confirm comes from a weight or door sensor; here
#    the driver taps 'confirm'. Reservations auto-cancel after 2-3 minutes."
# ---------------------------------------------------------------------------

class Locker:
    + reserveCompartment(size) -> reservationId
    + confirmDeposit(reservationId) -> tokenCode
    + cancelReservation(reservationId) -> void

class Compartment:
    - size: Size
    - status: CompartmentStatus  // AVAILABLE, RESERVED, OCCUPIED, OUT_OF_SERVICE

enum CompartmentStatus:
    AVAILABLE
    RESERVED
    OCCUPIED
    OUT_OF_SERVICE


reserveCompartment(size)
    compartment = getAvailableCompartment(size)
    if compartment == null
        throw Error("No available compartment")

    compartment.markReserved()
    compartment.open()
    reservationId = generateReservationId()
    reservationMapping[reservationId] = compartment

    return reservationId

confirmDeposit(reservationId)
    compartment = reservationMapping[reservationId]
    if compartment == null
        throw Error("Invalid reservation")

    compartment.markOccupied()
    accessToken = generateAccessToken(compartment)
    accessTokensMapping[accessToken.getCode()] = accessToken
    reservationMapping.remove(reservationId)

    return accessToken.getCode()

cancelReservation(reservationId)          # not written in the video; finishes the pattern
    compartment = reservationMapping[reservationId]
    if compartment == null
        throw Error("Invalid reservation")
    compartment.markAvailable()
    reservationMapping.remove(reservationId)
# ⚠️ TRICKY: without cancel or a timeout, abandoned reservations leak
#    compartments forever. Always mention the timeout.


"""
================================================================================
 MORE FOLLOW-UPS SENIORS GET (not in the video; short answers)
================================================================================
- Brute-force guessing of codes?  -> Long random codes (entropy) plus lockout after
  N failed attempts per kiosk/time window. That's a new small class (an attempt
  counter) checked at the top of pickup().
- Two drivers at the same kiosk at the same moment? -> getAvailableCompartment
  plus markOccupied must be ATOMIC: one lock around "find and mark", or the
  compartment does compare-and-set on its status.
- Many locker locations? -> Add a LockerStation/LockerRegistry ABOVE Locker. Locker
  is unchanged; a code is unique per locker, or prefix it with the locker id.
- Notify the customer when the deposit is confirmed? -> Observer/callback from
  confirmDeposit to the (out-of-scope) notification service. Locker stays unaware of SMS.
- A package bigger than LARGE? -> Validate size at the boundary and throw
  "unsupported size".

================================================================================
 60-SECOND RECAP (say this at the end)
================================================================================
- 3 entities: Locker (orchestrator), Compartment (physical state),
  AccessToken (bearer right with expiry). Package, Driver and User are external.
- Information Expert: the compartment owns occupancy/status. Separation of
  concerns: Locker orchestrates, Token controls access, Compartment holds
  physical state.
- KISS: linear scans, since n ≈ 50-100. The map is there for clarity, not Big-O.
- Specific errors: invalid vs expired. Expired tokens are remembered so the
  late customer gets the right message.
- Extensions land in one place each: fallback = outer loop, broken =
  boolean→enum, deposit guarantee = two-phase commit.
================================================================================
"""
