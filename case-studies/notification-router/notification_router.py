"""
================================================================================
 NOTIFICATION ROUTER: annotated interview solution
 Source: reported interview problem (no video). Designed with the Hello
         Interview delivery framework; see ../amazon-locker for the video-based
         reference study.
================================================================================

This is the editor file a strong senior candidate would type, with notes added.
The obvious first version of route() has two real bugs; the file shows that
version, explains both bugs, then gives the BEST version (see TRICKY #5).

Reported problem statement (what the interviewer had in mind):
    - Each user has a preferred channel: EMAIL, SMS, or PUSH.
    - Each notification has a priority: NORMAL or URGENT.
    - NORMAL notifications go only to the user's preferred channel.
    - URGENT notifications go to all channels.
    - Implement the routing logic; the channel handlers/senders are out of scope.

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

💬 SAY (at the start): "Before I design anything I'd like a few minutes on
   requirements. I'll go through primary capabilities, error handling and
   scope, then write down what we agree on."
================================================================================


--------------------------------------------------------------------------------
STEP 1: REQUIREMENTS                                  ⏱ ~5 min   📸 00
--------------------------------------------------------------------------------

Design a notification router for an e-commerce site that decides where each notification goes.

# ⚠️ TRICKY: the interviewer may hand you the full bullet list, or only this
#    one sentence. Either way, don't jump to code. The bullets leave real gaps
#    (no preference? one channel fails? who sends?) and finding them IS the
#    "problem analysis" score.
# 🎯 GRADED: Problem analysis.

1. primary capabilities
2. error handling
3. scope boundaries
# ✅ BEST: write the three bucket names first, then fill them in.

Primary capabilities:

- Which channels exist? Is the list fixed?
      -> EMAIL, SMS, PUSH. Fixed for now (adding one is a likely follow-up).
- How does a user's preferred channel get set? Exactly one per user? Can it change?
      -> Exactly one. The user can change it any time; the newest value wins.
- What priorities are there, and what does each mean?
      -> NORMAL: preferred channel only. URGENT: every channel.
- Does the router actually send, or just decide?
      -> It decides and hands off to a per-channel handler. The handlers
         (SMTP, SMS gateway, APNs/FCM) are out of scope.
# ✅ BEST: the "does it send?" question lets the interviewer cut scope. Once
#    they do, you know you need a handler INTERFACE (the seam), not its code.

Error handling:

- What if a NORMAL notification is for a user with no preference on file?
      -> Throw a specific error. Don't guess a channel.
- And an URGENT one for a user with no preference?
      -> Still goes to all channels. URGENT never needs the preference.
- During an URGENT fan-out, what if one channel's handler fails?
      -> The other channels must still get it. Tell the caller which channel
         failed so it can retry just that one.
- What if a channel has no handler registered?
      -> Configuration bug. Fail fast when the router is built.
# 💬 SAY: "Partial failure in the URGENT fan-out is the failure mode I most
#    want pinned down, because it decides what route() returns."
# ⚠️ TRICKY: most candidates never ask about partial failure, then write a
#    plain loop where one exception silently kills the rest of the fan-out.

Scope boundaries:

- Are retries, queues, templates, contact lookup (email address / phone /
  device token) and opt-outs ours?
      -> No. Route one notification in-process and report what happened.


Requirements:
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

# ✅ BEST: each requirement = ACTOR + ACTION + INPUT, with sub-bullets for the
#    reaction and the error. Req 1 -> setPreference, Reqs 2-5 -> route.
# ⚠️ TRICKY: "exactly once each" in Req 4 and "does not stop the others" in
#    Req 5 are the two phrases that catch the bugs later. Say them out loud.

Out of scope:
- Actual delivery: SMTP, SMS gateway, push service (handler implementations)
- Looking up contact details (email address, phone, device token)
- Retries, queues, rate limits, quiet hours, opt-outs / unsubscribe
- Message templates and formatting
# 💬 SAY: "Everything not on this list I'll treat as out of scope."


--------------------------------------------------------------------------------
STEP 2: ENTITIES                                      ⏱ ~3 min   📸 00
--------------------------------------------------------------------------------
# ✅ BEST: list EVERY noun, then prune each one out loud.
#
#   Noun              Keep?  Why
#   ---------------   -----  ---------------------------------------------------
#   E-commerce site    NO    Context. It's the caller, not something we model.
#   Calling service    NO    External actor; it just calls route().
#   User               NO    ⚠️ TRICKY: "most candidates add a User class". The
#                            user service owns the profile. We only need ONE fact:
#                            userId -> preferred channel. That's a map entry on
#                            the router, not a class.
#   Preference         NO    One value per user. A map entry is enough. (It becomes
#                            a class only when it grows rules: multi-channel,
#                            quiet hours. That's an extension.)
#   NotificationRouter YES   The orchestrator / entry point.
#   Notification       YES   The unit we route and report on. It carries the
#                            priority that drives the rule, and the id the caller
#                            uses to retry. Immutable value object.
#   Channel            ENUM  EMAIL, SMS, PUSH.
#   Priority           ENUM  NORMAL, URGENT.
#   Handler / sender   YES*  ⚠️ TRICKY: "handlers are out of scope, so why is it
#                            here?" The IMPLEMENTATIONS are out of scope; the
#                            CONTRACT is not. The router needs a seam to call.
#                            So: an interface, ChannelHandler, with send().
#   Delivery result    ENUM  SENT / FAILED per channel (Req 5).
#
# 💬 SAY: "I'll cut User: the router only needs userId-to-channel, so that's a
#    map. If preferences grow rules later, I'll promote it to a class."
# 🎯 GRADED: Class design (the right entities, and not too many).

Entities:
- NotificationRouter
- Notification
- ChannelHandler (interface)
- (enum) Channel: EMAIL, SMS, PUSH
- (enum) Priority: NORMAL, URGENT
- (enum) DeliveryStatus: SENT, FAILED


--------------------------------------------------------------------------------
STEP 3: CLASS DESIGN, TOP-DOWN                        ⏱ ~10-15 min  📸 00
--------------------------------------------------------------------------------
# ✅ BEST: start with the ORCHESTRATOR and work down.
#       state    = "what must I REMEMBER to enforce the requirements?"
#       behavior = "what user ACTION needs this?"  -> public methods
"""

