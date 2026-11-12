# Amazon Locker: Java code

Runnable Java for the design in [`../amazon_locker.py`](../amazon_locker.py). Run either phase with `./run.sh` (JDK 17+). `Main` is a set of self-checks: it prints each step and throws if anything is wrong.

```
Phase-N/src/amazonlocker/
  model/      Size, Compartment, AccessToken (+ CompartmentStatus, Reservation in Phase 2)
  hardware/   DoorActuator (the seam) + SolenoidDoorActuator: the latch call is stubbed with a println
  locker/     Locker (orchestrator), NoAvailableCompartmentException, InvalidAccessCodeException,
              AccessCodeExpiredException (+ InvalidReservationException in Phase 2)
  Main.java   self-checks
```

Time comes from an injected `java.time.Clock`, so `Main` can jump 9 days ahead without waiting.

## Phase 1: Core Requirements

Requirements 1-5 from Step 1, using the BEST version of `openExpiredCompartments` (not the TRICKY #6 trap).

| Class | Role |
|---|---|
| `Locker` | `depositPackage(size) -> code`, `pickup(code)`, `openExpiredCompartments() -> opened ids`. Linear scan for a free compartment. Expired codes move from the live map to `expiredCodes`, so a late customer still gets "expired" and a reused compartment is never reopened. Fails at construction on no compartments or duplicate ids. |
| `Compartment` | Physical slot; owns `occupied` (Information Expert) and `open()`, which calls the door. |
| `AccessToken` | Immutable bearer token: code + expiry + the one compartment it opens. `isExpired(now)`. |
| `DoorActuator` | The hardware seam. `SolenoidDoorActuator` stubs it. `Main` uses a recording test double to assert which doors opened. |
| 3 exceptions | One per requirement-named error: no space, invalid code, expired code. |

Checks in `Main`: the verification trace from the design, TRICKY #6 (a reused compartment is not reopened by a second staff run, and the run is idempotent), locker-full, input validation, and bad config at startup.

## Phase 2: Extensions

Each extension lands where the design says. `depositPackage(size)` still works (now reserve + confirm in one call).

| Extension | Pattern | What changed |
|---|---|---|
| 1. Size fallback | Outer loop over an ordered size list | Only `getAvailableCompartment`: starts at the requested size in `SIZES_IN_ORDER`, goes up, never down. |
| 2. Broken compartments | Boolean → enum, entity owns transitions | `CompartmentStatus`; `Compartment` rejects illegal moves. `markOutOfService` only from AVAILABLE, so no live token is orphaned. `Locker.markOutOfService / returnToService`. |
| 3. Confirm the deposit | Two-phase commit (reserve → confirm / cancel / timeout) | `reserveCompartment`, `confirmDeposit`, `cancelReservation`, `cancelStaleReservations` (also run lazily on every reserve). Token is issued only on confirm. 3-minute reservation TTL. |

Also: public `Locker` methods are `synchronized`, so "find + mark" is atomic for two drivers at once.

Not coded (answered in words in the design's "More follow-ups"): lockout after N failed codes, multiple locker stations, customer notification on confirm, periodic cleanup of old `expiredCodes`.
