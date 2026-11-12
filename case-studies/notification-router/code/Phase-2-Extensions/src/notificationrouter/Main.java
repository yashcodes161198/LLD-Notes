package notificationrouter;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import notificationrouter.handler.ChannelHandler;
import notificationrouter.handler.EmailHandler;
import notificationrouter.handler.PushHandler;
import notificationrouter.handler.SmsHandler;
import notificationrouter.handler.WhatsAppHandler;
import notificationrouter.model.Channel;
import notificationrouter.model.DeliveryStatus;
import notificationrouter.model.Notification;
import notificationrouter.model.NotificationType;
import notificationrouter.model.Priority;
import notificationrouter.policy.AllChannelsPolicy;
import notificationrouter.policy.PreferredOnlyPolicy;
import notificationrouter.policy.PreferredPlusPushPolicy;
import notificationrouter.policy.PreferredWithFallbackPolicy;
import notificationrouter.policy.RoutingPolicy;
import notificationrouter.preference.UserPreference;
import notificationrouter.router.NoPreferredChannelException;
import notificationrouter.router.NotificationRouter;

import static notificationrouter.model.Channel.*;
import static notificationrouter.model.DeliveryStatus.*;
import static notificationrouter.model.NotificationType.*;
import static notificationrouter.model.Priority.*;

// Self-checks: each one throws if the router misbehaves.
public class Main {
    public static void main(String[] args) {
        baseBehaviourStillHolds();
        ext1NewChannel();
        ext2PerTypePreferences();
        ext3Fallback();
        ext4HighPriority();
        badConfigFailsAtStartup();
        demoWithRealHandlers();
        System.out.println("\nAll checks passed.");
    }

    // The Phase 1 trace, now through the policy map.
    static void baseBehaviourStillHolds() {
        System.out.println("=== Base trace (SMS gateway down) ===");
        Fixture f = new Fixture(Set.of(SMS), defaultPolicies());
        f.router.setPreference("u1", SMS);
        f.router.setPreference("u2", EMAIL);

        expect(f.router.route(note("n1", "u2", NORMAL, ORDER_UPDATE)), Map.of(EMAIL, SENT), "n1 u2 NORMAL -> EMAIL");
        expect(f.router.route(note("n2", "u1", NORMAL, ORDER_UPDATE)), Map.of(SMS, FAILED), "n2 u1 NORMAL -> SMS fails");
        expect(f.router.route(note("n3", "u1", URGENT, SECURITY)),
                Map.of(EMAIL, SENT, SMS, FAILED, PUSH, SENT, WHATSAPP, SENT), "n3 u1 URGENT -> every channel");
        check(f.handler(SMS).attempts == 2, "SMS tried once per notification, no double send");
        expectThrows(() -> f.router.route(note("n4", "u9", NORMAL, ORDER_UPDATE)), NoPreferredChannelException.class,
                "n4 u9 NORMAL, no preference");
        f.router.setPreference("u1", PUSH);
        expect(f.router.route(note("n7", "u1", NORMAL, ORDER_UPDATE)), Map.of(PUSH, SENT), "n7 newest preference wins");
    }

    // EXT 1: WHATSAPP was added to the enum + one handler; URGENT picks it up, router unchanged.
    // URGENT can also be limited to a subset (WhatsApp costs money per message).
    static void ext1NewChannel() {
        System.out.println("\n=== EXT 1: new channel ===");
        Fixture f = new Fixture(Set.of(), defaultPolicies());
        f.router.setPreference("u1", WHATSAPP);
        expect(f.router.route(note("w1", "u1", NORMAL, ORDER_UPDATE)), Map.of(WHATSAPP, SENT), "NORMAL to WHATSAPP");

        Map<Priority, RoutingPolicy> cheapUrgent = defaultPolicies();
        cheapUrgent.put(URGENT, new AllChannelsPolicy(EnumSet.of(EMAIL, SMS, PUSH)));
        Fixture g = new Fixture(Set.of(), cheapUrgent);
        expect(g.router.route(note("w2", "u1", URGENT, SECURITY)), Map.of(EMAIL, SENT, SMS, SENT, PUSH, SENT),
                "URGENT without WHATSAPP");
    }