class NotificationRouter:
    - preferences: Map<string, Channel>                 # Req 1: userId -> preferred channel
    - handlers: Map<Channel, ChannelHandler>            # Req 5: one handler per channel

    + constructor(handlers: Map<Channel, ChannelHandler>)   # Req 5: error if a channel has none
    + setPreference(userId: string, channel: Channel) -> void | error              # Req 1
    + route(notification: Notification) -> Map<Channel, DeliveryStatus> | error   # Req 2-5

    - resolveChannels(notification) -> Set<Channel> | error       # Req 3, 4: DECIDE (pure)
    - dispatch(notification, channels) -> Map<Channel, DeliveryStatus>   # Req 5: SEND

    # ✅ BEST: route() = resolveChannels() (decide) + dispatch() (hand off).
    # 💬 SAY: "I'm splitting the decision from the sending. resolveChannels is
    #    pure: same input, same set, no side effects, so it's trivial to test,
    #    and every routing-rule follow-up will land there and nowhere else."
    # ⚠️ TRICKY: resolveChannels returns a SET, not a list. URGENT includes the
    #    preferred channel; a set makes "exactly once each" (Req 4) structural
    #    instead of something you have to remember.
    # ✅ BEST (where the preference lives): the preference is a RELATIONSHIP
    #    (user -> channel) owned by nobody else in our design, so it lives in
    #    the orchestrator as a map. Same rule as Amazon Locker: intrinsic state
    #    on the entity, relationships in the orchestrator.


