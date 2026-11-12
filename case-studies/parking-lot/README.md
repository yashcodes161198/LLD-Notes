# Parking Lot: LLD study notes

A Parking Lot low-level design built up over six phases. The notes PDF explains each phase, and each `Phase-N-*` folder holds the complete, compilable Java code for that point in the design.

Source: "Design Parking Lot | LLD, UML, Concurrency & Code Explained" by Shubh Patel (YouTube), and its reference code at github.com/shubhkpatel/Low-Level-Design (`parking_lot` package).

## What's in this folder

```
parking-lot/
├── README.md
├── notes/
│   └── ParkingLot_LLD_Study_Notes.pdf   32-page guide. Start here.
├── phases/
│   ├── Phase-1-Vehicles-and-Spots/
│   ├── Phase-2-Floors-Lot-and-Tickets/
│   ├── Phase-3-Factory-and-Gates/
│   ├── Phase-4-Pricing-Strategy/
│   ├── Phase-5-Payment-Strategy/
│   ├── Phase-6-Concurrency/             the final design
│   └── checkpoints/                    alternative four-checkpoint guide
├── code/                               interview-style Java: Phase 1 core + Phase 2 extensions (see code/README.md)
├── diagrams/                           SVG class and sequence diagrams
└── screenshots/                        reference video frames
```

Each phase folder contains everything from the phase before it plus that phase's new classes. You never need to combine folders.

Open the [study notes](notes/ParkingLot_LLD_Study_Notes.pdf) to begin. The [alternative checkpoint guide](phases/checkpoints/README.md) includes four standalone Java examples and an offline HTML edition. Run the commands below from this `parking-lot/` folder; the implementation snapshots now live under `phases/`.

## How to study with these notes

Do one phase at a time, using this loop (from page 2 of the PDF):

1. **Read** that phase's pages in the PDF: the goal, its UML, the concepts, and the real output.
2. **Sketch** the UML on paper before you look at the diagram.
3. **Retype** the new classes from memory. Start from a copy of the previous phase:
   ```bash
   cp -R phases/Phase-3-Factory-and-Gates my-phase-4
   ```
   Add the new classes under `my-phase-4/src/parkinglot/`. `run.sh` works from any folder, so the copy runs as it is.
4. **Run** your version, then compare it with the real one:
   ```bash
   bash my-phase-4/run.sh
   ```
   ```bash
   diff -ru phases/Phase-4-Pricing-Strategy/src my-phase-4/src
   ```
5. **Answer the checkpoint** questions at the end of that phase in the PDF before you move on.

Retyping teaches more than rereading. Delete your `my-phase-*` copies when you're done.

## Map: phase → PDF pages → code → diagram

| Phase | Topic and patterns | PDF pages | New classes | Diagram |
|---|---|---|---|---|
| 0 | Requirements, UML legend, pattern toolkit | 3–5 | none | `screenshots/01-03` |
| 1 | Vehicles & spots: abstraction, inheritance, enums | 6–7 | `VehicleType`, `Vehicle`, `Car`, `Bike`, `Truck`, `ParkingSpot` | `phase-1-class-diagram.svg` |
| 2 | Floors, lot, ticket: composition, **Singleton**, **Builder** | 8–11 | `ParkingFloor`, `ParkingLot`, `Ticket`, `DateTimeParser` | `phase-2-class-diagram.svg` |
| 3 | Factory & gates: **Simple Factory**, delegation | 12–14 | `VehicleFactory`, `GateType`, `Gate`, `EntryGate`, `ExitGate` | `phase-3-class-diagram.svg` |
| 4 | Pricing: **Strategy**, Open/Closed | 15–18 | `PricingStrategy`, `TimeBasedPricing`, `EventBasedPricing`, `PricingStrategyType`, `PricingStrategyFactory` | `phase-4-class-diagram.svg` |
| 5 | Payment: Strategy again, Dependency Inversion | 19–22 | `PaymentStrategy`, `CashPayment`, `UpiPayment`, `CardPayment`, `PaymentMode`, `PaymentStatus`, `PaymentStrategyFactory`, `PaymentProcessor` | `phase-5-class-diagram.svg` |
| 6 | Concurrency: race conditions, CAS, `ConcurrentHashMap` | 23–27 | `RaceConditionDemo` (`ParkingSpot` and `ParkingLot` made thread-safe) | `phase-6-class-diagram.svg` |

The rest of the PDF:

| Pages | Section | Use it for |
|---|---|---|
| 28–29 | Complete code flow: `parkVehicle` and `unparkVehicle`, step by step | Explaining the design end to end. Matches `diagrams/sequence-*.svg`. |
| 30 | The complete design | Final review. Same as `diagrams/complete-class-diagram.svg`. |
| 31 | Changes from the reference code, and "where to take it next" | Extension ideas to build yourself: many lots, display boards, lost-ticket penalty |
| 32 | Self-test interview questions | Mock interview before the real one |

## Running the code

You need JDK 17 or newer. There's no Maven, Gradle or Lombok, just `javac` and `java`.

Run any phase from this folder:

```bash
bash phases/Phase-1-Vehicles-and-Spots/run.sh
```

Each `run.sh` compiles `src/` into a throwaway `out/` folder and runs `parkinglot.Main`. Phase 6 also runs `RaceConditionDemo`, which shows a plain check-then-act spot being double-booked while the `AtomicBoolean` spot never is:

```bash
bash phases/Phase-6-Concurrency/run.sh
```

To see exactly what one phase changed, diff it against the phase before:

```bash
diff -ru phases/Phase-4-Pricing-Strategy/src phases/Phase-5-Payment-Strategy/src
```

Things to expect in the output:
- Ticket IDs (`T-xxxxxxxx`) are random, so they differ on every run.
- The Phase 6 concurrent-parking lines come out in a different order each run, and the number of double-booked rounds for the naive spot changes too. The checks that matter are fixed: `Cars parked: 2 (expected 2)` and `AtomicSpot double-booked in 0 rounds`.

## Viewing the diagrams

The SVGs open in any browser, or with Quick Look (select the file in Finder and press Space):

```bash
open diagrams/complete-class-diagram.svg
```

In each phase diagram, classes added in that phase are tagged **NEW** and classes that gained members are tagged **UPDATED**. Page 4 of the PDF explains the arrow types (inheritance, realization, composition and so on).

## Before an interview

1. Read the "Explain it in 30 seconds" summary on page 29 aloud.
2. Draw `complete-class-diagram.svg` from a blank page, then compare.
3. Answer the questions on page 32 without looking.
4. Write Phase 6 from an empty folder and run it.

## Related folder

`../parking-lot/` is an earlier take on the same study, split into 7 phases (vehicles, floors and spots, lot and ticket, gates, pricing, payment, concurrency). It has the same classes, but most files differ in their details and there's no PDF to go with it. Use this folder; the PDF's page references and snippets match this code.
