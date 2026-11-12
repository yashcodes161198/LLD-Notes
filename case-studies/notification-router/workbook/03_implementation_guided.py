"""
WORKBOOK 03: IMPLEMENTATION (GUIDED)             Notification Router
Time box: 12 minutes. Closed book. Pseudo-code is fine; any consistent syntax works.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the class design
    class NotificationRouter:
        - preferences: Map<string, Channel>             # userId -> preferred channel
        - handlers: Map<Channel, ChannelHandler>        # checked complete in the constructor
        + setPreference(userId, channel) -> void | error
        + route(notification) -> Map<Channel, DeliveryStatus> | error
        - resolveChannels(notification) -> Set<Channel> | error
        - dispatch(notification, channels) -> Map<Channel, DeliveryStatus>
    class Notification:   (immutable)
        - id   - userId   - priority: Priority   - content
        + getId()  + getUserId()  + getPriority()  + getContent()  + isUrgent()
    interface ChannelHandler:
        + send(notification) -> void | error
    enum Channel {EMAIL, SMS, PUSH}   enum Priority {NORMAL, URGENT}
    enum DeliveryStatus {SENT, FAILED}

The core logic and edge cases are PRE-FILLED as hints. Write the METHOD BODY
under each "# YOUR CODE". Each core-logic step should become one or two lines.
"""

class NotificationRouter:

    setPreference(userId, channel):
        """
        Core logic:
        1. Validate inputs
        2. Store (overwrite) the user's preferred channel

        Edge cases:
        1. empty userId -> throw
        2. null channel -> throw (a different message from 1)
        3. user already has a preference -> overwrite
        """
        # YOUR CODE                                        [I3-1]
        TODO


    route(notification):
        """
        Core logic:
        1. Validate the notification
        2. Resolve the set of target channels
        3. Dispatch to each channel's handler
        4. Return the per-channel results

        Edge cases:
        1. null notification / empty id / empty userId / null priority -> throw
        2. NORMAL + no preference -> throw (raised by resolveChannels)
        3. a handler throws -> handled in dispatch
        """
        # YOUR CODE                                        [I3-2]
        TODO


    private resolveChannels(notification):
        """
        Core logic:
        1. URGENT -> every channel
        2. NORMAL -> look up the user's preferred channel
        3. Return it as a one-element set

        Edge cases:
        1. NORMAL + no preference -> throw "no preferred channel for user"
        2. URGENT + no preference -> still every channel
        """
        # YOUR CODE                                        [I3-3]
        TODO


    private dispatch(notification, channels):
        """
        Core logic:
        1. For each channel, get its handler
        2. Call send; record SENT
        3. If send throws, record FAILED and keep going
        4. Return the results

        Edge cases:
        1. a handler throws -> FAILED for that channel only; the loop continues
        """
        # YOUR CODE                                        [I3-4]
        TODO


# ---------------------------------------------------------------------------
# Q1. Name two assumptions your code makes about the language or the handlers,
#     and what you'd say to the interviewer about each.       [I3-5]
# TODO
#
# Q2. Every handler fails for one notification. Should route() throw, or
#     return the results? Decide, and give the reason.        [I3-6]
# TODO
#
# Q3. A teammate wrote route() like this instead:
#
#         preferred = preferences[notification.getUserId()]
#         handlers[preferred].send(notification)
#         if notification.isUrgent()
#             for channel in Channel.values()
#                 handlers[channel].send(notification)
#
#     Trace it: u1 prefers SMS; the SMS handler throws "gateway down".
#     (a) route(URGENT for u1) with SMS working, (b) the same with SMS down,
#     (c) route(URGENT for u9, who has no preference).
#     What does each channel receive in each case? What's wrong?   [I3-7]
# TODO