class Notification:          # immutable value object, created by the caller
    - id: string             # Req 2: lets the caller retry / correlate results
    - userId: string         # Req 2: recipient
    - priority: Priority     # Req 3, 4: drives the routing rule
    - content: string        # passed through untouched to the handler

    + getId(), getUserId(), getPriority(), getContent()
    + isUrgent() -> boolean  # priority == URGENT

    # 💬 SAY: "Notification knows whether it's urgent (Information Expert), but
    #    it doesn't know the user's preference, so the routing RULE that
    #    combines the two lives on the router."


interface ChannelHandler:    # implementations (EmailHandler, SmsHandler, PushHandler) out of scope
    + send(notification: Notification) -> void | error

    # ✅ BEST: one handler per channel, held in Map<Channel, ChannelHandler>.
    #    The router never says "if EMAIL then SMTP". Adding WhatsApp later is
    #    a new enum value plus one registration; route() doesn't change.
    # ⚠️ TRICKY (alternatives):
    #    BAD:   switch(channel) inside route() that calls SMTP / Twilio directly.
    #           Router now knows every vendor; every new channel edits it.
    #    GOOD:  one big NotificationSender with sendEmail/sendSms/sendPush.
    #           Still edits the class for every channel.
    #    GREAT: a ChannelHandler interface in a map keyed by Channel (this).
    # 🎯 GRADED: Class design (clear boundaries) + Extensibility.


enum Channel:        EMAIL, SMS, PUSH
enum Priority:       NORMAL, URGENT
enum DeliveryStatus: SENT, FAILED


"""
--------------------------------------------------------------------------------
STEP 4: IMPLEMENTATION                                ⏱ ~10 min   📸 00
--------------------------------------------------------------------------------
# 💬 SAY: "Pseudo-code or a real language?" Then: "Notification is getters and
#    handlers are out of scope, so I'll implement the router: setPreference,
#    route, and its two helpers resolveChannels and dispatch."
#
# ✅ BEST: for EVERY method, write the docstring FIRST:
#        Core logic: numbered steps
#        Edge cases: walk each step asking "what can go wrong here?"
#    Then the code, one or two lines per step.
# 🎯 GRADED: Code quality plus Communication.
"""

# Implementation
# 1. Define the core logic
# 2. Consider the edge cases

