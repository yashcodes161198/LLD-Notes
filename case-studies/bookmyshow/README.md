# BookMyShow: phased LLD study pack

A **55-page study guide**, six independently runnable Java phases, seven growing/final class diagrams, two sequence diagrams, and 16 full-resolution video frames.

Start with the [PDF study notes](notes/BookMyShow_LLD_Study_Notes.pdf). Run the commands below from `case-studies/bookmyshow/`.

Source: [Shubh Patel’s BookMyShow video](https://www.youtube.com/watch?v=dC-lz_GGTL4).

**Repository found:** [shubhkpatel/Low-Level-Design](https://github.com/shubhkpatel/Low-Level-Design). Its current main branch does not contain BookMyShow. The video description says code will be provided later. This pack reconstructs the shown implementation from the full transcript and source frames; it is not an official checkout of a BookMyShow package. See [SOURCES.md](SOURCES.md).

Interview-prep format (Hello Interview framework, booking family with concurrency): [`hello-interview-bookmyshow/`](hello-interview-bookmyshow/README.md).

## Folder map

```text
bookmyshow/
  notes/
    BookMyShow_LLD_Study_Notes.pdf
  hello-interview-bookmyshow/   interview-prep pack (annotated solution, workbooks, keys, Java)
  phases/
    Phase-1-Venues-and-Seats/
    Phase-2-Movies-and-Shows/
    Phase-3-Bookings-and-Payments/
    Phase-4-Expiring-Locks/
    Phase-5-Booking-Flow/
    Phase-6-Safety-Extensions/
  diagrams/              7 class maps + 2 sequence diagrams, SVG
  screenshots/           16 numbered source-video PNGs
  README.md
  SOURCES.md
  VERIFICATION.md
```

Each folder under `phases/` contains `src/bookmyshow/`, `run.ps1`, `run.sh`, `expected-output.txt`, and `PHASE.md`. All earlier source files are carried forward; you never combine phase folders. Build tooling and downloaded video are outside the delivered pack.

## Study loop

1. Read the phase’s concepts and goal.
2. Sketch its UML before opening the diagram. Positions stay fixed as the design grows.
3. Copy the preceding phase into a practice folder and retype the new logic.
4. Run it and compare both source and output.
5. Answer the checkpoint questions aloud before proceeding.

From this folder, using Bash:

```bash
cp -R phases/Phase-3-Bookings-and-Payments my-phase-4
bash my-phase-4/run.sh
diff -ru phases/Phase-4-Expiring-Locks/src my-phase-4/src
```

Windows PowerShell equivalents:

```powershell
Copy-Item phases/Phase-3-Bookings-and-Payments my-phase-4 -Recurse
powershell -ExecutionPolicy Bypass -File .\my-phase-4\run.ps1
git diff --no-index phases/Phase-4-Expiring-Locks/src my-phase-4/src
```

`git diff --no-index` returns exit code 1 when it finds differences; that is expected during practice.

## Phase → PDF → code → UML

| Phase | Topic | PDF pages | New classes / changed pieces | Diagram |
|---|---|---|---|---|
| 1 | Venues and seats | 9-12 | `SeatType`, `ReclinerSeat`, `RegularSeat`, `Screen`, `Seat`, `Theatre` | [UML](diagrams/phase-1-class-diagram.svg) |
| 2 | Movies and shows | 13-18 | `Movie`, `Show`, `MovieRepository`, `ShowRepository`, `TheatreRepository`, `MovieService`, `ShowService`, `TheatreService` | [UML](diagrams/phase-2-class-diagram.svg) |
| 3 | Bookings and payments | 19-24 | `BookingStatus`, `PaymentType`, `SeatStatus`, `PaymentStrategyFactory`, `Booking`, `BookingRepository`, `CardPaymentStrategy`, `PaymentStrategy`, `UpiPaymentStrategy` | [UML](diagrams/phase-3-class-diagram.svg) |
| 4 | Expiring locks | 25-29 | `InMemoryLockProvider`, `LockProvider` | [UML](diagrams/phase-4-class-diagram.svg) |
| 5 | Booking flow and video demos | 30-37 | `SeatNotAvailableException`, `BookingService` | [UML](diagrams/phase-5-class-diagram.svg) |
| 6 | Safety extensions (beyond the video) | 38-47 | `SafetyChecks`; updates to `BookingService`, `BookingRepository`, `LockProvider`, `InMemoryLockProvider` | [UML](diagrams/phase-6-class-diagram.svg) |

The code-card line numbers point to the actual phase files. Highlighted lines are computed from the previous phase’s source. Long classes are split into numbered cards. Packages, imports, repeated setup, and most safety-test harness code are available in the folders even when omitted from PDF cards.

## The rest of the PDF

| Pages | Section |
|---|---|
| 1-2 | Cover, study loop and contents |
| 3-6 | Problem, requirements and worked examples |
| 7-8 | Source whiteboard, arrow legend and pattern toolkit |
| 48-49 | Creation and confirmation sequence diagrams (Phase 6) |
| 50 | Explain it in 30 seconds; expiry trace and complexity |
| 51 | Multiple servers and Redis discussion |
| 52 | Complete final class map |
| 53-54 | Source discrepancies, adaptations and next exercises |
| 55 | Interview self-test and answer cues |

## Run the code

Install JDK 17 or newer and make `javac` and `java` available on PATH. No Maven, Gradle, Lombok, database or network service is required.

```powershell
powershell -ExecutionPolicy Bypass -File .\phases\Phase-6-Safety-Extensions\run.ps1
```

```bash
bash phases/Phase-6-Safety-Extensions/run.sh
```

The scripts compile with `--release 17` into the phase’s `out/` directory and run `bookmyshow.Main`. Payment strategies only print simulated payments. No money is transferred. The provider closes its scheduled cleanup thread when the example finishes.

## What to check in the output

- Phase 1: a regular seat costs INR 150.00; a recliner costs INR 300.00.
- Phase 2: a case-insensitive Interstellar search returns show1 at PVR Phoenix, s1, 18:30-20:30 IST.
- Phase 3: one regular plus one recliner totals INR 450.00. Calling the payment strategy directly leaves the booking CREATED.
- Phase 4: user4 obtains the five-second hold, user5 initially fails, and user5 succeeds at the exact expiry boundary. Another show can hold the same physical seat independently.
- Phase 5: two real threads compete for [3,4] and [4,5]; exactly one obtains the shared seat. User4 loses [6,7] after six seconds, while user5 can pay.
- Phase 6: all demos plus **17 passing safety checks**, including rollback, duplicate confirmation, ownership, cancellation, failed payments, confirmed-seat exclusion, two service instances, and expiry while payment is in progress.

The injected clock advances immediately; you do not have to wait six real seconds. The default lock-provider constructor still supports real time. Winner identity is scheduler-dependent, so Main prints a stable winner count and confirms only after both acquisition attempts finish.

## Beyond the video: Phase 6

The final phase validates requests, rolls back partial acquisition, uses a unique booking-attempt token, releases only matching tokens, records permanently booked show-seat keys, handles a false/throwing mock payment, supports cancellation, and makes confirmation retries idempotent inside one shared repository monitor.

This proof boundary is **one JVM with a shared BookingRepository and LockProvider, a fixed catalog, and synchronous mock payments**. The monitor is intentionally coarse and held during payment. Real network payments need explicit pending state, durable idempotency and recovery; a timeout may mean an unknown outcome. The in-memory records do not survive a restart. Money remains `double` to match the teaching model. No Redis implementation is included.

Phases 1-5 preserve the visible service algorithm’s limitations. In Phase 5, partial holds are not rolled back, `pay()`’s boolean is ignored, and confirmed-seat allocation is not checked during creation. The notes demonstrate why those gaps matter instead of describing the baseline as production-safe.

## Differences from the source presentation

- Source class names use `Theatre`, `BookingRepository`, `CardPaymentStrategy` and `UpiPaymentStrategy`; the whiteboard abbreviates some names.
- Plain constructors/getters replace Lombok. Catalog iteration uses LinkedHashMap; baseline booking storage uses ConcurrentHashMap.
- Date and seat-list defensive copies keep example inputs stable. Models are still trusted internal teaching objects, not secure API DTOs.
- In-process sequential booking IDs, a fixed timestamp, an injected clock and stable logging make examples repeatable.
- The video shows two INR 150 seats in one input but INR 150 in an associated console run. This pack uses the correct INR 300 for the shown two-seat selection and documents the mismatch.
- The unused SeatStatus enum is retained as vocabulary. A Redis provider appears on the board but is discussed only.

## Interview checklist

- Explain why the key contains both show and seat IDs.
- Distinguish a physical seat, a temporary lease and a confirmed allocation.
- Explain atomic per-key compute versus an all-or-nothing seat group.
- Trace an expired attempt, reacquisition, stale confirmation and owner-safe release.
- Explain Strategy versus Simple Factory without calling every switch a SOLID violation.
- Draw the transaction boundary and say what changes across servers.
- Explain what happens when a payment response is lost after charging.
- Rebuild Phase 5 from memory, then justify each Phase 6 change.

Prepared **3 November 2026**. Validation details: [VERIFICATION.md](VERIFICATION.md).
