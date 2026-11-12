# Amazon Locker: LLD interview case study

Source: Hello Interview, *Design Amazon Locker* (Evan King, 50:55): https://www.youtube.com/watch?v=s6nGkoGJhXk

| Start here | |
|---|---|
| [`amazon_locker.py`](amazon_locker.py) | The whole interview in one file: the original editor file plus 💬 talking points, ⚠️ tricky spots, ✅ best approaches and 🎯 rubric notes |
| [`RULEBOOK.md`](RULEBOOK.md) | How to fill the workbooks and how the AI grades them |
| [`workbook/`](workbook/) | 01 requirements · 02 entities + class design · 03 implementation (hints) · 04 implementation (blank) · 05 extensibility |
| [`answer-key/`](answer-key/) | Scoring keys, 100 pts each. Don't open them before submitting |
| [`attempts/`](attempts/) | Your dated, filled-in copies plus eval reports |
| [`screenshots/`](screenshots/) | 17 frames from the video, numbered by framework step |
| [`code/`](code/) | Runnable Java, Phase 1 core + Phase 2 extensions |
| [`source/`](source/) | The 1080p video, transcript, captions, and the original editor file (`reference/amazon_locker.py`) |

**Study loop:** read `amazon_locker.py`, close it, do workbook 01, ask the AI to evaluate it, and repeat for each workbook. Do 04 on a different day from 03.

**The one non-obvious thing:** the video's `openExpiredCompartments` keeps expired tokens in the live map, which can reopen a *reused* compartment on a later staff run. See ⚠️ TRICKY #6.
