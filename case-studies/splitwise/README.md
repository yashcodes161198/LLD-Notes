# Splitwise: LLD study notes

A Splitwise low-level design built up over six phases. The notes PDF explains each phase, and each `Phase-N-*` folder holds the complete, compilable Java code for that point in the design.

Source: "Splitwise Low Level Design (LLD) Java | Simplify Debts Algorithm, Strategy Pattern & Code" by Shubh Patel (YouTube), and its reference code at github.com/shubhkpatel/Low-Level-Design (`splitwise` package).

## What's in this folder

```
splitwise/
├── README.md
├── notes/
│   └── Splitwise_LLD_Study_Notes.pdf            38-page guide. Start here.
├── phases/
│   ├── Phase-1-Users-Groups-and-Expenses/
│   ├── Phase-2-Split-Strategy-and-Factory/
│   ├── Phase-3-Balance-Sheet-and-Expense-Service/
│   ├── Phase-4-Repository-and-Group-Service/
│   ├── Phase-5-Simplify-Debts/                  the video's complete design
│   └── Phase-6-Extensions/                     the final design
├── code/                                      interview-sized Java: Phase 1 core + Phase 2 extensions
├── diagrams/                                  SVG class and sequence diagrams
└── screenshots/                               reference video frames
```

Each phase folder contains everything from the phase before it plus that phase's new or changed classes. You never need to combine folders.

Open the [study notes](notes/Splitwise_LLD_Study_Notes.pdf) to begin. Run the commands below from this `splitwise/` folder; the implementation snapshots now live under `phases/`.

## How to study with these notes

Do one phase at a time, using this loop (from page 2 of the PDF):

1. **Read** that phase's pages in the PDF: the goal, the concepts, its UML page, the code and the real output.
2. **Sketch** the UML on paper before you look at the diagram.
3. **Retype** the new classes from memory. Start from a copy of the previous phase:
   ```bash
   cp -R phases/Phase-2-Split-Strategy-and-Factory my-phase-3
   ```
   Add the new classes under `my-phase-3/src/splitwise/`. `run.sh` works from any folder, so the copy runs as it is.
4. **Run** your version, then compare it with the real one:
   ```bash
   bash my-phase-3/run.sh
   ```
   ```bash
   diff -ru phases/Phase-3-Balance-Sheet-and-Expense-Service/src my-phase-3/src
   ```
5. **Answer the checkpoint** questions at the end of that phase in the PDF before you move on.

Delete your `my-phase-*` copies when you're done.

## Map: phase → PDF pages → code → diagram

| Phase | Topic and patterns | PDF pages | New classes | Diagram |
|---|---|---|---|---|
| 0 | The app, requirements, balance sheet, the video's UML, arrow legend, pattern toolkit | 3–7 | none | `screenshots/01-09` |
| 1 | Users, groups, expenses: entities, composition vs aggregation, `List<Split>` | 8–11 | `User`, `Group`, `Expense`, `Split`, `SplitType` | `phase-1-class-diagram.svg` |
| 2 | Split types: **Strategy**, **Simple Factory**, Open/Closed | 12–14 | `SplitStrategy`, `EqualSplitStrategy`, `PercentageSplitStrategy`, `SplitStrategyFactory` | `phase-2-class-diagram.svg` |
| 3 | Balance sheet: two-sided ledger, SRP, constructor injection | 15–19 | `BalanceSheet`, `BalanceSheetService`, `ExpenseService` (`Group` gains balance sheets) | `phase-3-class-diagram.svg` |
| 4 | **Repository**, Dependency Inversion, **Facade**, 1-on-1 expenses | 20–23 | `GroupRepository`, `InMemoryGroupRepository`, `GroupService` | `phase-4-class-diagram.svg` |
| 5 | Simplify debts: net balances, greedy with two heaps, greedy vs DFS | 24–29 | `DebtSimplificationService` | `phase-5-class-diagram.svg` |
| 6 | Extensions: EXACT split, cent-exact equal split, validation, settle up | 30–33 | `ExactSplitStrategy` (`SplitType.EXACT`, `settleUp`, `recordPayment`) | `phase-6-class-diagram.svg` |

The rest of the PDF:

| Pages | Section | Use it for |
|---|---|---|
| 34–35 | Complete code flow: `addExpense` and `simplifyDebts` as sequence diagrams, plus "Explain it in 30 seconds" | Explaining the design end to end. Matches `diagrams/sequence-*.svg`. |
| 36 | The complete design | Final review. Same as `diagrams/complete-class-diagram.svg`. |
| 37 | Differences from the reference code, and "where to take it next" | Extension ideas to build yourself: concurrency, edit/delete, notifications, money types |
| 38 | Self-test interview questions | Mock interview before the real one |

## Running the code

You need JDK 17 or newer. There's no Maven, Gradle or Lombok, just `javac` and `java`.

Run any phase from this folder:

```bash
bash phases/Phase-5-Simplify-Debts/run.sh
```

Each `run.sh` compiles `src/` into a throwaway `out/` folder and runs `splitwise.Main`. The output is the same on every run.

To see exactly what one phase changed, diff it against the phase before:

```bash
diff -ru phases/Phase-4-Repository-and-Group-Service/src phases/Phase-5-Simplify-Debts/src
```

Things to check in the output:
- Phase 3 reproduces the video's balance-sheet example: Shubh paid 60, his own expense was 23.33, Bob owes him 23.33 and Tom owes him 13.33.
- Phase 5 reproduces the video's three simplification examples: Goa trip 2 → 1 payment, the app demo 3 → 2 (Jay owes Shubh 54 and Parth 12), and the A–E graph 7 → 2 (A→C 200, D→E 50). Its fourth demo is a case where greedy needs 4 payments but 3 would do.
- Phase 6 ends with five bad inputs, each rejected with a message.

## Viewing the diagrams

The SVGs open in any browser, or with Quick Look (select the file in Finder and press Space):

```bash
open diagrams/complete-class-diagram.svg
```

In each phase diagram, classes added in that phase are tagged **NEW**, and classes that gained members are tagged **UPDATED** with the new members shaded. Page 6 of the PDF explains the arrow types (inheritance, realization, composition and so on). The handwritten notes use the Gochi Hand web font, so they look plainer offline.

## Before an interview

1. Read the "Explain it in 30 seconds" summary on page 35 aloud.
2. Draw `complete-class-diagram.svg` from a blank page, then compare.
3. Walk through the greedy trace for the A–E graph (page 25) without looking.
4. Answer the questions on page 38 without looking.
5. Write Phase 5 from an empty folder and run it.

## How Phase 5 differs from the reference code

Same 17 classes and the same algorithms. The differences: no Lombok (plain getters and constructors), the package is `splitwise`, balance maps are `LinkedHashMap` so output order is stable, and printing lives in `BalanceSheet.print`. Page 37 of the PDF lists these, plus where the whiteboard UML and the code disagree.
