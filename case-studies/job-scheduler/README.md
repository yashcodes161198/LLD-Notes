# Job Scheduler: LLD interview case study

Source: a commonly reported interview problem (no video). Clients schedule one-off or recurring jobs; the scheduler runs due jobs when ticked, retries failures per the job's delivery guarantee (AT_MOST_ONCE / AT_LEAST_ONCE), and supports cancel and status queries. Designed with the same delivery framework as [Amazon Locker](../amazon-locker/README.md). Level: senior. Language: pseudo-code.

| Start here | |
|---|---|
| [`job_scheduler.py`](job_scheduler.py) | The whole interview in one file: the editor content plus 💬 talking points, ⚠️ tricky spots, ✅ best approaches and 🎯 rubric notes |
| [`RULEBOOK.md`](RULEBOOK.md) | How to fill the workbooks and how the AI grades them |
| [`workbook/`](workbook/) | 01 requirements · 02 entities + class design · 03 implementation (hints) · 04 implementation (blank) · 05 extensibility |
| [`answer-key/`](answer-key/) | Scoring keys, 100 pts each. Don't open them before submitting |
| [`attempts/`](attempts/) | Your dated, filled-in copies plus eval reports |
| [`screenshots/`](screenshots/) | The delivery-framework slide (no video for this topic) |
| [`code/`](code/README.md) | Runnable Java, Phase 1 core + Phase 2 extensions |

**Study loop:** read `job_scheduler.py`, close it, do workbook 01, ask the AI to evaluate it, and repeat for each workbook. Do 04 on a different day from 03.

**The one non-obvious thing:** retries belong to a **run**, not to the job. The obvious version keeps one `nextRunAt` and one lifetime `attempts` counter, so a retry silently moves the recurring schedule (09:00 becomes 09:00:30), and the counter, which also counts successes, leaves the job with no retries by its third run and then kills the schedule with FAILED. Keep `scheduledFor` (the slot, also the execution key) apart from `nextRunAt` (the next try), compute the next slot from `scheduledFor`, and reset attempts per slot. See ⚠️ TRICKY #5.
