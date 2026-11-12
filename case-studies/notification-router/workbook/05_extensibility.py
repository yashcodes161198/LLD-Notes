"""
WORKBOOK 05: EXTENSIBILITY                       Notification Router
Time box: 3 minutes per question (~20 min total). Closed book.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the finished base design. NotificationRouter holds
preferences: Map<userId, Channel> and handlers: Map<Channel, ChannelHandler>
(checked complete in the constructor). route(n) validates, calls
resolveChannels(n) -> Set<Channel> (URGENT: Channel.values(); NORMAL: the
preferred channel, or throw), then dispatch(n, channels), which calls each
handler in a per-channel try/catch and returns Map<Channel, SENT|FAILED>.

For EACH question, fill in all four parts:
  Pattern:  the reusable design move (name it)
  Lands in: exactly which class(es) and method(s) change; everything else stays the same
  Say:      what you'd say out loud, 2-3 sentences
  Code:     the minimal code delta (pseudo-code). Write "describe only" if you'd
            only talk it through, but you still need the Pattern and Lands-in.
"""

# X1. "We're adding WhatsApp as a channel. What changes?"
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X2. "Users want several channels, or different channels per notification
#      type (order updates vs marketing). How?"
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X3. "The preferred channel failed for a NORMAL notification. Can we fall back
#      to another channel? And how do retries work without double-sending?"
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X4. "We're adding a HIGH priority (preferred + PUSH) and quiet hours."
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X5. "Marketing started marking every promo URGENT. How do you stop that?"
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X6. "We now send millions per minute and the SMS provider is slow. What changes?"
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO
