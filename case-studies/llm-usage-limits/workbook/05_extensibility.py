"""
WORKBOOK 05: EXTENSIBILITY                       LLM Usage Limits
Time box: 3 minutes per question (~20 min total). Closed book.
Rules: see ../RULEBOOK.md. Copy this file into attempts/<YYYY-MM-DD>/ before filling it in.

GIVEN: the finished base design.
    UsageLimiter.handle(request): validate -> estimateUnits -> account.tryReserve
        (atomic: roll windows, check ALL used + reserved < limit, reserve in ALL)
        -> backend.generate (outside the lock) -> finally account.settle(estimate, actual)
    UsageAccount: List<UsageWindow> [FIVE_HOUR, WEEKLY], synchronized per user.
    UsageWindow: limit, duration, used, reserved, resetAt; rollIfExpired keeps reserved.
    estimateUnits = (chars/4 + 100 typical reply tokens) x model weight.

For EACH question, fill in all four parts:
  Pattern:  the reusable design move (name it)
  Lands in: exactly which class(es) and method(s) change; everything else stays the same
  Say:      what you'd say out loud, 2-3 sentences
  Code:     the minimal code delta (pseudo-code). Write "describe only" if you'd
            only talk it through, but you still need the Pattern and Lands-in.
"""

# X1. "Free, Pro and Max users get different limits. And a user can upgrade in
#      the middle of a window."
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X2. "Your estimate is a fixed 100-token reply. Can you do better?"
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X3. "OPUS is expensive. Add a separate weekly limit that only OPUS requests
#      count against."
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X4. "A user opens 500 chats at once to squeeze out extra usage. How do you stop it?"
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X5. "We now run 50 gateway servers behind a load balancer."
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO


# X6. "Answers are streamed. What if the user hits the limit mid-answer?"
# Pattern:  TODO
# Lands in: TODO
# Say:      TODO
# Code:
#   TODO