class NotificationRouter:

    constructor(handlers)
        """
        Core logic:
        1. Check every Channel has a handler
        2. Store the handlers
        Edge cases:
        1. a channel with no handler -> throw "no handler for channel"
        """
        for channel in Channel.values()
            if handlers[channel] == null
                throw Error("no handler for channel " + channel)
        this.handlers = handlers
        this.preferences = {}
        # ✅ BEST: fail fast at startup, not at 3 a.m. on the first URGENT.
        # 💬 SAY: "Now dispatch can assume every channel has a handler."


    setPreference(userId, channel)
        """
        Core logic:
        1. Validate inputs
        2. Store (overwrite) the user's preferred channel
        Edge cases:
        1. empty userId -> throw "invalid user"
        2. null channel -> throw "invalid channel"
        3. user already has one -> overwrite (Req 1: newest wins)
        """
        if userId.isEmpty()
            throw Error("invalid user")
        if channel == null
            throw Error("invalid channel")
        preferences[userId] = channel


    route(notification)
        """
        Core logic:
        1. Validate the notification
        2. Resolve the set of target channels
        3. Dispatch to each channel's handler
        4. Return the per-channel results
        Edge cases:
        1. null notification / empty id / empty userId / null priority -> throw "invalid notification"
        2. NORMAL + no preference -> throw "no preferred channel" (from resolveChannels)
        3. a handler throws -> that channel FAILED, the rest still run (in dispatch)
        4. every handler fails -> return all FAILED???  decision: return, don't throw
        """
        if notification == null || notification.getId().isEmpty()
                || notification.getUserId().isEmpty() || notification.getPriority() == null
            throw Error("invalid notification")

        channels = resolveChannels(notification)
        return dispatch(notification, channels)

        # 💬 SAY (the product trade-off): "If every channel fails, should route()
        #    throw? I'd rather return all FAILED. The caller then has one shape
        #    to handle, and a retry loop that works per channel. Throwing would
        #    hide WHICH channels failed." Confirm with the interviewer.
        # 🎯 GRADED: Communication (you raise the decision instead of picking silently).
        # ⚠️ TRICKY: all validation happens BEFORE anything is dispatched. A
        #    half-sent notification followed by "invalid notification" is the
        #    worst of both worlds.


    private resolveChannels(notification)
        """
        Core logic:
        1. URGENT -> every channel
        2. NORMAL -> look up the user's preferred channel
        3. Return it as a one-element set
        Edge cases:
        1. NORMAL + no preference -> throw "no preferred channel for user"
        2. URGENT + no preference -> still every channel???  decision: yes
        """
        if notification.isUrgent()
            return Set(Channel.values())
        preferred = preferences[notification.getUserId()]
        # 💬 SAY: "In my pseudo-code a map miss returns null."
        if preferred == null
            throw Error("no preferred channel for user")
        return Set(preferred)

        # ✅ BEST: check URGENT FIRST, so a missing preference can never block
        #    an urgent message (fraud alert, payment failed). That's a product
        #    decision; say it out loud.
        # ⚠️ TRICKY: "all channels" = Channel.values(), the domain list, not
        #    handlers.keys(). They're equal only because the constructor checked.
        #    Using the enum states the intent ("every channel we support").


    private dispatch(notification, channels)
        """
        Core logic:
        1. For each channel, get its handler
        2. Call send; record SENT
        3. If send throws, record FAILED and keep going
        4. Return the results
        Edge cases:
        1. a handler throws -> FAILED for that channel only, the loop continues
        2. empty set -> empty map (can't happen; resolveChannels never returns empty)
        """
        results = Map<Channel, DeliveryStatus>
        for channel in channels
            try
                handlers[channel].send(notification)
                results[channel] = SENT
            catch error
                results[channel] = FAILED      # log(error, notification.getId(), channel)
        return results


    # ⚠️ TRICKY #5 (the trap; spotting it is the SENIOR signal)
    # ---- AS WRITTEN (the version most candidates type first) ----
    route(notification)
        preferred = preferences[notification.getUserId()]
        handlers[preferred].send(notification)
        if notification.isUrgent()
            for channel in Channel.values()
                handlers[channel].send(notification)

    # It reads like the requirements ("send to preferred; if urgent, send to all"),
    # and it has TWO bugs:
    #   Bug A, DUPLICATE: an URGENT for a user who prefers SMS sends SMS twice
    #          (once as preferred, once in the loop). Req 4 says "exactly once".
    #   Bug B, PARTIAL FAILURE: if the SMS handler throws, the exception escapes.
    #          EMAIL and PUSH are never tried, and the caller can't tell what went
    #          out. If it retries the whole route(), channels that DID succeed
    #          get it again.
    #   Bonus bug: URGENT for a user with no preference crashes on
    #          handlers[null] before the fan-out even starts.
    #
    # ✅ BEST FIX (the version above): DECIDE first, as a SET (fixes A and the
    #    bonus bug), then SEND with a per-channel try/catch that records each
    #    outcome (fixes B). The caller retries only the FAILED channels.
    # 💬 SAY: "If I send to the preferred channel and then loop over all of
    #    them, URGENT double-sends. So I compute the target set first, then
    #    dispatch, and each channel's failure is isolated and reported."


"""
VERIFICATION TRACE (spend 1-2 min on this; it catches bugs out loud)
    handlers = {EMAIL: e, SMS: s, PUSH: p}; s.send throws "gateway down"
    setPreference("u1", SMS)
    setPreference("u2", EMAIL)
    route(n1: u2, NORMAL)  -> channels {EMAIL}           -> {EMAIL: SENT}
    route(n2: u1, NORMAL)  -> channels {SMS}             -> {SMS: FAILED}       (no exception)
    route(n3: u1, URGENT)  -> channels {EMAIL, SMS, PUSH} -> {EMAIL: SENT, SMS: FAILED, PUSH: SENT}
                              SMS tried exactly once; EMAIL and PUSH still went out
    route(n4: u9, NORMAL)  -> throw "no preferred channel for user"; nothing sent
    route(n5: u9, URGENT)  -> all three tried (no preference needed)
    route(n6: "", URGENT)  -> throw "invalid notification"; nothing sent
    setPreference("u1", PUSH); route(n7: u1, NORMAL) -> {PUSH: SENT}  (newest wins)
"""


"""
--------------------------------------------------------------------------------
STEP 5: EXTENSIBILITY                                 ⏱ ~5 min   📸 00
--------------------------------------------------------------------------------
# ✅ BEST: say WHERE the change lands (one method or one class), describe it,
#    and code it only if asked. A good design absorbs each change in ONE place.
# 🎯 GRADED: Extensibility (small changes, no rewrite).
"""

# Extensions
"""
1. "We're adding WhatsApp (or in-app) as a channel. What changes?"
2. "Users want several channels, or different channels per notification type
    (order updates vs marketing). How?"
3. "The preferred channel failed for a NORMAL notification. Can we fall back
    to another channel, and how do retries work?"
"""

# ---------------------------------------------------------------------------
# EXT 1: a new channel
# Pattern: OPEN/CLOSED via the handler map (each channel is a plug-in).
# Lands in: the Channel enum + one handler registered at construction.
#    route, resolveChannels and dispatch are unchanged.
# 💬 SAY: "Add WHATSAPP to the enum and register a WhatsAppHandler. URGENT picks
#    it up automatically because it iterates Channel.values(). If someone
#    forgets the handler, the constructor fails at startup."
# ---------------------------------------------------------------------------
enum Channel: EMAIL, SMS, PUSH, WHATSAPP
router = NotificationRouter({EMAIL: e, SMS: s, PUSH: p, WHATSAPP: w})
# ⚠️ TRICKY (senior bonus): "Should URGENT really go to EVERY channel forever?
#    WhatsApp costs money per message." That's a product question. If the answer
#    is "not WhatsApp", URGENT needs its own channel list, which is Ext 4.


# ---------------------------------------------------------------------------
# EXT 2: multiple / per-type preferences
# Pattern: PROMOTE the map value to a class once it carries rules.
#    Single value -> Set<Channel>; per-type -> Map<NotificationType, Set<Channel>>.
# Lands in: the preferences map's value type + resolveChannels's NORMAL branch.
#    dispatch already takes a set, so it doesn't change.
# 💬 SAY: "This is why resolveChannels returns a set. NORMAL now returns the
#    user's set for that notification type; dispatch already loops a set."
# ---------------------------------------------------------------------------
class UserPreference:
    - channelsByType: Map<NotificationType, Set<Channel>>
    - defaultChannels: Set<Channel>
    + channelsFor(type) -> Set<Channel>     # falls back to defaultChannels

# resolveChannels, NORMAL branch:
    pref = preferences[notification.getUserId()]
    if pref == null || pref.channelsFor(notification.getType()).isEmpty()
        throw Error("no preferred channel for user")
    return pref.channelsFor(notification.getType())
# ⚠️ TRICKY: per-type preferences bring OPT-OUT (marketing: none). Ask whether
#    URGENT may override an opt-out. Usually only transactional/security ones.


# ---------------------------------------------------------------------------
# EXT 3: fallback + retries
# Pattern: ORDERED FALLBACK LIST (one loop, stop at the first success) for
#    NORMAL; for retries, the per-channel result IS the retry contract.
# Lands in: dispatch's NORMAL path (or a small sendWithFallback helper).
#    resolveChannels and URGENT are unchanged.
# 💬 SAY: "For NORMAL I'd try the preferred channel, then the rest in a fixed
#    order, and stop at the first success. URGENT already tries everything.
#    For retries: the caller re-submits only the FAILED channels, and handlers
#    dedupe on (notificationId, channel) so a retry can't double-send."
# ---------------------------------------------------------------------------
FALLBACK_ORDER = [PUSH, EMAIL, SMS]       # explicit list, not enum declaration order

