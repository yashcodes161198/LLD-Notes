"""
WORKBOOK 04: IMPLEMENTATION (NO HINTS)           Notification Router
Time box: 12 minutes. Closed book. Do this on a DIFFERENT day from workbook 03.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the class design (same as workbook 03)
    class NotificationRouter:
        - preferences: Map<string, Channel>
        - handlers: Map<Channel, ChannelHandler>
        + constructor(handlers)
        + setPreference(userId, channel) -> void | error
        + route(notification) -> Map<Channel, DeliveryStatus> | error
    class Notification:   id, userId, priority, content | getters, isUrgent()
    interface ChannelHandler:   send(notification) -> void | error
    enum Channel {EMAIL, SMS, PUSH}   enum Priority {NORMAL, URGENT}
    enum DeliveryStatus {SENT, FAILED}

For EACH method:
  1. Write the docstring yourself: "Core logic:" (numbered steps) and
     "Edge cases:" (condition -> what happens).
  2. Then write the body so it follows your steps in order.
Add any private helper you need, and say why.
"""

class NotificationRouter:

    constructor(handlers):
        """
        Core logic:
        TODO
        Edge cases:
        TODO
        """
        TODO                                               # [I4-1]


    setPreference(userId, channel):
        """
        Core logic:
        TODO
        Edge cases:
        TODO
        """
        TODO                                               # [I4-2]


    route(notification):
        """
        Core logic:
        TODO
        Edge cases:
        TODO
        """
        TODO                                               # [I4-3]


    # helper(s) TODO                                       # [I4-4]


# ---------------------------------------------------------------------------
# Q1. Write the 2-3 sentence product trade-off you'd raise with the
#     interviewer about an URGENT notification for a user with no
#     preference on file.                                     [I4-5]
# TODO
#
# Q2. Prove in 1-2 lines that your route() sends an URGENT notification to
#     each channel EXACTLY once, and that one failing handler can't stop the
#     others.                                                 [I4-6]
# TODO
#
# Q3. Write a 4-line verification trace (calls, then the result map) that
#     covers one happy path, one partial failure and one error. [I4-7]
# TODO
