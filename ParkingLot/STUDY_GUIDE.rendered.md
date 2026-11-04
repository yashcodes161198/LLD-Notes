# Parking lot LLD: build the design one requirement at a time

**A guided study document · Java 17+ · four runnable checkpoints · UML, dry runs and exercises**

The goal is to learn how to arrive at a design you can explain. At each checkpoint, read the problem, pause at the question, inspect the diagram, and then read the implementation. Try predicting which existing class must change before moving on.

**About your video.** Your reference is [“6. Parking Lot, Low Level Design (Hindi)” by Concept && Coding — Shrayansh](https://www.youtube.com/watch?v=MtjZf7291zc). When checked on 28 October 2026, YouTube returned a members-only playback restriction and no accessible transcript. Its description links to an [English version and completed implementation](https://www.youtube.com/watch?v=g8AVGr_0Wko). I could verify that metadata, but not the lesson's spoken explanation or diagrams. This guide is therefore an original walkthrough of the same problem, not a transcript, timestamped companion, or claim about the instructor's exact design. The extra failure-handling material is part of this guide's own design.

## Study route

| Stop | Question you should be able to answer | New idea |
|---|---|---|
| 0. Requirements | What does “park a vehicle” promise? | Scope and invariants |
| 1. Basic allocation | Which objects must exist to park and release a car? | Objects and ownership |
| 2. Tickets and billing | How do we identify a particular visit? | Visit identity and immutable facts |
| 3. Variable policies | What changes when “first available” becomes “nearest”? | Strategy and composition |
| 4. Payments and multiple gates | What must remain true when requests fail or overlap? | State, idempotency and atomic operations |
| Extensions | Which requirement would justify another class? | Floors, factories, displays and persistence |

You can stop at checkpoint 2 for a small object-model exercise, checkpoint 3 for a design-pattern discussion, or checkpoint 4 for an interview that includes payments and concurrency. These are progressively richer scopes, not a ranking of programmers.

## 0. Establish the rules before drawing classes

Imagine a driver arriving, receiving a spot and a ticket, staying for some time, paying, and leaving. That story gives you a workflow; it does not yet tell you whether the system supports bikes, reservations, multiple floors or cash payments.

An effective interview opening is: “I'll start with one lot, bikes and cars, one spot per vehicle, ticket-based entry, and hourly fees. I'll then add configurable allocation, payment outcomes, and concurrent gates.” Give the interviewer room to adjust those assumptions.

### Questions that actually affect the design

| Clarification | Assumption used here | Why it matters |
|---|---|---|
| Which vehicles? | Bikes and cars | Compatibility and rates |
| Can a bike use a car spot? | No; exact type matching | Avoids an unstated fallback rule |
| How many floors? | One; floors are a later extension | No need for a floor class initially |
| Which spot gets assigned? | First available, later nearest to a fixed reference entrance | Creates the allocation-policy variation |
| How are fees calculated? | INR 20/hour for bikes, INR 50/hour for cars; each started hour, minimum one hour | Makes rounding and zero-duration behavior explicit |
| When is the fee fixed? | Checkpoints 2–3: exit time. Checkpoint 4: first payment request | Payment retries must use a stable amount |
| What does payment failure mean? | Definitive decline differs from an unknown result | Determines whether a new attempt is safe |
| When is a spot released? | Checkpoint 4: successful payment followed by logical exit confirmation | Paying alone does not make the space available |
| What about physical barriers? | An adapter can invoke the service; motor control and sensors are outside the code | Physical departure needs additional state in a real installation |
| Where is data stored? | In memory, one Java process | A Java lock cannot protect multiple application instances |

Reservations, monthly passes, lost-ticket penalties, EV charging, administrative screens and multiple payment methods are follow-up requirements. Do not implement them before the interviewer asks.

### Write invariants, not just features

An invariant is a condition that must hold after every completed operation:

1. A spot has at most one current parking session.
2. A normalized plate has at most one current parking session.
3. An assigned spot accepts the vehicle's type.
4. A ticket identifies one visit, even if the same vehicle visits again later.
5. In checkpoint 4, unpaid or unresolved payment cannot release the spot.
6. Repeating a completed payment or exit does not charge or release again.
7. A rejected request leaves the existing allocation intact.

The first three prevent incorrect entry. The next three protect the visit lifecycle. The last one tells you to validate before changing shared state.

### UML: the behavior before the classes

![diagram](./STUDY_GUIDE.rendered-1.svg)

**Pause and answer:** If there are ten empty bike spots and zero empty car spots, is the lot full? For a car, yes. “Full” is relative to compatibility, not just total occupancy.

## 1. Checkpoint one: the smallest useful object model

**Requirement:** Park a compatible vehicle in an available spot; release it when it leaves. We temporarily identify departures by plate and assume one thread.

### Derive the objects

Start with nouns, then ask whether each noun needs identity or behavior.

| Candidate | Keep it? | Reason |
|---|---|---|
| Vehicle | Yes | Plate and type travel together |
| Parking spot | Yes | Has identity, compatibility and occupancy |
| Parking lot | Yes | Coordinates the collection of spots |
| Driver | Not yet | No driver-specific rule or data is required |
| Car and Bike subclasses | Not yet | Their only distinction is a type value |
| Entrance gate | Not yet | No independent gate behavior in this checkpoint |

`VehicleType` is an enum because we have a small, explicit set of categories. `Vehicle` is a record because its facts do not change during a visit. `ParkingSpot` is mutable because its occupant changes. `ParkingLot` coordinates the operation because assigning one spot requires looking across multiple spots.

“A lot has spots” is composition. “A spot currently holds a vehicle” is an association; the vehicle still exists when it leaves the lot.

### UML: three meaningful objects

![diagram](./STUDY_GUIDE.rendered-2.svg)

**UML reading key:** `*--` means ownership/composition, `-->` means an association/reference, `..>` means a dependency, and `..|>` means a class implements an interface. `0..1` means optional; `0..*` means any number. Class diagrams show the important domain types, not every Java library class or test helper. UML operations are conceptually public API; nested demo classes use package access to keep the files short.

### Walk through the operation

1. Normalize the plate: trim surrounding spaces and uppercase it with `Locale.ROOT`.
2. Reject the vehicle if that normalized plate is already parked.
3. Scan for the first empty spot of the same type.
4. Ask the spot to occupy itself; it checks its own availability and compatibility.
5. Return the spot's ID.

Do not store both `isEmpty` and `occupant` unless you have a good reason. If `isEmpty == true` while `occupant != null`, which value should a caller believe? Here, `occupant == null` answers the question directly.

### Runnable code

The complete implementation is [BasicParkingDemo.java](checkpoint1/BasicParkingDemo.java). All types are nested in one executable class for convenience; nesting is not a domain requirement.

<details>
<summary>Full runnable Java: checkpoint1/BasicParkingDemo.java</summary>

```java
import java.util.*;

public class BasicParkingDemo {
    enum VehicleType { BIKE, CAR }

    record Vehicle(String plate, VehicleType type) {
        Vehicle {
            if (plate == null || plate.isBlank()) throw new IllegalArgumentException("Plate required");
            plate = plate.strip().toUpperCase(Locale.ROOT);
            Objects.requireNonNull(type);
        }
    }

    static final class ParkingSpot {
        final int id;
        final VehicleType type;
        private Vehicle occupant;

        ParkingSpot(int id, VehicleType type) {
            this.id = id;
            this.type = Objects.requireNonNull(type);
        }

        boolean accepts(Vehicle vehicle) { return occupant == null && type == vehicle.type(); }
        void park(Vehicle vehicle) {
            if (!accepts(vehicle)) throw new IllegalStateException("Unavailable or incompatible spot");
            occupant = vehicle;
        }
        void release() { occupant = null; }
    }

    // shortcut: single-threaded, replace plate-based exit with tickets in checkpoint 2.
    static final class ParkingLot {
        private final List<ParkingSpot> spots = List.of(
                new ParkingSpot(1, VehicleType.BIKE), new ParkingSpot(2, VehicleType.CAR));

        int park(Vehicle vehicle) {
            if (spots.stream().anyMatch(s -> s.occupant != null && s.occupant.plate().equals(vehicle.plate())))
                throw new IllegalStateException("Vehicle already parked");
            ParkingSpot spot = spots.stream().filter(s -> s.accepts(vehicle)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("No compatible spot"));
            spot.park(vehicle);
            return spot.id;
        }

        void leave(String plate) {
            if (plate == null || plate.isBlank()) throw new IllegalArgumentException("Plate required");
            String normalized = plate.strip().toUpperCase(Locale.ROOT);
            ParkingSpot spot = spots.stream()
                    .filter(s -> s.occupant != null && s.occupant.plate().equals(normalized))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Vehicle not parked"));
            spot.release();
        }
    }

    public static void main(String[] args) {
        ParkingLot lot = new ParkingLot();
        Vehicle car = new Vehicle(" ka-01-a-100 ", VehicleType.CAR);
        if (lot.park(car) != 2) throw new AssertionError("Wrong spot type");
        lot.leave(car.plate());
        if (lot.park(car) != 2) throw new AssertionError("Spot was not reusable");
        System.out.println("Checkpoint 1 passed: compatible allocation and reuse.");
    }
}
```

</details>


### Dry run

| Action | Bike spot 1 | Car spot 2 | Result |
|---|---|---|---|
| Start | Empty | Empty | Two available spots |
| Park car A | Empty | A | Return spot 2 |
| Park car A again | Empty | A | Reject duplicate plate |
| Park car B | Empty | A | Reject: no car spot |
| A leaves | Empty | Empty | Spot 2 reusable |

### Why stop here before adding more?

This design already solves allocation. Its limitations now give us concrete reasons to extend it: there is no entry time, no bill, and no separate identity for today's visit versus tomorrow's. Passing only a plate into `leave()` cannot identify a historical visit.

**Checkpoint question:** Would you fix this by adding `entryTime` to `Vehicle`? That would make the vehicle describe a visit. A car exists before and after each visit, so the visit needs its own object.

## 2. Checkpoint two: a ticket identifies a visit

**New requirement:** Record entry time, compute a fee on departure, and handle a repeated exit request safely. Payment processing is still outside this version; its exit operation treats fee settlement as external.

### The design change

Add an immutable `Ticket` containing a unique ID, the vehicle, assigned spot ID and entry timestamp. Add a `Receipt` for the completed visit. A plate identifies a vehicle; a ticket ID identifies one stay.

Keep these indexes inside `ParkingLot`:

- `active`: ticket ID → ticket, for active visits.
- `byPlate`: normalized plate → ticket ID, for duplicate-entry checks.
- `closed`: ticket ID → original receipt, for repeated exits.

The spot now holds the current ticket ID instead of the vehicle. The ticket provides the vehicle facts. That makes the occupancy relationship explicitly about a visit.

### UML: preserve the visit after the car leaves

![diagram](./STUDY_GUIDE.rendered-3.svg)

The `0..*` tickets per spot refer to visits over time, not simultaneous occupancy. The current-occupancy invariant still limits a spot to one active ticket.

### Billing must be a precise rule

For a nonnegative stay:

`billableHours = max(1, ceil(elapsedDuration / 1 hour))`

`amountPaise = billableHours × hourlyRatePaise`

| Car's stay | Billable hours | Charge |
|---|---:|---:|
| Zero duration | 1 | INR 50 |
| 59 minutes | 1 | INR 50 |
| Exactly 60 minutes | 1 | INR 50 |
| 60 minutes + 1 nanosecond | 2 | INR 100 |
| 90 minutes | 2 | INR 100 |

The minimum one hour is a business assumption, not a mathematical necessity. You could agree on a free grace period instead, but you must change the rule and its tests together.

The code uses `Duration`, counts complete hours, and checks whether any remainder exists. Truncating to minutes before rounding would incorrectly bill 60 minutes plus one second as one hour. Money is a `long` number of paise; `5000` means INR 50. A multi-currency system would also need currency identity. `Math.multiplyExact` detects overflow instead of silently wrapping.

`Instant` represents a moment; a displayed local time is a presentation concern. The examples pass time in so tests are deterministic. A deployed entry adapter should obtain time from a trusted server clock, not accept an arbitrary timestamp supplied by a driver.

### UML: exit and a repeated request

![diagram](./STUDY_GUIDE.rendered-4.svg)

### Runnable code

<details>
<summary>Full runnable Java: checkpoint2/TicketParkingDemo.java</summary>

```java
import java.time.*;
import java.util.*;

public class TicketParkingDemo {
    enum VehicleType { BIKE, CAR }
    record Vehicle(String plate, VehicleType type) {
        Vehicle {
            if (plate == null || plate.isBlank()) throw new IllegalArgumentException("Plate required");
            plate = plate.strip().toUpperCase(Locale.ROOT);
            Objects.requireNonNull(type);
        }
    }
    record Ticket(UUID id, Vehicle vehicle, int spotId, Instant enteredAt) {}
    record Receipt(UUID ticketId, Instant exitedAt, long amountPaise) {}

    static final class ParkingSpot {
        final int id;
        final VehicleType type;
        private UUID ticketId;
        ParkingSpot(int id, VehicleType type) { this.id = id; this.type = type; }
        boolean accepts(Vehicle vehicle) { return ticketId == null && type == vehicle.type(); }
    }

    // shortcut: one thread and no payment provider; checkpoint 4 adds both concerns.
    static final class ParkingLot {
        private final List<ParkingSpot> spots = List.of(
                new ParkingSpot(1, VehicleType.BIKE), new ParkingSpot(2, VehicleType.CAR));
        private final Map<UUID, Ticket> active = new HashMap<>();
        private final Map<UUID, Receipt> closed = new HashMap<>();
        private final Map<String, UUID> byPlate = new HashMap<>();

        Ticket enter(Vehicle vehicle, Instant now) {
            Objects.requireNonNull(now);
            if (byPlate.containsKey(vehicle.plate())) throw new IllegalStateException("Vehicle already parked");
            ParkingSpot spot = spots.stream().filter(s -> s.accepts(vehicle)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("No compatible spot"));
            Ticket ticket = new Ticket(UUID.randomUUID(), vehicle, spot.id, now);
            spot.ticketId = ticket.id();
            active.put(ticket.id(), ticket);
            byPlate.put(vehicle.plate(), ticket.id());
            return ticket;
        }

        Receipt exit(UUID ticketId, Instant now) {
            Objects.requireNonNull(now);
            if (closed.containsKey(ticketId)) return closed.get(ticketId);
            Ticket ticket = active.get(ticketId);
            if (ticket == null) throw new IllegalArgumentException("Unknown ticket");
            long fee = fee(ticket, now);
            ParkingSpot spot = spots.stream().filter(s -> s.id == ticket.spotId()).findFirst().orElseThrow();
            if (!ticketId.equals(spot.ticketId)) throw new IllegalStateException("Occupancy mismatch");
            Receipt receipt = new Receipt(ticketId, now, fee);
            spot.ticketId = null;
            active.remove(ticketId);
            byPlate.remove(ticket.vehicle().plate());
            closed.put(ticketId, receipt);
            return receipt;
        }

        private long fee(Ticket ticket, Instant now) {
            Duration stay = Duration.between(ticket.enteredAt(), now);
            if (stay.isNegative()) throw new IllegalArgumentException("Exit before entry");
            long hours = stay.toHours();
            if (!stay.minusHours(hours).isZero()) hours++;
            long ratePaise = ticket.vehicle().type() == VehicleType.BIKE ? 2000 : 5000;
            return Math.multiplyExact(Math.max(1, hours), ratePaise);
        }
    }

    public static void main(String[] args) {
        ParkingLot lot = new ParkingLot();
        Instant start = Instant.parse("2026-01-01T10:00:00Z");
        Ticket ticket = lot.enter(new Vehicle("KA-01-A-100", VehicleType.CAR), start);
        Receipt receipt = lot.exit(ticket.id(), start.plusSeconds(3601));
        if (receipt.amountPaise() != 10000) throw new AssertionError("Round started hours up");
        if (!lot.exit(ticket.id(), start.plusSeconds(7200)).equals(receipt))
            throw new AssertionError("Repeated exit must return the original receipt");
        lot.enter(ticket.vehicle(), start.plusSeconds(7200));
        System.out.println("Checkpoint 2 passed: ticket, fee, repeat exit and re-entry.");
    }
}
```

</details>


### Dry run: same vehicle, different visits

At 10:00, car A enters and receives ticket T1. At 11:00:01, it exits with an INR 100 receipt. Repeating `exit(T1)` returns that receipt; it does not repeat the release. At 12:00, the same car receives ticket T2. A late retry using T1 still returns T1's receipt and cannot free T2's spot.

This is **idempotency**: repeating the same logical operation has the same observable result. Another valid API could reject a repeated exit, but returning the original receipt is convenient when a caller retries after losing a response. Unknown tickets still fail.

### What begins to feel awkward?

“Choose the first spot” is embedded in `enter()`, and hourly pricing is embedded in `ParkingLot`. The lot changes whenever either business policy changes. That is manageable with one policy. A request for a second policy gives us a reason to extract those decisions.

**Checkpoint question:** If allocation changes but pricing does not, why should the fee code be involved? It should not. They are independent dimensions of variation.

## 3. Checkpoint three: separate policy from coordination

**New requirement:** Support first-available and nearest-entry allocation, and hourly or flat-rate pricing, without rewriting the entry/exit workflow.

### Discover Strategy from the requirement

First describe the changing operation in one sentence:

- Allocation: “Choose one spot from the eligible candidates.”
- Pricing: “Compute an amount from a ticket and a billing cutoff.”

Those sentences become two interfaces. `ParkingLot` receives implementations in its constructor. It calls those objects when it reaches the relevant decision. This is the Strategy pattern, introduced because there are multiple actual behaviors to choose from.

Keep **eligibility** separate from **preference**. The lot filters for compatible, unoccupied spots. The allocation strategy chooses among them. That means `NearestEntry` cannot intentionally allocate an occupied spot; the lot also checks the returned choice belongs to the candidate list before mutating state.

### A deliberate representation change

In this version, `Spot` is an immutable description: ID, type and distance. `ParkingLot.occupancy` stores spot ID → current ticket ID. We removed mutable occupancy from each spot because the lot must coordinate occupancy with its ticket and plate indexes.

Both representations are valid. Mutable spot entities can work, especially when spot-level behavior becomes richer. The important rule is to have one authoritative occupancy representation and a clear owner for updates. Do not keep a map and an independent `isEmpty` flag that can drift apart.

### UML: two independent policy families

![diagram](./STUDY_GUIDE.rendered-5.svg)

`ParkingLot` has an allocation strategy and has a pricing strategy; it does not inherit either behavior. You can combine nearest allocation with hourly pricing or flat pricing without creating subclasses for every combination.

The constructor is also where configuration is checked. Duplicate spot IDs are rejected because an occupancy map keyed by ID could otherwise treat two physical spots as one.

### Runnable code

<details>
<summary>Full runnable Java: checkpoint3/PolicyParkingDemo.java</summary>

```java
import java.time.*;
import java.util.*;

public class PolicyParkingDemo {
    enum VehicleType { BIKE, CAR }
    record Vehicle(String plate, VehicleType type) {
        Vehicle {
            if (plate == null || plate.isBlank()) throw new IllegalArgumentException("Plate required");
            plate = plate.strip().toUpperCase(Locale.ROOT);
            Objects.requireNonNull(type);
        }
    }
    record Spot(int id, VehicleType type, int distanceFromEntry) {
        Spot {
            Objects.requireNonNull(type);
            if (distanceFromEntry < 0) throw new IllegalArgumentException("Negative distance");
        }
    }
    record Ticket(UUID id, Vehicle vehicle, int spotId, Instant enteredAt) {}
    record Receipt(UUID ticketId, Instant exitedAt, long amountPaise) {}

    interface AllocationStrategy {
        Optional<Spot> choose(List<Spot> eligible);
    }
    static final class FirstAvailable implements AllocationStrategy {
        public Optional<Spot> choose(List<Spot> eligible) { return eligible.stream().findFirst(); }
    }
    static final class NearestEntry implements AllocationStrategy {
        public Optional<Spot> choose(List<Spot> eligible) {
            return eligible.stream().min(Comparator.comparingInt(Spot::distanceFromEntry).thenComparingInt(Spot::id));
        }
    }
    interface PricingStrategy {
        long fee(Ticket ticket, Instant until);
    }
    static final class HourlyPricing implements PricingStrategy {
        public long fee(Ticket ticket, Instant until) {
            Duration stay = Duration.between(ticket.enteredAt(), until);
            if (stay.isNegative()) throw new IllegalArgumentException("Exit before entry");
            long hours = stay.toHours();
            if (!stay.minusHours(hours).isZero()) hours++;
            return Math.multiplyExact(Math.max(1, hours), ticket.vehicle().type() == VehicleType.BIKE ? 2000 : 5000);
        }
    }
    static final class FlatPricing implements PricingStrategy {
        private final long amountPaise;
        FlatPricing(long amountPaise) {
            if (amountPaise < 0) throw new IllegalArgumentException("Negative fee");
            this.amountPaise = amountPaise;
        }
        public long fee(Ticket ticket, Instant until) {
            if (until.isBefore(ticket.enteredAt())) throw new IllegalArgumentException("Exit before entry");
            return amountPaise;
        }
    }

    // shortcut: single-threaded; checkpoint 4 makes the whole allocation operation atomic.
    static final class ParkingLot {
        private final List<Spot> spots;
        private final AllocationStrategy allocation;
        private final PricingStrategy pricing;
        private final Map<Integer, UUID> occupancy = new HashMap<>();
        private final Map<UUID, Ticket> active = new HashMap<>();
        private final Map<UUID, Receipt> closed = new HashMap<>();
        private final Map<String, UUID> byPlate = new HashMap<>();

        ParkingLot(List<Spot> spots, AllocationStrategy allocation, PricingStrategy pricing) {
            this.spots = List.copyOf(spots);
            if (spots.stream().map(Spot::id).distinct().count() != spots.size())
                throw new IllegalArgumentException("Duplicate spot IDs");
            this.allocation = Objects.requireNonNull(allocation);
            this.pricing = Objects.requireNonNull(pricing);
        }

        Ticket enter(Vehicle vehicle, Instant now) {
            Objects.requireNonNull(now);
            if (byPlate.containsKey(vehicle.plate())) throw new IllegalStateException("Vehicle already parked");
            List<Spot> eligible = spots.stream()
                    .filter(s -> s.type() == vehicle.type() && !occupancy.containsKey(s.id())).toList();
            Spot spot = allocation.choose(eligible).orElseThrow(() -> new IllegalStateException("No compatible spot"));
            if (!eligible.contains(spot)) throw new IllegalStateException("Strategy returned an ineligible spot");
            Ticket ticket = new Ticket(UUID.randomUUID(), vehicle, spot.id(), now);
            occupancy.put(spot.id(), ticket.id());
            active.put(ticket.id(), ticket);
            byPlate.put(vehicle.plate(), ticket.id());
            return ticket;
        }

        Receipt exit(UUID ticketId, Instant now) {
            Objects.requireNonNull(now);
            if (closed.containsKey(ticketId)) return closed.get(ticketId);
            Ticket ticket = active.get(ticketId);
            if (ticket == null) throw new IllegalArgumentException("Unknown ticket");
            long amount = pricing.fee(ticket, now);
            if (amount < 0) throw new IllegalStateException("Pricing returned a negative fee");
            if (!ticketId.equals(occupancy.get(ticket.spotId()))) throw new IllegalStateException("Occupancy mismatch");
            Receipt receipt = new Receipt(ticketId, now, amount);
            occupancy.remove(ticket.spotId());
            active.remove(ticketId);
            byPlate.remove(ticket.vehicle().plate());
            closed.put(ticketId, receipt);
            return receipt;
        }
    }

    public static void main(String[] args) {
        List<Spot> spots = List.of(new Spot(1, VehicleType.CAR, 100), new Spot(2, VehicleType.CAR, 10));
        Instant start = Instant.parse("2026-01-01T10:00:00Z");
        Vehicle car = new Vehicle("KA-01-A-100", VehicleType.CAR);
        ParkingLot first = new ParkingLot(spots, new FirstAvailable(), new HourlyPricing());
        if (first.enter(car, start).spotId() != 1) throw new AssertionError("First available");
        ParkingLot nearest = new ParkingLot(spots, new NearestEntry(), new FlatPricing(7500));
        Ticket ticket = nearest.enter(car, start);
        if (ticket.spotId() != 2) throw new AssertionError("Nearest entry");
        if (nearest.exit(ticket.id(), start.plusSeconds(3601)).amountPaise() != 7500)
            throw new AssertionError("Flat pricing");
        System.out.println("Checkpoint 3 passed: interchangeable allocation and pricing.");
    }
}
```

</details>


### Dry run: change the configuration, keep the workflow

Suppose car spot 1 is 100 distance units from the entrance, and spot 2 is 10 units away. The list order is `[1, 2]`.

| Configuration | Chosen spot | Charge after 60 minutes + 1 second |
|---|---:|---:|
| First available + hourly | 1 | INR 100 |
| Nearest entry + hourly | 2 | INR 100 |
| Nearest entry + flat INR 75 | 2 | INR 75 |

The sample executable checks the first choice, the nearest choice, and flat pricing. The table also follows directly from the independently tested hourly calculation.

The nearest strategy uses distance, then spot ID as a deterministic tie-breaker. “Nearest” here is to one fixed reference entrance. Multiple gate objects can share the service, but true nearest-to-the-current-gate requires a gate ID and a gate-specific distance function; that extension appears later.

### Do we need a factory or inheritance hierarchy now?

No factory is required merely because an interface exists. In this program, constructing `new NearestEntry()` at startup makes the chosen policy explicit. A factory becomes useful when runtime configuration such as `allocationMode = NEAREST` must select and construct a configured implementation.

Similarly, separate `BikeSpot` and `CarSpot` subclasses are unnecessary when the only difference is a type value. Subclasses make more sense if their behavior and constraints differ substantially. A charger capability, dimensions or permit rules may also be better represented as fields or composed policies than as a growing subtype tree.

### Connect this to SOLID without reciting acronyms

| Principle | Concrete example | Limit to remember |
|---|---|---|
| Single responsibility | Pricing implementations own tariff calculations | The lot still legitimately coordinates one parking use case |
| Open/closed | Add a different pricing implementation without editing `exit()` | A new vehicle category may still require changes to enums, configuration and compatibility |
| Liskov substitution | Every allocation strategy returns an eligible candidate or no result | An implementation that chooses outside the supplied list breaks the contract |
| Interface segregation | Allocation and pricing have separate small interfaces | Avoid one interface containing park, charge, print and notify |
| Dependency inversion | The lot depends on policy interfaces, supplied from outside | An interface around every data class adds no value |

**Checkpoint question:** Does introducing a Strategy interface make allocation thread-safe? No. We separated a decision; we have not protected concurrent mutations.

## 4. Checkpoint four: failures and simultaneous requests

**New requirement:** Multiple gates share the same lot. Payment can succeed, be declined, or have an unknown outcome. Repeated requests must be safe. Keep the spot until confirmed payment and logical exit.

### First derive the states

An immutable ticket contains facts about entry. The mutable lifecycle belongs to an internal `Session`:

| State | What it means | Does the session still own the spot? |
|---|---|---|
| ACTIVE | Entered; no successful payment, and no unresolved attempt | Yes |
| PAYMENT_PENDING | A payment attempt exists; its outcome is unresolved or being checked | Yes |
| PAID | Payment success is confirmed; logical exit has not occurred | Yes |
| CLOSED | Logical exit completed and the receipt is stored | No |

The caller receives an immutable `Ticket`, not the mutable `Session`. Otherwise a caller could change its own status to `PAID` and bypass the workflow.

### UML: ticket lifecycle

![diagram](./STUDY_GUIDE.rendered-6.svg)

Do not add a separate `PAYMENT_FAILED` state unless it has distinct business behavior. Here, a definitive decline returns to `ACTIVE`, from which another attempt is allowed. An unresolved attempt cannot return to `ACTIVE`, because the provider may already have taken the money.

### Payment outcomes are not a boolean

Consider a provider charging INR 100 and sending a success response that never reaches the parking application. Treating a timeout as “failed” and sending a new charge can take INR 100 twice.

The interface therefore distinguishes:

- `SUCCESS`: record `PAID`.
- `DECLINED`: the provider definitively says this attempt did not charge; allow a new attempt key.
- `UNKNOWN` or an exception: keep `PAYMENT_PENDING`; retry or reconcile with the same key and amount.

Each logical attempt has a key such as `ticketId:attemptNumber`. Reusing a key with a different amount violates the processor contract. After a definitive decline, increment the attempt number. After an unknown outcome, preserve it. The fake processor implements this contract in memory; a real adapter must use the provider's supported idempotency and status-query behavior.

### Why store a Quote?

If the first request is INR 100 and the retry crosses another billing boundary, recomputing INR 150 under the same key is inconsistent. Store a `Quote(pricedUntil, amountPaise)` before making the attempt.

For this teaching version, the first quote stays fixed for the rest of the visit, including declined attempts. There is no expiration or post-payment grace limit. That is a deliberate simplification, not a recommended commercial tariff: a real lot should define quote expiration and an exit grace period. Never replace the amount while an old attempt remains unresolved.

### UML: the final executable model

![diagram](./STUDY_GUIDE.rendered-7.svg)

The final demo keeps one concrete allocation policy and one pricing policy so you can focus on the lifecycle. Checkpoint 3 contains the other interchangeable implementations. Gate classes are thin adapters; every gate must reference the same `ParkingLot` instance for this locking design to work.

### Why a thread-safe map would not fix allocation

Two gates can interleave as follows:

| Time | Gate A | Gate B |
|---|---|---|
| 1 | Reads spot S as free | |
| 2 | | Reads spot S as free |
| 3 | Assigns S to ticket A | |
| 4 | | Assigns S to ticket B |

Even if every individual map operation is thread-safe, the combined check-and-assign operation is not automatically atomic. We must protect eligibility checking, selecting a spot, occupying it, and recording the ticket/plate together.

The demo makes the service operations `synchronized` on the shared lot instance. This also protects exit, status reads and availability snapshots. One gate completes its state change before another gate starts an operation on that instance. Private mutable state cannot escape through a returned object.

### UML: entry under the shared lot lock

![diagram](./STUDY_GUIDE.rendered-8.svg)

This lock is intentionally broad. It is easy to explain and sufficient for a small in-memory exercise. It serializes independent entries, and the fake payment call also runs while holding it. A slow real payment request would block unrelated operations. The later production section explains the next boundary; the sample does not claim network-ready concurrency.

### UML: a lost response, retry and exit

![diagram](./STUDY_GUIDE.rendered-9.svg)

### Runnable code

The fake can return scripted outcomes, including a successful charge whose response is lost. The executable contains behavioral checks; they throw on failure even without Java's `-ea` flag.

<details>
<summary>Full runnable Java: checkpoint4/PaymentParkingDemo.java</summary>

```java
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public class PaymentParkingDemo {
    enum VehicleType { BIKE, CAR }
    enum TicketStatus { ACTIVE, PAYMENT_PENDING, PAID, CLOSED }
    enum PaymentResult { SUCCESS, DECLINED, UNKNOWN }

    record Vehicle(String plate, VehicleType type) {
        Vehicle {
            if (plate == null || plate.isBlank()) throw new IllegalArgumentException("Plate required");
            plate = plate.strip().toUpperCase(Locale.ROOT);
            Objects.requireNonNull(type);
        }
    }
    record Spot(int id, VehicleType type, int distanceFromEntry) {
        Spot {
            Objects.requireNonNull(type);
            if (distanceFromEntry < 0) throw new IllegalArgumentException("Negative distance");
        }
    }
    record Ticket(UUID id, Vehicle vehicle, int spotId, Instant enteredAt) {}
    record Quote(Instant pricedUntil, long amountPaise) {}
    record Receipt(UUID ticketId, Instant exitedAt, long amountPaise, String paymentKey) {}

    interface AllocationStrategy { Optional<Spot> choose(List<Spot> eligible); }
    static final class NearestEntry implements AllocationStrategy {
        public Optional<Spot> choose(List<Spot> eligible) {
            return eligible.stream().min(Comparator.comparingInt(Spot::distanceFromEntry).thenComparingInt(Spot::id));
        }
    }
    interface PricingStrategy { long fee(Ticket ticket, Instant until); }
    static final class HourlyPricing implements PricingStrategy {
        public long fee(Ticket ticket, Instant until) {
            Duration stay = Duration.between(ticket.enteredAt(), until);
            if (stay.isNegative()) throw new IllegalArgumentException("Exit before entry");
            long hours = stay.toHours();
            if (!stay.minusHours(hours).isZero()) hours++;
            return Math.multiplyExact(Math.max(1, hours), ticket.vehicle().type() == VehicleType.BIKE ? 2000 : 5000);
        }
    }

    interface PaymentProcessor {
        // Same key + amount must resolve the same logical attempt, without a second charge.
        PaymentResult charge(String key, long amountPaise);
    }

    static final class FakePayments implements PaymentProcessor {
        private record Attempt(long amountPaise, PaymentResult result) {}
        private final Map<String, Attempt> attempts = new HashMap<>();
        private final Deque<PaymentResult> script = new ArrayDeque<>();
        private int successfulCharges;

        FakePayments(PaymentResult... outcomes) { script.addAll(List.of(outcomes)); }

        public synchronized PaymentResult charge(String key, long amountPaise) {
            Attempt prior = attempts.get(key);
            if (prior != null) {
                if (prior.amountPaise() != amountPaise) throw new IllegalArgumentException("Key reused with different amount");
                return prior.result();
            }
            PaymentResult reply = script.isEmpty() ? PaymentResult.SUCCESS : script.removeFirst();
            // UNKNOWN simulates a successful charge whose response was lost.
            PaymentResult actual = reply == PaymentResult.UNKNOWN ? PaymentResult.SUCCESS : reply;
            attempts.put(key, new Attempt(amountPaise, actual));
            if (actual == PaymentResult.SUCCESS) successfulCharges++;
            return reply;
        }

        synchronized int successfulCharges() { return successfulCharges; }
    }

    private static final class Session {
        final Ticket ticket;
        TicketStatus status = TicketStatus.ACTIVE;
        Quote quote;
        int attemptNumber;
        String paymentKey;
        Instant paidAt;
        Receipt receipt;

        Session(Ticket ticket) { this.ticket = ticket; }
    }

    static final class ParkingLot {
        private final List<Spot> spots;
        private final AllocationStrategy allocation;
        private final PricingStrategy pricing;
        private final PaymentProcessor payments;
        private final Map<Integer, UUID> occupancy = new HashMap<>();
        private final Map<String, UUID> byPlate = new HashMap<>();
        private final Map<UUID, Session> sessions = new HashMap<>();

        ParkingLot(List<Spot> spots, AllocationStrategy allocation, PricingStrategy pricing, PaymentProcessor payments) {
            this.spots = List.copyOf(spots);
            if (spots.stream().map(Spot::id).distinct().count() != spots.size())
                throw new IllegalArgumentException("Duplicate spot IDs");
            this.allocation = Objects.requireNonNull(allocation);
            this.pricing = Objects.requireNonNull(pricing);
            this.payments = Objects.requireNonNull(payments);
        }

        synchronized Ticket enter(Vehicle vehicle, Instant now) {
            Objects.requireNonNull(vehicle);
            Objects.requireNonNull(now);
            if (byPlate.containsKey(vehicle.plate())) throw new IllegalStateException("Vehicle already parked");
            List<Spot> eligible = spots.stream()
                    .filter(s -> s.type() == vehicle.type() && !occupancy.containsKey(s.id())).toList();
            Spot spot = allocation.choose(eligible).orElseThrow(() -> new IllegalStateException("No compatible spot"));
            if (!eligible.contains(spot)) throw new IllegalStateException("Strategy returned an ineligible spot");
            Ticket ticket = new Ticket(UUID.randomUUID(), vehicle, spot.id(), now);
            occupancy.put(spot.id(), ticket.id());
            byPlate.put(vehicle.plate(), ticket.id());
            sessions.put(ticket.id(), new Session(ticket));
            return ticket;
        }

        // shortcut: the whole operation is locked for a fast in-memory fake;
        // move real network payment outside this lock with durable attempt state.
        synchronized PaymentResult pay(UUID ticketId, Instant now) {
            Objects.requireNonNull(now);
            Session session = session(ticketId);
            if (session.status == TicketStatus.PAID || session.status == TicketStatus.CLOSED)
                return PaymentResult.SUCCESS;
            if (now.isBefore(session.ticket.enteredAt())) throw new IllegalArgumentException("Payment before entry");
            if (session.quote == null) {
                long fee = pricing.fee(session.ticket, now);
                if (fee < 0) throw new IllegalStateException("Negative fee");
                session.quote = new Quote(now, fee);
            }
            if (session.status == TicketStatus.ACTIVE) {
                session.attemptNumber++;
                session.paymentKey = ticketId + ":" + session.attemptNumber;
                session.status = TicketStatus.PAYMENT_PENDING;
            }
            // An exception deliberately leaves PAYMENT_PENDING and preserves this key.
            PaymentResult result = Objects.requireNonNull(payments.charge(session.paymentKey, session.quote.amountPaise()));
            if (result == PaymentResult.SUCCESS) {
                session.paidAt = now;
                session.status = TicketStatus.PAID;
            }
            if (result == PaymentResult.DECLINED) session.status = TicketStatus.ACTIVE;
            return result;
        }

        synchronized Receipt exit(UUID ticketId, Instant now) {
            Objects.requireNonNull(now);
            Session session = session(ticketId);
            if (session.status == TicketStatus.CLOSED) return session.receipt;
            if (session.status != TicketStatus.PAID) throw new IllegalStateException("Payment required");
            if (now.isBefore(session.paidAt)) throw new IllegalArgumentException("Exit before payment confirmation");
            Ticket ticket = session.ticket;
            if (!ticketId.equals(occupancy.get(ticket.spotId()))) throw new IllegalStateException("Occupancy mismatch");
            Receipt receipt = new Receipt(ticketId, now, session.quote.amountPaise(), session.paymentKey);
            occupancy.remove(ticket.spotId());
            byPlate.remove(ticket.vehicle().plate());
            session.receipt = receipt;
            session.status = TicketStatus.CLOSED;
            return receipt;
        }

        synchronized TicketStatus status(UUID ticketId) { return session(ticketId).status; }
        synchronized int available(VehicleType type) {
            Objects.requireNonNull(type);
            return (int) spots.stream().filter(s -> s.type() == type && !occupancy.containsKey(s.id())).count();
        }
        private Session session(UUID id) {
            Session session = sessions.get(id);
            if (session == null) throw new IllegalArgumentException("Unknown ticket");
            return session;
        }
    }

    static final class EntranceGate {
        private final ParkingLot lot;
        EntranceGate(ParkingLot lot) { this.lot = lot; }
        Ticket enter(Vehicle vehicle, Instant now) { return lot.enter(vehicle, now); }
    }
    static final class ExitGate {
        private final ParkingLot lot;
        ExitGate(ParkingLot lot) { this.lot = lot; }
        PaymentResult pay(UUID ticketId, Instant now) { return lot.pay(ticketId, now); }
        Receipt exit(UUID ticketId, Instant now) { return lot.exit(ticketId, now); }
    }

    private static final Instant START = Instant.parse("2026-01-01T10:00:00Z");
    private static final Vehicle CAR = new Vehicle("KA-01-A-100", VehicleType.CAR);
    private static ParkingLot lot(PaymentProcessor payments) {
        return new ParkingLot(List.of(new Spot(1, VehicleType.CAR, 10)), new NearestEntry(), new HourlyPricing(), payments);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void rejects(Class<? extends RuntimeException> type, Runnable action) {
        try { action.run(); }
        catch (RuntimeException e) {
            if (type.isInstance(e)) return;
            throw e;
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private static void checkPricing() {
        PricingStrategy pricing = new HourlyPricing();
        Ticket ticket = new Ticket(UUID.randomUUID(), CAR, 1, START);
        check(pricing.fee(ticket, START) == 5000, "Minimum one hour");
        check(pricing.fee(ticket, START.plusSeconds(3600)) == 5000, "Exact hour");
        check(pricing.fee(ticket, START.plusSeconds(3600).plusNanos(1)) == 10000, "Fraction after hour");
        rejects(IllegalArgumentException.class, () -> pricing.fee(ticket, START.minusSeconds(1)));
    }

    private static void checkPaymentLifecycle() {
        FakePayments payments = new FakePayments(PaymentResult.DECLINED, PaymentResult.UNKNOWN);
        ParkingLot lot = lot(payments);
        EntranceGate entry = new EntranceGate(lot);
        ExitGate exit = new ExitGate(lot);
        Ticket ticket = entry.enter(CAR, START);
        rejects(IllegalStateException.class, () -> entry.enter(CAR, START));
        rejects(IllegalStateException.class, () -> entry.enter(new Vehicle("OTHER", VehicleType.CAR), START));
        rejects(IllegalArgumentException.class, () -> exit.pay(UUID.randomUUID(), START));
        rejects(IllegalStateException.class, () -> exit.exit(ticket.id(), START));
        Instant end = START.plusSeconds(3601);
        check(exit.pay(ticket.id(), end) == PaymentResult.DECLINED, "Decline");
        check(lot.status(ticket.id()) == TicketStatus.ACTIVE, "Decline allows a new attempt");
        check(lot.available(VehicleType.CAR) == 0, "Decline retains spot");
        check(exit.pay(ticket.id(), end) == PaymentResult.UNKNOWN, "Lost response");
        check(lot.status(ticket.id()) == TicketStatus.PAYMENT_PENDING, "Unknown remains pending");
        rejects(IllegalStateException.class, () -> exit.exit(ticket.id(), end));
        check(exit.pay(ticket.id(), end.plusSeconds(3600)) == PaymentResult.SUCCESS, "Retry resolves same attempt");
        check(lot.available(VehicleType.CAR) == 0, "Paid vehicle still owns spot");
        Receipt receipt = exit.exit(ticket.id(), end.plusSeconds(3600));
        check(receipt.amountPaise() == 10000, "Quote stays frozen during retries");
        check(exit.exit(ticket.id(), end.plusSeconds(7200)).equals(receipt), "Repeat exit is idempotent");
        check(exit.pay(ticket.id(), end) == PaymentResult.SUCCESS, "Repeat payment returns success");
        check(payments.successfulCharges() == 1, "Exactly one successful charge");
        check(lot.available(VehicleType.CAR) == 1, "Exit releases spot");
        Ticket next = entry.enter(CAR, end.plusSeconds(7200));
        exit.exit(ticket.id(), end.plusSeconds(7201));
        check(lot.available(VehicleType.CAR) == 0, "Old ticket cannot release the next visit");
        check(!ticket.id().equals(next.id()), "New visit gets a new identity");
    }

    private static void checkThrowingPayment() {
        FakePayments provider = new FakePayments();
        PaymentProcessor flaky = new PaymentProcessor() {
            boolean first = true;
            public PaymentResult charge(String key, long amount) {
                PaymentResult result = provider.charge(key, amount);
                if (first) { first = false; throw new IllegalStateException("Response lost"); }
                return result;
            }
        };
        ParkingLot lot = lot(flaky);
        Ticket ticket = lot.enter(CAR, START);
        rejects(IllegalStateException.class, () -> lot.pay(ticket.id(), START));
        check(lot.status(ticket.id()) == TicketStatus.PAYMENT_PENDING, "Exception preserves attempt");
        check(lot.pay(ticket.id(), START) == PaymentResult.SUCCESS, "Retry after exception");
        check(provider.successfulCharges() == 1, "No duplicate charge after exception");
    }

    private static void checkConcurrentGates() throws Exception {
        ParkingLot lot = lot(new FakePayments());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                final int number = i;
                results.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    try {
                        new EntranceGate(lot).enter(new Vehicle("CAR-" + number, VehicleType.CAR), START);
                        return true;
                    } catch (IllegalStateException e) {
                        if (!"No compatible spot".equals(e.getMessage())) throw e;
                        return false;
                    }
                }));
            }
            check(ready.await(5, TimeUnit.SECONDS), "Workers ready");
            go.countDown();
            int winners = 0;
            for (Future<Boolean> result : results) if (result.get(5, TimeUnit.SECONDS)) winners++;
            check(winners == 1, "Only one gate gets the last spot");
        } finally { pool.shutdownNow(); }
    }

    public static void main(String[] args) throws Exception {
        checkPricing();
        checkPaymentLifecycle();
        checkThrowingPayment();
        checkConcurrentGates();
        ParkingLot lot = lot(new FakePayments());
        rejects(IllegalStateException.class, () -> lot.enter(new Vehicle("BIKE-1", VehicleType.BIKE), START));
        rejects(IllegalArgumentException.class, () -> new Vehicle(" ", VehicleType.CAR));
        rejects(IllegalArgumentException.class, () -> new ParkingLot(
                List.of(new Spot(1, VehicleType.CAR, 1), new Spot(1, VehicleType.CAR, 2)),
                new NearestEntry(), new HourlyPricing(), new FakePayments()));
        System.out.println("Checkpoint 4 passed: pricing boundaries, payment failures/retries, lifecycle, validation and concurrent gates.");
    }
}
```

</details>


### A full dry run

There is one car spot, S1. The car rate is INR 50 per started hour.

| Step | Event | State | Occupancy | Financial effect |
|---|---|---|---|---|
| 1 | A enters at 10:00 | ACTIVE | S1 → T1 | None |
| 2 | Another car attempts entry | ACTIVE | S1 → T1 | Rejected; no mutation |
| 3 | A requests payment at 11:00:01 | PAYMENT_PENDING | S1 → T1 | Quote fixed at INR 100, key T1:1 |
| 4 | Provider declines | ACTIVE | S1 → T1 | No successful charge |
| 5 | A retries payment | PAYMENT_PENDING | S1 → T1 | New attempt T1:2, same quote |
| 6 | Provider charges but response is unknown | PAYMENT_PENDING | S1 → T1 | Provider has one successful INR 100 charge |
| 7 | Exit is attempted | PAYMENT_PENDING | S1 → T1 | Rejected; payment not confirmed locally |
| 8 | Payment is retried at 12:00:01 | PAID | S1 → T1 | Same T1:2 resolves success; no extra charge |
| 9 | Logical exit confirmed | CLOSED | Empty | INR 100 receipt saved |
| 10 | Exit repeated | CLOSED | Empty | Original receipt returned |
| 11 | A enters again with T2 | T2 ACTIVE | S1 → T2 | New visit |
| 12 | Old T1 exit repeated again | T1 CLOSED | S1 → T2 | T2's occupancy is untouched |

### What the executable checks

The final `main()` checks minimum billing, exact-hour billing, a one-nanosecond boundary, negative duration, duplicate plates, full/incompatible capacity, unknown tickets, unpaid exit, decline/retry, unresolved outcomes, thrown payment exceptions, stable quotes, repeated payment, repeated exit, re-entry, old-ticket isolation, duplicate configuration IDs, blank plates and two concurrent gates competing for one spot.

The concurrency check starts two workers behind a latch and asserts exactly one succeeds. It is a useful regression check, not a formal proof of every schedule. The correctness argument is that all relevant mutations and reads share one monitor and mutable session state stays internal.

### Where this model deliberately ends

- State and the fake provider's idempotency records disappear on restart. No persistence or crash recovery is implemented.
- Completed sessions/receipts are retained indefinitely. A real retention policy must preserve retry guarantees while bounding storage.
- Quoted fees never expire. There is no charge for staying longer after the initial quote.
- The final `exit()` represents logical departure confirmation. Barrier opening, crossing sensors, no-shows and physical spot occupancy require more states and event handling.
- Availability is a snapshot. Seeing “1 free” does not reserve that spot; `enter()` remains authoritative.
- The code validates the essential learning boundaries; it does not implement authentication, license-plate recognition, operator permissions or a complete plate-format policy.
- Timing is supplied by the trusted caller for deterministic examples. Real adapters should use a server clock and handle clock corrections according to policy.

## 5. Alternative designs and interview follow-ups

These are design sketches, not additional runnable versions. Use them when a new requirement justifies the extra objects.

### A. A manager/factory-oriented vocabulary

You may encounter designs organized around `ParkingSpotManager`, `ParkingStrategy`, `CostComputation`, `PricingStrategy`, `EntranceGate` and `ExitGate`. Here is one possible decomposition, shown to help you translate responsibilities. It is not presented as a reconstruction of the inaccessible video.

![diagram](./STUDY_GUIDE.rendered-10.svg)

| Responsibility | This guide's implementation | Alternative name or owner |
|---|---|---|
| Own occupancy and allocate | `ParkingLot` plus `AllocationStrategy` | `ParkingSpotManager` plus `ParkingStrategy` |
| Calculate a fee | `PricingStrategy` | `CostComputation` delegating to tariff strategies |
| Coordinate tickets and payment | `ParkingLot` | `ParkingService` |
| Select an implementation from configuration | Constructor calls in `main()` | A factory |

Separating `ParkingSpotManager` becomes useful when inventory operations grow. It also creates a new question: who atomically coordinates spot assignment with ticket creation? A manager returning a free spot without claiming it is insufficient. A service lock or transaction must still encompass the related state changes.

Separate two-wheeler and four-wheeler managers can work if they encapsulate genuinely different rules. If both merely scan lists and mark occupancy, a shared manager with typed configuration avoids duplicating that logic. `CostComputation` is useful if it coordinates taxes, discounts or tariff selection; a pass-through wrapper around a single strategy is optional.

### B. Multiple floors and gate-specific distance

Add `ParkingFloor` when floor identity affects allocation, closures, capacity or displays. Give spots globally unique IDs or use a composite `(floorId, spotNumber)` key.

![diagram](./STUDY_GUIDE.rendered-11.svg)

A single `distanceFromEntry` cannot express distances from every gate. Pass the current gate identity into the allocation context, then rank candidates using a distance table or function. Decide whether “nearest” means walking distance, driving distance, or lowest floor first. The algorithm cannot infer the business definition.

### C. Compatibility beyond exact type matching

If bikes may use car spaces, do not rely on enum ordinals to imply size. Define a compatibility matrix or `FitPolicy`. Then define preference separately: perhaps bike spots first, car spots only as fallback.

A truck needing several adjacent spaces changes the unit of allocation from one spot to a set. Reserve the whole compatible set atomically or reserve none. A new `TRUCK` enum value alone does not solve adjacency or partial reservation.

### D. Display boards and Observer

A display can initially call `available(type)`. If multiple displays need updates after allocation or release, emit an event after the operation commits. Subscribers can refresh their view.

![diagram](./STUDY_GUIDE.rendered-12.svg)

Observer is justified by the need to notify independent consumers. A display failure should not undo a successful allocation. Availability on the board may lag; the allocation service must recheck the real state.

### E. Adding discounts

You could implement a pricing decorator that wraps another `PricingStrategy`, obtains its amount, and applies a discount. That is useful when combinations of adjustments need to compose. Specify order: applying a cap before a discount may produce a different result from discounting before capping. Define rounding in minor units and keep the result nonnegative.

### F. Should ParkingLot be a Singleton?

Multiple gates need the same service instance. Constructor injection already achieves that. A Singleton is not required by the fact that today's example has one lot, and it complicates isolated tests and multiple-lot deployments. “One shared instance in this application” and “a globally enforced Singleton type” are different choices.

## 6. From an interview demo to durable operations

The following is a production discussion, not a claim that the sample implements distributed transactions.

### Move real payments outside the inventory lock

1. In a short transaction or lock, load the session, freeze the quote, and create or find its pending payment attempt. Persist the key and amount.
2. Release that transaction/lock before calling the payment provider.
3. Call the provider using the saved key. An unknown result keeps the attempt pending.
4. In a new transaction, apply the confirmed outcome only if it still corresponds to that attempt. A version or conditional update prevents conflicting transitions.
5. Let logical exit atomically release the allocation, close the session and save the receipt.

Concurrent callers may reach the provider with the same key. The provider adapter must deduplicate the attempt, and the database must ensure there is only one current attempt for the visit. A crash after charging but before recording success is resolved through the saved key, provider status/retry semantics and reconciliation. A database transaction does not roll back an external charge.

### A possible durable data model

| Record | Important data and constraint |
|---|---|
| Spot | Unique spot ID, type and optional floor |
| Visit | Ticket ID, vehicle facts, assigned spot, entry time, lifecycle state |
| CurrentAllocation | Unique spot ID and unique normalized plate, referencing the current visit |
| Quote | Visit, billing cutoff, amount, currency and tariff/version identity |
| PaymentAttempt | Unique idempotency key, visit, attempt number, immutable amount, outcome, provider reference |
| Receipt | Unique ticket ID, confirmed exit time, settled amount, successful payment reference |

Historical visits can all reference the same spot. Put exclusivity on current allocations, or use an appropriate conditional uniqueness design, so historical records do not incorrectly prohibit reuse.

Two application processes do not share a Java `synchronized` monitor. Use database transactions and uniqueness/conditional updates to enforce current allocation. If the response to entry is lost, plate-based rejection only prevents duplication; it does not return the original ticket. Add an entry request idempotency key if entry retries must recover the same ticket.

## 7. Complexity and correctness review

Let `S` be the number of spots, `A` the number of current visits, and `H` the number of retained historical visits.

| Operation | Checkpoint 1 | Checkpoint 2 | Checkpoints 3–4 |
|---|---|---|---|
| Entry | O(S) scan | O(S) scan | O(S) filtering and selection |
| Exit | O(S) plate search | O(S) spot lookup | Expected O(1) map operations, excluding provider latency |
| Availability | Not exposed | Not exposed | Checkpoint 4: O(S) |
| Stored state | O(S) | O(S + A + H) | O(S + A + H), plus provider attempt history in checkpoint 4 |

Candidate lists use O(S) temporary memory in checkpoints 3–4. The nearest implementation scans for the minimum; it does not sort the entire list. Hash-map lookup is described as expected O(1), not a universal worst-case guarantee. Lock waiting and real provider latency are separate from these data-structure costs.

For a larger lot, free-spot indexes by type, ordered sets or priority queues can reduce searches. Multiple gate-specific distance orders make maintaining such indexes more complicated. Optimize after defining which order matters; an index is another piece of state that must stay consistent during allocate/release.

### Common mistakes you should be able to explain

| Mistake | Why it breaks | Better choice |
|---|---|---|
| Start with a dozen classes and patterns | There is no requirement explaining their roles | Grow the design at explicit checkpoints |
| Store mutable payment status on the returned ticket | The caller can bypass the lifecycle | Immutable ticket; internal session |
| Use only a plate to identify a visit | An old exit can target a later visit | Ticket ID per entry |
| Keep unrelated `isEmpty` and occupant values | They can contradict each other | One occupancy source |
| Free a spot before payment succeeds | Declines and timeouts lose the allocation | Release only through the exit transition |
| Treat timeout as definitive failure | A successful charge may have lost its response | Pending state and stable payment key |
| Recalculate the amount on an unresolved retry | Same key can refer to different amounts | Persist a quote/attempt amount |
| Use only a concurrent map | Compound allocation is still racy | Lock/transaction across related state changes |
| Hold a global lock during network I/O | One slow payment stalls unrelated requests | Persist attempt, call outside the lock, reconcile |
| Use integer division for started-hour billing | Partial hours are truncated | Explicit round-up rule |
| Use `double` for money | Binary fractions complicate exact monetary arithmetic | Minor units or a defined decimal-money type |
| Claim a Singleton solves multi-process coordination | Each process still has its own instance | Durable constraints and transactions |

## 8. Practice: derive the next change yourself

Try these before reading the answer notes.

1. A car parks for exactly two hours. Then try two hours plus one nanosecond. What should the charges be?
2. A bike arrives when only car spots are empty. What does the current code do? Which two decisions change if fallback is allowed?
3. You add `DailyCappedPricing`. Which workflow methods should change?
4. Two gates see the same final free spot. Which operations must share the critical section?
5. Payment was charged, but the response was lost. Which status, key and amount should the retry use?
6. A paid car remains in the lot for a day. What happens today, and which business rule is missing?
7. A duplicate exit arrives after the spot has been assigned to a new ticket. Why is the new visit safe?
8. Someone passes a strategy returning a spot outside its candidate list. Where is it rejected?
9. You deploy two JVMs, each with its own `ParkingLot`. Why can both still allocate the same physical spot?
10. The entry response is lost. How does duplicate-plate protection differ from idempotent entry recovery?

<details>
<summary>Answer notes</summary>

1. INR 100 and INR 150 for a car, respectively.
2. Entry is rejected. Add a compatibility rule and a preference order; do not accidentally make bikes consume car spots before bike spaces.
3. Supply a new strategy through configuration. Entry and exit orchestration should remain unchanged. Decide whether the cap is per rolling 24 hours, calendar day or visit.
4. Duplicate-plate check, candidate selection, occupancy claim and ticket/plate registration. Release and availability must use the same synchronization discipline.
5. `PAYMENT_PENDING`, the existing attempt key and the original quoted amount. Resolve the provider's actual outcome.
6. The quote stays fixed and the spot stays allocated until exit; no extra fee is charged. Define quote expiration and post-payment exit grace behavior.
7. The old session is already `CLOSED`; the method returns its saved receipt before touching occupancy.
8. The lot checks membership in the eligible list before creating the allocation.
9. Java monitors are process-local. Enforce allocation constraints in shared durable storage.
10. Plate protection rejects a second active entry; request idempotency lets a retry retrieve the original successful ticket.

</details>

### A suggested interview presentation

Use the first few minutes to establish types, compatibility, pricing and scope. Walk through one arrival and departure. Draw `Vehicle`, `Spot`, `Ticket` and the coordinating lot. Explain ownership and duplicate-entry protection. Introduce strategies only when describing alternate allocation or pricing. Then discuss states and the last-spot race if payments or multiple gates are in scope. Finish with a concrete failure/retry example and the limits of the in-memory model.

A concise explanation you can practice:

> Each entry creates a ticket identifying one visit. The lot owns occupancy and the current plate index. It filters compatible free spots, then delegates preference to an allocation strategy. A pricing strategy calculates a fee from the ticket and billing cutoff. Once payments are introduced, a private session tracks active, pending, paid and closed states. Pending attempts retain their key and amount; only confirmed payment followed by exit releases a spot. All mutable operations share one lot lock in the demo; persistent multi-process implementations need transactions and constraints.

## 9. Run and explore

From the `LLD` directory, using Java 17 or newer:

```powershell
java --source 17 ParkingLot/checkpoint1/BasicParkingDemo.java
java --source 17 ParkingLot/checkpoint2/TicketParkingDemo.java
java --source 17 ParkingLot/checkpoint3/PolicyParkingDemo.java
java --source 17 ParkingLot/checkpoint4/PaymentParkingDemo.java
```

Each file is standalone. Run versions separately; do not combine their nested domain models into one application. Every executable prints a checkpoint success line or throws if a check fails. The source-file launcher avoids creating compiled class files in your workspace.

The HTML edition includes rendered SVG diagrams and embedded source code. The Markdown edition keeps editable Mermaid diagrams. Use the source links or expand a checkpoint's full code to compare the design with its implementation.

**Source boundary:** The external reference is the user-supplied [Hindi parking-lot video](https://www.youtube.com/watch?v=MtjZf7291zc) and its metadata-linked [English companion](https://www.youtube.com/watch?v=g8AVGr_0Wko). All requirements, checkpoint designs, code, diagrams, tests and exercises in this document are original explanatory material. No inaccessible transcript or video-specific timestamp is represented as verified.
