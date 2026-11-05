# Rate Limiter: LLD study notes

A per-tier, thread-safe rate limiter built up over six phases. The notes PDF explains each phase, and each `Phase-N-*` folder holds the complete, compilable Java code for that point in the design.

Source: "Design Rate Limiter (LLD) - Token Bucket, Fixed & Sliding Window with Thread Safety" by Shubh Patel (YouTube, 32 min), and its reference code at github.com/shubhkpatel/Low-Level-Design (`rate_limiter` package).

## What's in this folder

```
rate-limiter/
├── README.md
├── notes/
│   └── RateLimiter_LLD_Study_Notes.pdf          41-page guide. Start here.
├── phases/
│   ├── Phase-1-Users-Tiers-and-Limits/
│   ├── Phase-2-RateLimiter-and-Fixed-Window/
│   ├── Phase-3-Sliding-Log-and-Token-Bucket/
│   ├── Phase-4-Factory-and-RateLimiter-Service/
│   ├── Phase-5-Thread-Safety/                   the video's complete design
│   └── Phase-6-Extensions/                     the final design
├── diagrams/                                  SVG class and sequence diagrams
└── screenshots/                               reference video frames
```

Each phase folder contains everything from the phase before it plus that phase's new or changed classes. You never need to combine folders.

Open the [study notes](notes/RateLimiter_LLD_Study_Notes.pdf) to begin. Run the commands below from this `rate-limiter/` folder; the implementation snapshots now live under `phases/`.

## How to study with these notes

Do one phase at a time, using this loop (from page 2 of the PDF):

1. **Read** that phase's pages in the PDF: the goal, the concepts, its UML page, the code and the real output.
2. **Sketch** the UML on paper before you look at the diagram.
3. **Retype** the new classes from memory. Start from a copy of the previous phase:
   ```bash
   cp -R phases/Phase-2-RateLimiter-and-Fixed-Window my-phase-3
   ```
   Add the new classes under `my-phase-3/src/ratelimiter/`. `run.sh` works from any folder, so the copy runs as it is.
4. **Run** your version, then compare it with the real one:
   ```bash
   bash my-phase-3/run.sh
   ```
   ```bash
   diff -ru phases/Phase-3-Sliding-Log-and-Token-Bucket/src my-phase-3/src
   ```
5. **Answer the checkpoint** questions at the end of that phase in the PDF before you move on.

Delete your `my-phase-*` copies when you're done.

## Map: phase → PDF pages → code → diagram

| Phase | Topic and patterns | PDF pages | New classes | Diagram |
|---|---|---|---|---|
| 0 | LeetCode's limiter, requirements, the four algorithms, the video's UML, arrow legend, pattern toolkit | 3–8 | none | `screenshots/01-13` |
| 1 | Users, tiers and limits: entities, enums, immutable config | 9–11 | `User`, `UserTier`, `RateLimitConfig` | `phase-1-class-diagram.svg` |
| 2 | The contract and the first algorithm: abstract class, window index, per-user state | 12–15 | `RateLimiter` (abstract), `FixedWindowRateLimiter`, `RateLimitType` | `phase-2-class-diagram.svg` |
| 3 | Two more algorithms: **Strategy** via inheritance, polymorphism, refill rate | 16–19 | `SlidingWindowLogRateLimiter`, `TokenBucketRateLimiter` | `phase-3-class-diagram.svg` |
| 4 | **Simple Factory**, per-tier routing, the video's demo | 20–23 | `RateLimiterFactory`, `RateLimiterService` | `phase-4-class-diagram.svg` |
| 5 | Thread safety: `ConcurrentHashMap.compute` per-user locking, `AtomicBoolean`, race tests | 24–29 | none (the three limiters are rewritten; `Main` gains the race tests) | `phase-5-class-diagram.svg` |
| 6 | Extensions: injected `Clock`, two bug fixes, the sliding window counter, config validation | 30–35 | `ManualClock`, `SlidingWindowCounterRateLimiter` | `phase-6-class-diagram.svg` |

The rest of the PDF:

| Pages | Section | Use it for |
|---|---|---|
| 36–37 | Complete code flow: `allowRequest` and two threads racing for the last token, as sequence diagrams, plus "Explain it in 30 seconds" | Explaining the design end to end. Matches `diagrams/sequence-*.svg`. |
| 38 | The complete design | Final review. Same as `diagrams/complete-class-diagram.svg`. |
| 39 | Differences from the reference code, where the whiteboard and code disagree, and the bugs fixed in Phase 6 | Knowing what the video gets wrong |
| 40 | Where to take it next | Extension ideas to build yourself: Redis for many servers, idle-user eviction, leaky bucket, 429 headers |
| 41 | Self-test interview questions | Mock interview before the real one |

## Running the code

You need JDK 17 or newer. There's no Maven, Gradle or Lombok, just `javac` and `java`.

Run any phase from this folder:

```bash
bash phases/Phase-5-Thread-Safety/run.sh
```

Each `run.sh` compiles `src/` into a throwaway `out/` folder and runs `ratelimiter.Main`. The output is the same on every run. Phases 2 and 3 sleep for a few seconds on purpose, to show time passing; the others finish in under a second.

To see exactly what one phase changed, diff it against the phase before. This one shows everything thread safety adds:

```bash
diff -ru phases/Phase-4-Factory-and-RateLimiter-Service/src phases/Phase-5-Thread-Safety/src
```

Things to check in the output:
- Phase 2 ends with `request 4: BLOCKED` after a new window has started. That is a real bug in the reference code, left in on purpose; Phase 6 explains and fixes it.
- Phase 3 runs one burst through three algorithms at t = 0, 1.2 and 3.6 s: the sliding log recovers fully at 3.6 s, the token bucket one token per second, and the fixed window never (the same bug).
- Phase 4 reproduces the video's demo: the free user gets requests 1–10 and is blocked for 11–15; the premium user gets 1–100 and is blocked for 101–120.
- Phase 5 reproduces the video's concurrency check (20 simultaneous requests, exactly 10 allowed) and repeats the race 500 times per algorithm, 500/500 each.
- Phase 6 replays everything on a `ManualClock`: both bug fixes, a table of the same traffic through all four algorithms (fixed window lets 20 through between t=50 and t=60), 500/500 races including the new algorithm, and three rejected inputs.

## Viewing the diagrams

The SVGs open in any browser, or with Quick Look (select the file in Finder and press Space):

```bash
open diagrams/complete-class-diagram.svg
```

In each phase diagram, classes added in that phase are tagged **NEW**, and classes that gained or changed members are tagged **UPDATED** with those members shaded. Page 7 of the PDF explains the arrow types. The algorithm boxes show the map type actually created (`HashMap` in Phases 2–4, `ConcurrentHashMap` from Phase 5), because that is what Phase 5 changes. The handwritten notes use the Gochi Hand web font, so they look plainer offline.

## Before an interview

1. Read the "Explain it in 30 seconds" summary on page 37 aloud.
2. Draw `complete-class-diagram.svg` from a blank page, then compare.
3. Explain the fixed-window boundary burst and the sliding window counter formula (pages 5–6) without looking.
4. Walk through the two-thread race on page 37: why `compute` makes exactly one request win.
5. Answer the questions on page 41 without looking.
6. Write Phase 5 from an empty folder and make its 500 races pass.

## How the code differs from the reference

Phase 5 has the same 11 classes and the same algorithms as the reference. The differences: no Lombok (plain getters and constructors), the package is `ratelimiter`, a few stray comments are gone, and `Main` prints counts plus a 500-race test instead of one line per thread in random order. Phases 2–4 use single-threaded versions of the same algorithms, so the diff into Phase 5 shows only what thread safety adds.

Phase 6 goes beyond the video. It fixes two bugs in the reference (the fixed window never resets for a new user; the token bucket loses partial refill time), makes `windowStart` a `ConcurrentHashMap`, counts in milliseconds, adds the sliding window counter from the requirements, and validates `RateLimitConfig`. Page 39 of the PDF lists all of this, plus where the whiteboard and the code disagree.
