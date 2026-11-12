# Answer key 02: Entities + class design (100 pts)

Evaluator: follow `../RULEBOOK.md` §3. Names don't have to match exactly; responsibilities do.

## Part A: Entities (35 pts)

### E1: Noun list · 5 pts
- **Must:** at least 6 of: carrier/driver, package, size, compartment, locker/system, access token/code, user/customer, staff, sender.
- 1 pt per noun up to 5.

### E2: Keep/drop decisions · 15 pts
| Noun | Expected | Reason that must appear | Pts |
|---|---|---|---|
| Driver/Carrier | DROP | external actor; we store no state about it | 2 |
| User/Customer | DROP | external actor; only types a code | 2 |
| Staff | DROP | external actor | 1 |
| Package | DROP | owned by another system; only size matters | 4 |
| Locker | KEEP | orchestrator / entry point | 2 |
| Compartment | KEEP | physical slot with state (size, occupied) | 2 |
| AccessToken | KEEP | carries rules: expiry, maps to a compartment | 2 |
- **Red flag −6:** keeps Package as a class *without* saying why (e.g. needing tracking IDs, which is out of scope). If they keep it with an explicit, defensible reason, −2 only.
- **Red flag −4:** keeps User or Driver as classes.

### E3: Dropped noun whose attribute survives · 6 pts
- **Must:** Package → `size` → becomes a **parameter** of `depositPackage(size)`.

### E4: The "just a string" entity · 6 pts
- **Must:** AccessToken is more than a string because it has an **expiration** (3) and grants the right to a **specific compartment** (3). Bonus wording: "bearer token".

### E5: Enums · 3 pts
- **Must:** `Size {SMALL, MEDIUM, LARGE}`.

## Part B: Class design (40 pts)

### C1: Locker (orchestrator) · 18 pts
| Member | Pts | Accept |
|---|---|---|
| `- compartments: Compartment[]` | 3 | any list/collection |
| `- accessTokensMapping: Map<string, AccessToken>` | 4 | a list of tokens gets 2 |
| `+ depositPackage(size) -> string \| error` | 4 | must take **size** and **return the code** (−2 if it returns void, or takes a Package) |
| `+ pickup(code) -> void \| error` | 4 | any name like retrieve/collect |
| `+ openExpiredCompartments() -> void` | 3 | any name like clearExpired |

### C2: AccessToken · 10 pts
- `code: string` (2), `expiration: timestamp` (2), `compartment: Compartment` (2), `isExpired()` (2), `getCompartment()` / `getCode()` (2).
- **Red flag −3:** expiry checked by the Locker comparing timestamps instead of `token.isExpired()` (breaks Information Expert).

### C3: Compartment · 8 pts
- `size: Size` (2), `occupied: boolean` (or status) (2), `isOccupied()` / `getSize()` (1), `markOccupied()` / `markFree()` (2), `open()` (1).

### C4: Top-down and traceability · 4 pts
- Locker written first (2), and members annotated with the requirement they come from (2).

## Part C: Design decisions (25 pts)

### D1: Map vs list · 5 pts
- **Must:** a map from code to token, chosen **for clarity of intent**. O(1) is incidental because n ≈ 50–100. If the only answer is "O(1) lookup", give 2 pts.

### D2: Where occupancy lives · 7 pts
- **Must:** on **Compartment** (3); principle **Information Expert** or encapsulation (2); rule: **physical/intrinsic state lives on the entity, relationships live in the orchestrator** (2).

### D3: Rejected alternative · 5 pts
- **Must:** size-indexed free queues or an occupied-set on Locker → O(1), but **duplicated state that can drift out of sync**, and unnecessary at this n.

### D4: A wrong approach · 5 pts
- **Must:** deriving occupancy from "has a valid token". It breaks for an **expired token**: the token is invalid but the compartment is still physically full.

### D5: Methods not implemented · 3 pts
- **Must:** getters/setters, `isExpired` (a one-line comparison), and `open()` (hardware).

---
**Level calibration:** ≥85 senior · 70–84 mid · 50–69 junior · <50 redo Step 2–3 of `../amazon_locker.py`.
