# Source and reconstruction record

Checked on 3 November 2026.

- Video: https://www.youtube.com/watch?v=dC-lz_GGTL4
- Title: I Solved BookMyShow Seat Clash! Full LLD + Lock Expiry + Why Redis is Must
- Creator: Shubh Patel; runtime 45:26.
- Full English auto-caption transcript was read. The video and metadata were retrieved with yt-dlp 2026.10.26; every included frame was visually inspected.
- Author repository: https://github.com/shubhkpatel/Low-Level-Design
- Inspected snapshot: fd753ebd1fc03b6a499c8671151f62ed7dc13c05. The current tree and README contain no BookMyShow package. Repository code search also returned no BookMyShow match. The video description says notes/code will be provided after the series.
- The visible IDE project is named object_oriented_design with package org.lld_yt.bookmyshow. This is evidence of the shown local project, not a verified public repository URL.
- Java code in this pack is reconstructed from source frames and narration. Unseen bodies and constructors are educational reconstructions. Phase 6 is an original extension, not attributed to the presenter.

## Primary technical references

- Java 17 ConcurrentHashMap: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html
- Redis distributed locks and safe token release: https://redis.io/docs/latest/develop/clients/patterns/distributed-locks/

## Screenshot index

| File | Timestamp | Source |
|---|---|---|
| screenshots/01-show-list.png | 01:00 | [Video frame](https://www.youtube.com/watch?v=dC-lz_GGTL4&t=60s) |
| screenshots/02-seat-selection.png | 02:00 | [Video frame](https://www.youtube.com/watch?v=dC-lz_GGTL4&t=120s) |
| screenshots/03-payment-timer.png | 03:00 | [Video frame](https://www.youtube.com/watch?v=dC-lz_GGTL4&t=180s) |
| screenshots/04-extensible-requirements.png | 04:40 | [Video frame](https://www.youtube.com/watch?v=dC-lz_GGTL4&t=280s) |
| screenshots/05-concurrency-requirements.png | 05:40 | [Video frame](https://www.youtube.com/watch?v=dC-lz_GGTL4&t=340s) |
| screenshots/06-whiteboard-overview.png | 17:00 | [Video frame](https://www.youtube.com/watch?v=dC-lz_GGTL4&t=1020s) |
| screenshots/07-venue-and-seats.png | 09:30 | [Video frame](https://www.youtube.com/watch?v=dC-lz_GGTL4&t=570s) |
| screenshots/08-show-and-movie.png | 10:00 | [Video frame](https://www.youtube.com/watch?v=dC-lz_GGTL4&t=600s) |
| screenshots/09-payment-strategy.png | 11:05 | [Video frame](https://www.youtube.com/watch?v=dC-lz_GGTL4&t=665s) |
| screenshots/10-lock-contract.png | 12:40 | [Video frame](https://www.youtube.com/watch?v=dC-lz_GGTL4&t=760s) |
| screenshots/11-booking-model.png | 15:10 | [Video frame](https://www.youtube.com/watch?v=dC-lz_GGTL4&t=910s) |
| screenshots/12-atomic-compute.png | 35:35 | [Video frame](https://www.youtube.com/watch?v=dC-lz_GGTL4&t=2135s) |
| screenshots/13-create-booking.png | 39:45 | [Video frame](https://www.youtube.com/watch?v=dC-lz_GGTL4&t=2385s) |
| screenshots/14-confirm-booking.png | 41:30 | [Video frame](https://www.youtube.com/watch?v=dC-lz_GGTL4&t=2490s) |
| screenshots/15-demo-output.png | 25:00 | [Video frame](https://www.youtube.com/watch?v=dC-lz_GGTL4&t=1500s) |
| screenshots/16-distributed-locks.png | 44:50 | [Video frame](https://www.youtube.com/watch?v=dC-lz_GGTL4&t=2690s) |


Source screenshots reproduce the presenter’s view, including any source-level cropping. The final complete diagram is recreated as a vector SVG. Frames 13-14 are evidence for the booking-service gaps discussed in the guide; frame 15 is the demonstrated output.

## Tooling and format

The supplied Splitwise uml.py, seq.py and build_pdf.py were copied to the task scratch directory and adapted. The older macOS study-pack folders were unavailable on this Windows machine. Brave-specific skill tooling was unavailable; web access and local yt-dlp downloads were used. Fonts are Fraunces, Inter, JetBrains Mono and Gochi Hand from Google Fonts, embedded in the PDF and diagrams. Windows and Bash launchers are included.

The environment exposed no writable persistent-memory tool, and the named lld-study-pack-format memory was not found locally. The format entry is recorded in VERIFICATION.md for reuse; no claim is made that an external memory store was updated.
