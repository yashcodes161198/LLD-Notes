# Parking Lot: Java code

Runnable Java for the parking lot, designed with the Hello Interview delivery framework (the same flow as `../../amazon-locker/`). Run either phase with `./run.sh` (JDK 17+). `Main` is a set of self-checks: it prints each step and throws if anything is wrong.

This is a separate, interview-sized design. It is not the six-phase walkthrough in `../phases/`, which follows the video and has gates, factories and a Singleton.

```
Phase-N/src/parkinglot/
  model/      SpotSize, VehicleType, PaymentMethod, ParkingSpot, Ticket, Receipt
  payment/    PaymentProcessor (the seam) + CardPaymentProcessor, CashPaymentProcessor: the charge is stubbed with a println
  service/    ParkingLot (orchestrator), LotFullException, VehicleAlreadyParkedException,
              InvalidTicketException, PaymentFailedException
  pricing/    PricingStrategy + HourlyPricing, PeakHourPricing, FlatRatePricing   (Phase 2)
  selection/  SpotSelector + SmallestFitSelector, LowestFloorSelector              (Phase 2)
  Main.java   self-checks
```

Time comes from an injected `java.time.Clock`, so `Main` can jump hours ahead without waiting.

## The design in brief

Requirements:
1. A driver enters with a plate and a vehicle type (MOTORCYCLE, CAR, TRUCK). The lot gives the smallest free spot the vehicle fits in, then the lowest floor, and issues a ticket. If nothing fits: `LotFullException`. If that plate is already inside: `VehicleAlreadyParkedException`.
2. A driver exits with a ticket and a payment method. Unknown or already-used ticket: `InvalidTicketException`.
3. The fee is an hourly rate per vehicle type. Every started hour counts, and the first hour is always charged. A car bumped up to a LARGE spot still pays the car rate (a product decision).
4. Payment happens before the gate opens. If the charge fails: `PaymentFailedException`, and nothing changes, so the driver can pay another way.

Out of scope: payment processing itself (the seam), reservations, gates and hardware, multiple lots.

Entities: `ParkingLot` (orchestrator), `ParkingSpot`, `Ticket`, plus `Receipt` and the enums. `Vehicle` is pruned: a plate string plus a `VehicleType` carry everything the rules need. `Floor` is pruned too: it's a field on the spot, because it has no behaviour.

**The trap.** If the design treats "I hold a `Ticket` object" as proof the car is inside, a ticket used a second time frees its old spot, which by then may hold someone else's car. The fix: only the lot's `activeTickets` map makes a ticket valid, an exit removes the ticket from it, and `ParkingSpot.vacate()` throws if the spot is already free. `Main` checks this in both phases: exit T1, park KA-5 in the same spot, exit T1 again, and the result is `InvalidTicketException` with KA-5's spot still occupied.

## Phase 1: Core Requirements

| Class | Role |
|---|---|
| `ParkingLot` | `enter(plate, type) -> Ticket`, `exit(ticketId, method) -> Receipt`. Spots are sorted once at construction (size, floor, id), so a linear scan for the first free fitting spot finds the best one. `exit` charges first, then retires the ticket and frees the spot. Fails at construction on no spots, duplicate ids, a vehicle type with no rate, or a payment method with no processor. |
| `ParkingSpot` | The physical spot. It owns `occupied`. `occupy()` and `vacate()` throw on an illegal move. |
| `Ticket` | An immutable record of one stay (plate, type, spot, entry time). It is valid only while it's in `activeTickets`. |
| `Receipt` | What the exit returns: the amount, the method, entry and exit times. |
| `PaymentProcessor` | The seam. `CardPaymentProcessor` and `CashPaymentProcessor` stub it. `Main` uses a recording test double that can be set to decline. |
| 4 exceptions | One per requirement-named error. |

Checks in `Main`: the verification trace (best-fit order, bump-up to LARGE, lot full, duplicate plate, fee rounding, the 1-hour minimum), the trap (a reused ticket is rejected, the other car's spot stays occupied, the first exit is charged exactly once), a declined payment that changes nothing and then succeeds on retry, input validation, and bad config at startup.

## Phase 2: Extensions

Each extension lands in one place. The Phase 1 constructor `new ParkingLot(spots, rates, processors, clock)` still works: it becomes hourly pricing plus smallest-fit selection.

| Extension | Pattern | What changed |
|---|---|---|
| 1. Different pricing (peak hours, event days) | Strategy, swappable at runtime | `PricingStrategy`: `HourlyPricing` (Phase 1's rule), `PeakHourPricing` (each billed hour is priced by the local time it starts at), `FlatRatePricing`. `setPricingStrategy` is a `volatile` field. Product decision: the strategy in force at exit prices the whole stay. |
| 2. Spot assignment policy | Strategy that ranks, while the lot claims | `SpotSelector` returns the fitting spots best first: `SmallestFitSelector` or `LowestFloorSelector` (nearest to a ground-level entrance). The order is computed once per vehicle type, at construction. Selectors are pure, so they can't break atomicity. |
| 3. Availability display board | Read-only query | `freeSpotsBySize()`. It's a snapshot, so it can be stale by the time the driver reaches the spot. |
| 4. Many gates at once | CAS + `ConcurrentHashMap`, no lock across the payment call | `ParkingSpot.tryOccupy()` is a compare-and-set. `enter` claims the plate with `putIfAbsent` before it takes a spot, and releases the claim if the lot is full. `exit` claims the ticket with `remove()`, and puts it back if pricing or the charge fails. `Main` runs 50 rounds: the same plate at 4 gates gets 1 ticket, 8 cars racing for 2 spots get 2 tickets, and the same ticket at 4 exit gates gives 1 exit and 1 charge. |

Adding a vehicle type is one enum value plus one rate in each pricing map. The constructors fail at startup if a rate is missing.

Not coded (answer these in words): EV-charging and accessible spots (a spot feature set and a selector that filters on it), reservations (a hold with a TTL, as in Amazon Locker's two-phase deposit), lost-ticket handling (look up by plate, charge the daily maximum), many lots behind one service, and payment idempotency keys and refunds.