    // EXT 2: several default channels, a per-type override, and a MARKETING opt-out.
    static void ext2PerTypePreferences() {
        System.out.println("\n=== EXT 2: multi-channel and per-type preferences ===");
        Fixture f = new Fixture(Set.of(), defaultPolicies());
        f.router.setPreference("u1", new UserPreference(
                EnumSet.of(EMAIL, PUSH),
                Map.of(SECURITY, EnumSet.of(SMS), MARKETING, EnumSet.noneOf(Channel.class))));

        expect(f.router.route(note("p1", "u1", NORMAL, ORDER_UPDATE)), Map.of(EMAIL, SENT, PUSH, SENT),
                "ORDER_UPDATE -> default set {EMAIL, PUSH}");
        expect(f.router.route(note("p2", "u1", NORMAL, SECURITY)), Map.of(SMS, SENT), "SECURITY -> override {SMS}");
        expect(f.router.route(note("p3", "u1", NORMAL, MARKETING)), Map.of(), "MARKETING -> opted out, nothing sent");
        check(f.handler(EMAIL).sent.size() == 1, "opt-out must not reach any handler");
        expectThrows(() -> new UserPreference(EnumSet.noneOf(Channel.class), Map.of()), IllegalArgumentException.class,
                "empty default channels");
    }

    // EXT 3: NORMAL with fallback order [PUSH, EMAIL, SMS]; SMS and PUSH are down.
    static void ext3Fallback() {
        System.out.println("\n=== EXT 3: fallback (SMS and PUSH down) ===");
        Map<Priority, RoutingPolicy> policies = defaultPolicies();
        policies.put(NORMAL, new PreferredWithFallbackPolicy(List.of(PUSH, EMAIL, SMS)));
        Fixture f = new Fixture(Set.of(SMS, PUSH), policies);
        f.router.setPreference("u1", SMS);
        f.router.setPreference("u2", EMAIL);
        f.router.setPreference("u3", new UserPreference(EnumSet.of(SMS), Map.of(MARKETING, EnumSet.noneOf(Channel.class))));

        expect(f.router.route(note("f1", "u1", NORMAL, ORDER_UPDATE)), Map.of(SMS, FAILED, PUSH, FAILED, EMAIL, SENT),
                "SMS fails -> PUSH fails -> EMAIL sent, stop");
        check(f.handler(SMS).attempts == 1, "fallback never retries the preferred channel");
        expect(f.router.route(note("f2", "u2", NORMAL, ORDER_UPDATE)), Map.of(EMAIL, SENT), "preferred works -> no fallback");
        expect(f.router.route(note("f3", "u3", NORMAL, MARKETING)), Map.of(), "opted out -> no fallback either");
        expectThrows(() -> new PreferredWithFallbackPolicy(List.of()), IllegalArgumentException.class, "empty fallback order");
    }

    // EXT 4: HIGH = preferred + PUSH, a new strategy registered in the policy map.
    static void ext4HighPriority() {
        System.out.println("\n=== EXT 4: HIGH priority ===");
        Fixture f = new Fixture(Set.of(), defaultPolicies());
        f.router.setPreference("u1", EMAIL);
        f.router.setPreference("u2", PUSH);
        expect(f.router.route(note("h1", "u1", HIGH, ORDER_UPDATE)), Map.of(EMAIL, SENT, PUSH, SENT), "HIGH -> EMAIL + PUSH");
        expect(f.router.route(note("h2", "u2", HIGH, ORDER_UPDATE)), Map.of(PUSH, SENT), "HIGH, prefers PUSH -> PUSH once");
        check(f.handler(PUSH).attempts == 2, "PUSH sent once per notification");
        expectThrows(() -> f.router.route(note("h3", "u9", HIGH, ORDER_UPDATE)), NoPreferredChannelException.class,
                "HIGH, no preference");
    }

    static void badConfigFailsAtStartup() {
        System.out.println("\n=== Bad config fails at startup ===");
        Map<Channel, ChannelHandler> noWhatsApp = Map.of(EMAIL, n -> {}, SMS, n -> {}, PUSH, n -> {});
        expectThrows(() -> new NotificationRouter(noWhatsApp, defaultPolicies()), IllegalArgumentException.class,
                "WHATSAPP has no handler");
        Map<Priority, RoutingPolicy> noHigh = defaultPolicies();
        noHigh.remove(HIGH);
        expectThrows(() -> new Fixture(Set.of(), noHigh), IllegalArgumentException.class, "HIGH has no policy");
    }

