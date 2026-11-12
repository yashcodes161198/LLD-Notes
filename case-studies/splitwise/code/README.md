# Splitwise: Java code

Two runnable versions of the Splitwise interview design. Run either phase with `./run.sh` (JDK 17+). `Main` is a set of self-checks: it prints each step and throws if anything is wrong. Phase 2's notifier checks also print `[warn]` lines to stderr; that's expected.

This is the interview-sized design (one orchestrator, ledger in the group, money in cents). The video's six-phase build with a factory and repository is in [`../phases/`](../phases/).

```
Phase-N/src/splitwise/
  model/      SplitType, Split, Expense, Transfer, Group (owns members, expenses, net balances)
  service/    SplitwiseService (orchestrator) + GroupNotFound, NotAGroupMember, InvalidSplit,
              DuplicateExpense (+ ExpenseNotFound in Phase 2) exceptions
  strategy/   SplitStrategy + Equal, Exact, Percent, Shares strategies   (Phase 2)
  notifier/   Notifier (the seam) + EmailNotifier, PushNotifier stubs    (Phase 2)
  Main.java   self-checks
```

## Phase 1: Core Requirements

1. Create a group of members. 2. A member adds an expense split EQUAL / EXACT (cents) / PERCENT (basis points). 3. See each member's net balance. 4. Settle up (record a payment). 5. Simplify debts into a short list of transfers.

| Class | Role |
|---|---|
| `SplitwiseService` | `createGroup`, `addExpense`, `getBalances`, `settleUp`, `simplifyDebts`. Validates everything (members, values, duplicate id) and builds the `Expense` before the ledger changes. Split rules are a `switch` (three cases, no Strategy yet). `simplifyDebts` = greedy with two heaps, at most n-1 transfers. |
| `Group` | Owns members, expenses and one net balance per member. Payer +amount, each participant -share, so balances always sum to 0. |
| `Expense`, `Split` | Immutable. `Expense` rejects splits that don't add up to the amount, so an unbalancing expense can't exist. |
| `Transfer` | One "X pays Y" line of a settle-up plan. |
| Exceptions | `GroupNotFoundException`, `NotAGroupMemberException`, `InvalidSplitException`, `DuplicateExpenseException` (a retried add must not count twice). |

**The trap:** dividing money. With `double` or plain integer division, 100 / 3 loses a cent and the balances stop summing to 0. Everything is `long` cents, and the leftover cents (fewer than n) go one each to the first participants. `Main` checks the sum is exactly 0 after awkward amounts.

Checks in `Main`: the verification trace (one expense per split type, simplify, settle), the rounding trap, every invalid input leaving the ledger unchanged, and bad group setup.

## Phase 2: Extensions

Phase 1's calls work unchanged (`new SplitwiseService()` wires the defaults).

| Extension | Pattern | What changed |
|---|---|---|
| 1. New split type (SHARES, e.g. 2:1:1) | Strategy map keyed by enum | `Map<SplitType, SplitStrategy>`; the `switch` is gone. Adding a type = enum value + one class. Fails at construction if a type has no strategy. `Expense` still re-checks the sum, so a buggy strategy can't unbalance the ledger. |
| 2. Edit / delete an expense | Store the computed splits, reverse them | `Group.removeExpense` / `replaceExpense` undo exactly what was applied. The new version is validated first, so a bad edit keeps the old one. Deleting twice throws `ExpenseNotFoundException` instead of reversing twice. |
| 3. Concurrent users | Lock per group | `ConcurrentHashMap` of groups (`putIfAbsent` on create); `Group`'s ledger methods are `synchronized`, so the duplicate check + apply is atomic and readers see whole expenses. Groups don't block each other. |
| 4. Notify participants | Observer-style seam | `Notifier` list called after the ledger change; a failure is logged, never rolled back. Payer is not notified. |

Not coded (answer in words): multi-currency (store currency per expense, convert at settle time), pairwise "who owes whom" ledger alongside net balances, recurring expenses, activity feed / audit log, persistence with an idempotency key per request, the true minimum-transfers algorithm (NP-hard; greedy is what ships).
