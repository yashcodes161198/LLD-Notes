# LLM usage limits (5-hour + weekly quota): LLD interview case study

Source: designed with the Hello Interview delivery framework (no video). The prompt is modelled on how AI chat products show a "5-hour" and a "weekly" usage limit; every number in the notes is illustrative.

**Problem statement.** Users send requests (userId, model, messages) to an AI chat product. Each user has a 5-hour limit and a weekly limit, measured in usage units = tokens × model weight. A request's cost is only known after the model answers, users can run requests in parallel chats, and they must be blocked only when their usage has actually run out. Design the component that admits, runs and charges these requests.

| Start here | |
|---|---|
| [`llm_usage_limits.py`](llm_usage_limits.py) | The whole interview in one file: requirements, entities, class design, implementation, extensions, with 💬 talking points, ⚠️ tricky spots, ✅ best approaches and 🎯 rubric notes |
| [`RULEBOOK.md`](RULEBOOK.md) | How to fill the workbooks and how the AI grades them |
| [`workbook/`](workbook/) | 01 requirements · 02 entities + class design · 03 implementation (hints) · 04 implementation (blank) · 05 extensibility |
| [`answer-key/`](answer-key/) | Scoring keys, 100 pts each. Don't open them before submitting |
| [`attempts/`](attempts/) | Your dated, filled-in copies plus eval reports |
| [`screenshots/`](screenshots/) | The delivery-framework slide |
| [`code/`](code/README.md) | Runnable Java, Phase 1 core + Phase 2 extensions |

**Study loop:** read `llm_usage_limits.py`, close it, do workbook 01, ask the AI to evaluate it, and repeat for each workbook. Do 04 on a different day from 03.

**The one non-obvious thing:** "check remaining > 0, call the model, then charge" is check-then-act across a call that takes seconds. Ten chats sent at once all pass the check and the overshoot is unbounded. The fix is to **reserve an estimate atomically at admission and settle the real cost in a `finally`**, with the model call outside the lock so chats still run in parallel. See ⚠️ TRICKY #1–#3.
