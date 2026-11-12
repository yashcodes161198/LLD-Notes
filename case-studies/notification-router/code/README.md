# Notification Router: Java code

Runnable Java for the design in [`../notification_router.py`](../notification_router.py). Run either phase with `./run.sh` (JDK 17+). `Main` is a set of self-checks: it prints each step and throws if anything is wrong. Failed sends also print a `[warn]` line to stderr.

```
Phase-N/src/notificationrouter/
  model/       Channel, Priority, DeliveryStatus, Notification (+ NotificationType in Phase 2)
  handler/     ChannelHandler (the seam) + EmailHandler, SmsHandler, PushHandler (+ WhatsAppHandler in Phase 2): sending is stubbed with a println
  router/      NotificationRouter (orchestrator), NoPreferredChannelException
  preference/  UserPreference                      (Phase 2)
  policy/      RoutingPolicy + RoutePlan + policies (Phase 2)
  Main.java    self-checks
```

## Phase 1: Core Requirements

Requirements 1-5 from Step 1, using the best version of `route()` from Step 4 (not the TRICKY #5 trap).

| Class | Role |
|---|---|
| `NotificationRouter` | `setPreference(userId, channel)`, `route(notification) -> Map<Channel, DeliveryStatus>`. `route` = `resolveChannels` (pure, returns a `Set`, URGENT checked first) + `dispatch` (per-channel try/catch). Fails at construction if a channel has no handler. |
| `Notification` | Immutable value object. Validates itself in the constructor, so an invalid notification can't exist and nothing is half-sent. |
| `ChannelHandler` | The seam. `EmailHandler`, `SmsHandler`, `PushHandler` implement it with the provider call stubbed out (they print). `Main` uses a recording test double for the checks, then a demo with the stub handlers. |
| `NoPreferredChannelException` | The specific error for NORMAL with no preference. |

Checks in `Main`: the verification trace from the design (SMS down, URGENT tries SMS exactly once, the other channels still go out), input validation, and the missing-handler startup check.

## Phase 2: Extensions

Each extension from Step 5 lands in one place. `NotificationRouter` no longer has any routing rule in it.

| Extension | Pattern | What changed |
|---|---|---|
| 1. New channel | Open/closed via the handler map | `WHATSAPP` added to `Channel`, plus one handler. URGENT picks it up. `AllChannelsPolicy` can take a subset if paid channels should be left out. |
| 2. Multiple / per-type preferences | Promote the map value to a class | `UserPreference` (default set + per-`NotificationType` sets; an empty set = opt-out). `setPreference(userId, Channel)` still works. |
| 3. Fallback | Ordered fallback list | `PreferredWithFallbackPolicy`: if every preferred channel fails, try the fallback order and stop at the first success. Opt-in per priority (product decision). Never falls back for an opted-out type. |
| 4. More priorities | Strategy | `Map<Priority, RoutingPolicy>`: `PreferredOnlyPolicy` (NORMAL), `PreferredPlusPushPolicy` (new HIGH), `AllChannelsPolicy` (URGENT). Policies return a `RoutePlan` (primary set + fallback list); the router just executes it. Fails at construction if a priority has no policy. |

Also: preferences live in a `ConcurrentHashMap` of immutable `UserPreference`s, read once per `route()`, so concurrent `setPreference` calls are safe.

Not coded (answered in words in the design's "More follow-ups"): URGENT caller allow-lists and caps, queues + idempotent handlers, a SKIPPED status for missing contact details, audit logging.