private sendWithFallback(notification, preferred)
    ordered = [preferred] + FALLBACK_ORDER.without(preferred)
    results = Map<Channel, DeliveryStatus>
    for channel in ordered
        try
            handlers[channel].send(notification)
            results[channel] = SENT
            return results                 # stop at the first success
        catch error
            results[channel] = FAILED
    return results                          # all FAILED
# ⚠️ TRICKY: "SMS fell back to EMAIL" is a product decision (users chose SMS for
#    a reason). Ask before building it. And a handler timeout is NOT proof of
#    failure: the SMS may have gone out. That's why the idempotency key matters.


# ---------------------------------------------------------------------------
# EXT 4: more priorities / richer rules (HIGH = preferred + PUSH, quiet hours)
# Pattern: STRATEGY. Map<Priority, RoutingPolicy>, each policy returns a Set<Channel>.
# Lands in: resolveChannels only (it becomes a one-line lookup).
# 💬 SAY: "With two priorities an if is clearer. Once there's a third, or rules
#    like quiet hours, each priority gets a RoutingPolicy and resolveChannels
#    just picks the policy."
# ---------------------------------------------------------------------------
interface RoutingPolicy:
    + channels(notification, preferredChannel) -> Set<Channel>

policies = {NORMAL: PreferredOnly(), HIGH: PreferredPlusPush(), URGENT: AllChannels()}

private resolveChannels(notification)
    return policies[notification.getPriority()].channels(notification, preferences[notification.getUserId()])
# ⚠️ TRICKY: don't introduce Strategy in the base design for 2 priorities.
#    Interviewers read that as pattern-showing-off (KISS / YAGNI).


"""
================================================================================
 MORE FOLLOW-UPS SENIORS GET (short answers)
================================================================================
- Marketing marks everything URGENT to get through? -> URGENT is a privilege:
  an allow-list of caller services that may send it, plus a per-user cap on
  URGENT per hour. Checked at the top of route(); nothing else changes.
- route() called from many threads; setPreference at the same time? -> preferences
  becomes a concurrent map. resolveChannels reads the preference ONCE into a local,
  so one notification never sees half an update. Handlers must be thread-safe.
- Millions per minute, slow SMS provider? -> route() stays the decision point but
  dispatch publishes (notification, channel) to a per-channel queue. Handlers
  become consumers with their own retries and backoff, deduping on
  (notificationId, channel). The result becomes ENQUEUED instead of SENT.
- A channel the user can't receive on (no phone number on file)? -> The handler
  reports FAILED (or a new SKIPPED status). Contact lookup stays out of the router.
- Audit "what did we send to whom"? -> log the results map per notification id;
  it already has exactly the right shape.

================================================================================
 60-SECOND RECAP (say this at the end)
================================================================================
- Entities: NotificationRouter (orchestrator), Notification (immutable value
  with priority), ChannelHandler (interface; implementations out of scope),
  plus Channel / Priority / DeliveryStatus enums. User is not a class: the
  router keeps a userId -> channel map.
- route = resolveChannels (decide, pure, returns a SET) + dispatch (send, per-
  channel try/catch, returns per-channel results).
- The trap: "send preferred, then if urgent send all" double-sends and lets one
  failing handler kill the fan-out. Set + isolated failures fixes both.
- Decisions: URGENT never needs a preference; NORMAL without one throws a
  specific error; all-failed returns results instead of throwing; missing
  handler fails at construction.
- Extensions land in one place each: new channel = enum + handler; multi/per-
  type prefs = map value becomes a class; fallback = ordered list loop; more
  priorities = Strategy in resolveChannels.
================================================================================
"""
