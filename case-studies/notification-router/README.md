# Notification Router: LLD interview case study

Source: a reported interview problem (no video). Users have a preferred channel (EMAIL / SMS / PUSH); NORMAL notifications go to that channel, URGENT ones to every channel; the senders are out of scope. Designed with the same delivery framework as [Amazon Locker](../amazon-locker/README.md). Level: senior. Language: pseudo-code.

| Start here | |
|---|---|
| [`notification_router.py`](notification_router.py) | The whole interview in one file: the editor content plus 💬 talking points, ⚠️ tricky spots, ✅ best approaches and 🎯 rubric notes |
| [`RULEBOOK.md`](RULEBOOK.md) | How to fill the workbooks and how the AI grades them |
| [`workbook/`](workbook/) | 01 requirements · 02 entities + class design · 03 implementation (hints) · 04 implementation (blank) · 05 extensibility |
| [`answer-key/`](answer-key/) | Scoring keys, 100 pts each. Don't open them before submitting |
| [`attempts/`](attempts/) | Your dated, filled-in copies plus eval reports |
| [`screenshots/`](screenshots/) | The delivery-framework slide (no video for this topic) |

**Study loop:** read `notification_router.py`, close it, do workbook 01, ask the AI to evaluate it, and repeat for each workbook. Do 04 on a different day from 03.

**The one non-obvious thing:** the obvious `route()` ("send to the preferred channel; if URGENT, also loop over all channels") double-sends the preferred channel, and one failing handler aborts the rest of the fan-out. Decide the targets first as a **set**, then dispatch with a **per-channel try/catch** that returns per-channel results. See ⚠️ TRICKY #5.
