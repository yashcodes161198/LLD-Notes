# Parking lot LLD: interview revision

Companion to [the full final-code notes](FINAL_CODE_NOTES.md). Based on [the linked Java implementation](https://github.com/shubhkpatel/Low-Level-Design/tree/main/src/main/java/org/nailyourinterview/lld/parking_lot) and [Shubh Patel's lesson](https://www.youtube.com/watch?v=kzfrbkDcYjA).

## Recall the two flows

```text
EntryGate → ParkingLot → ParkingFloor → ParkingSpot.tryOccupy()
                      → build Ticket → register active ticket → return

ExitGate → ParkingLot → find Ticket → PricingStrategy.calculateFee()
                      → PaymentStrategyFactory → PaymentProcessor.pay()
                      → success: vacate spot + remove active ticket
                      → failure: retain ticket + occupancy
```

Remember: **claim → record → price → pay → release**.

## Objects and ownership

| Object | Owns or does |
|---|---|
| Lot | Floors, active tickets, current pricing strategy, orchestration |
| Floor | Spots; scans for and claims one compatible spot |
| Spot | Allowed vehicle type and atomic occupancy |
| Ticket | One visit: ID, vehicle, entry time, floor/spot IDs, payment status |
| Gates | Delegate entry/exit requests |

A vehicle is an arriving object. A ticket is one visit by that vehicle.

## Patterns and the questions they answer

| Pattern | Exact location | Question |
|---|---|---|
| Strategy: pricing | `PricingStrategy` → `TimeBasedPricing`, `EventBasedPricing`; context is `ParkingLot` | How much should this visit cost? |
| Strategy: payment | `PaymentStrategy` → cash, UPI, card; context is `PaymentProcessor` | How should payment be collected? |
| Simple Factory × 3 | Vehicle, pricing, payment factories | Which concrete object should be created? |
| Eager Singleton | `ParkingLot.INSTANCE`, private constructor, `getInstance()` | How does this example share one lot instance? |
| Builder | Lombok `@Builder` on `Ticket` | How can many fields be assembled readably? |

Factory creates. Strategy executes. Builder assembles. Singleton shares an instance.

Atomic compare-and-set is a concurrency technique. Inheritance and delegation are object-oriented relationships. There is no allocation Strategy, Observer, or State pattern implemented here.

## Concurrency explanation

`compareAndSet(false, true)` atomically claims a free spot. Two competing arrivals cannot both successfully claim it before another release.

Do not claim the whole service is thread-safe: `activeTickets` is a `HashMap`, allocation and ticket registration are separate, and simultaneous exits can both charge the same ticket. A concurrent map alone does not protect the complete checkout workflow or the mutable values inside it.

## Billing example

Car enters 07:30 and leaves 13:15. Duration = 345 minutes, rounded to 6 billable hours.

- Time policy samples 07:00 through 12:00: one non-peak hour at ₹20 and five peak hours at ₹30 = **₹170**.
- Event policy charges six hours at ₹50 = **₹300**.

The current time algorithm samples rounded units; it does not split exact peak/non-peak minute intervals. The event policy must be selected; it does not detect events.

## Details worth catching

- `findAvailableSpot()` also occupies the spot.
- Compatibility is exact type matching. Bikes cannot use car spots.
- HashMap iteration does not guarantee nearest or lowest-numbered allocation.
- Ticket Builder omits `paymentStatus`; without `@Builder.Default`, its intended `PENDING` initializer does not supply that builder value.
- All three payment implementations are stubs that return success.
- Pricing is selected globally at exit, not preserved per ticket.
- Singleton construction safety is different from workflow thread safety.

## Rebuild it from a blank page

1. Clarify types, fit rules, floors/gates, fees, payments, and concurrent arrivals.
2. Draw lot → floors → spots; add vehicle and visit ticket.
3. Trace entry and exit; give the lot coordination responsibility.
4. Extract varying pricing and payment algorithms into Strategies.
5. Add factories when enum-driven construction is needed.
6. Explain atomic spot claims and the separate checkout consistency problem.
7. Walk one successful visit, one full-lot result, and two competing arrivals.

## Spoken answer

“The lot owns floors, floors own typed spots, and tickets record visits. Gates delegate to the lot. Entry atomically claims a compatible spot and returns a ticket. Exit calculates a fee using a pricing strategy, collects payment through a payment strategy and processor, then frees the spot on success. Simple factories centralize object selection, and Builder assembles the ticket. This implementation uses an eager Singleton. Its atomic spot claim protects competing arrivals, while ticket storage and duplicate checkout need additional concurrency protection.”
