# BookMyShow (seat booking with concurrency): LLD interview case study

Method: Hello Interview delivery framework, from *Design Amazon Locker* (Evan King): https://www.youtube.com/watch?v=s6nGkoGJhXk. There's no video for this topic; the design was built with that framework.

This covers the whole booking family that's commonly reported as the most-repeated LLD prompt: BookMyShow, event ticketing, hotel booking and meeting-room scheduling. Extension 3 shows how the same skeleton becomes a room/time-slot booker.

It's separate from the phased notes in [`../`](../README.md), which follow Shubh Patel's video. This one follows the leaner interview design: one `seatId -> Booking` map and one lock per show.

| Start here | |
|---|---|
| [`bookmyshow.py`](bookmyshow.py) | The whole interview in one file: the editor content plus 💬 talking points, ⚠️ tricky spots, ✅ best approaches and 🎯 rubric notes |
| [`FLOW-WALKTHROUGH.md`](FLOW-WALKTHROUGH.md) | Plain walk through the Java code: setup → hold → confirm, then 5 concurrency/timing cases |
| [`RULEBOOK.md`](RULEBOOK.md) | How to fill the workbooks and how the AI grades them |
| [`workbook/`](workbook/) | 01 requirements · 02 entities + class design · 03 implementation (hints) · 04 implementation (blank) · 05 extensibility |
| [`answer-key/`](answer-key/) | Scoring keys, 100 pts each. Don't open them before submitting |
| [`attempts/`](attempts/) | Your dated, filled-in copies plus eval reports |
| [`screenshots/`](screenshots/) | The delivery-framework slide (`00`) |
| [`code/`](code/README.md) | Runnable Java, Phase 1 core + Phase 2 extensions |

**Study loop:** read `bookmyshow.py`, close it, do workbook 01, ask the AI to evaluate it, and repeat for each workbook. Do 04 on a different day from 03.

**The one non-obvious thing:** a booking's seat list is history, not truth. After user A's hold expires, user B can hold the same seat; if A then cancels and `release` removes every seat in A's list, it frees B's seat and the seat gets sold twice. Release by owner (`seatHolders.remove(seatId, booking)`). See ⚠️ TRICKY #4. Close second: `confirm` must take the same per-show lock as `hold` and read the clock inside it (TRICKY #3).