    // Production wiring: the stub handlers print instead of calling a provider.
    static void demoWithRealHandlers() {
        System.out.println("\n=== Demo with the stub handlers ===");
        Map<Priority, RoutingPolicy> policies = defaultPolicies();
        policies.put(NORMAL, new PreferredWithFallbackPolicy(List.of(PUSH, EMAIL, SMS)));
        NotificationRouter router = new NotificationRouter(Map.of(
                EMAIL, new EmailHandler(), SMS, new SmsHandler(), PUSH, new PushHandler(), WHATSAPP, new WhatsAppHandler()),
                policies);
        router.setPreference("alice", new UserPreference(EnumSet.of(SMS), Map.of(MARKETING, EnumSet.noneOf(Channel.class))));
        expect(router.route(new Notification("d1", "alice", NORMAL, ORDER_UPDATE, "Your order shipped")),
                Map.of(SMS, SENT), "NORMAL -> SMS (no fallback needed)");
        expect(router.route(new Notification("d2", "alice", HIGH, ORDER_UPDATE, "Out for delivery")),
                Map.of(SMS, SENT, PUSH, SENT), "HIGH -> SMS + PUSH");
        expect(router.route(new Notification("d3", "alice", NORMAL, MARKETING, "50% off")),
                Map.of(), "MARKETING -> opted out");
        expect(router.route(new Notification("d4", "alice", URGENT, SECURITY, "New login from Berlin")),
                Map.of(EMAIL, SENT, SMS, SENT, PUSH, SENT, WHATSAPP, SENT), "URGENT -> every channel");
    }

    // ---- helpers ----

    static Map<Priority, RoutingPolicy> defaultPolicies() {
        Map<Priority, RoutingPolicy> policies = new java.util.EnumMap<>(Priority.class);
        policies.put(NORMAL, new PreferredOnlyPolicy());
        policies.put(HIGH, new PreferredPlusPushPolicy());
        policies.put(URGENT, new AllChannelsPolicy());
        return policies;
    }

    // A router with one RecordingHandler per channel; channels in `down` always fail.
    static class Fixture {
        final Map<Channel, RecordingHandler> handlers = new java.util.EnumMap<>(Channel.class);
        final NotificationRouter router;

        Fixture(Set<Channel> down, Map<Priority, RoutingPolicy> policies) {
            for (Channel c : Channel.values()) handlers.put(c, new RecordingHandler(down.contains(c)));
            router = new NotificationRouter(Map.copyOf(handlers), policies);
        }

        RecordingHandler handler(Channel c) { return handlers.get(c); }
    }

    static class RecordingHandler implements ChannelHandler {
        final boolean down;
        final List<String> sent = new ArrayList<>();
        int attempts;

        RecordingHandler(boolean down) { this.down = down; }

        @Override
        public void send(Notification notification) {
            attempts++;
            if (down) throw new IllegalStateException("gateway down");
            sent.add(notification.getId());
        }
    }

    static Notification note(String id, String userId, Priority priority, NotificationType type) {
        return new Notification(id, userId, priority, type, "hello");
    }

    static void expect(Map<Channel, DeliveryStatus> got, Map<Channel, DeliveryStatus> want, String label) {
        check(got.equals(want), label + ": expected " + want + ", got " + got);
        System.out.println("OK     " + label + "  " + got);
    }

    static void expectThrows(Runnable action, Class<? extends RuntimeException> type, String label) {
        try {
            action.run();
        } catch (RuntimeException e) {
            check(type.isInstance(e), label + ": expected " + type.getSimpleName() + ", got " + e);
            System.out.println("THROWS " + label + " -> " + e.getMessage());
            return;
        }
        throw new IllegalStateException(label + ": expected an exception");
    }

    static void check(boolean ok, String message) {
        if (!ok) throw new IllegalStateException("CHECK FAILED: " + message);
    }
}
