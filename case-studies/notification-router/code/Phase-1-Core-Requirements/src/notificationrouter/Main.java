package notificationrouter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import notificationrouter.handler.ChannelHandler;
import notificationrouter.handler.EmailHandler;
import notificationrouter.handler.PushHandler;
import notificationrouter.handler.SmsHandler;
import notificationrouter.model.Channel;
import notificationrouter.model.DeliveryStatus;
import notificationrouter.model.Notification;
import notificationrouter.model.Priority;
import notificationrouter.router.NoPreferredChannelException;
import notificationrouter.router.NotificationRouter;

import static notificationrouter.model.Channel.*;
import static notificationrouter.model.DeliveryStatus.*;

// Self-checks: each one throws if the router misbehaves.
public class Main {
    public static void main(String[] args) {
        verificationTrace();
        badConfigFailsAtStartup();
        demoWithRealHandlers();
        System.out.println("\nAll checks passed.");
    }

    // The trace from notification_router.py: the SMS handler is down.
    static void verificationTrace() {
        System.out.println("=== Verification trace (SMS gateway down) ===");
        RecordingHandler email = new RecordingHandler(false);
        RecordingHandler sms = new RecordingHandler(true);
        RecordingHandler push = new RecordingHandler(false);
        NotificationRouter router = new NotificationRouter(Map.of(EMAIL, email, SMS, sms, PUSH, push));

        router.setPreference("u1", SMS);
        router.setPreference("u2", EMAIL);

        expect(router.route(note("n1", "u2", Priority.NORMAL)), Map.of(EMAIL, SENT), "n1 u2 NORMAL -> EMAIL");
        expect(router.route(note("n2", "u1", Priority.NORMAL)), Map.of(SMS, FAILED), "n2 u1 NORMAL -> SMS fails, no exception");

        int smsBefore = sms.attempts;
        expect(router.route(note("n3", "u1", Priority.URGENT)), Map.of(EMAIL, SENT, SMS, FAILED, PUSH, SENT),
                "n3 u1 URGENT -> all three, SMS failure isolated");
        check(sms.attempts - smsBefore == 1, "SMS must be tried exactly once for URGENT (no double send)");

        expectThrows(() -> router.route(note("n4", "u9", Priority.NORMAL)), NoPreferredChannelException.class,
                "n4 u9 NORMAL, no preference");
        expect(router.route(note("n5", "u9", Priority.URGENT)), Map.of(EMAIL, SENT, SMS, FAILED, PUSH, SENT),
                "n5 u9 URGENT -> no preference needed");
        expectThrows(() -> note("n6", "", Priority.URGENT), IllegalArgumentException.class, "n6 empty user");
        expectThrows(() -> router.route(null), IllegalArgumentException.class, "null notification");

        router.setPreference("u1", PUSH);
        expect(router.route(note("n7", "u1", Priority.NORMAL)), Map.of(PUSH, SENT), "n7 u1 NORMAL -> PUSH (newest wins)");

        expectThrows(() -> router.setPreference("", EMAIL), IllegalArgumentException.class, "empty user id");
        expectThrows(() -> router.setPreference("u1", null), IllegalArgumentException.class, "null channel");
    }

    static void badConfigFailsAtStartup() {
        System.out.println("\n=== Bad config fails at startup ===");
        RecordingHandler ok = new RecordingHandler(false);
        expectThrows(() -> new NotificationRouter(Map.of(EMAIL, ok, SMS, ok)), IllegalArgumentException.class,
                "PUSH has no handler");
    }

    // Production wiring: the stub handlers print instead of calling a provider.
    static void demoWithRealHandlers() {
        System.out.println("\n=== Demo with the stub handlers ===");
        NotificationRouter router = new NotificationRouter(Map.of(
                EMAIL, new EmailHandler(), SMS, new SmsHandler(), PUSH, new PushHandler()));
        router.setPreference("alice", SMS);
        expect(router.route(new Notification("d1", "alice", Priority.NORMAL, "Your order shipped")),
                Map.of(SMS, SENT), "NORMAL -> SMS only");
        expect(router.route(new Notification("d2", "alice", Priority.URGENT, "Payment failed")),
                Map.of(EMAIL, SENT, SMS, SENT, PUSH, SENT), "URGENT -> every channel");
    }

    // ---- helpers ----

    // Test double: records what it was asked to send, optionally always fails.
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

    static Notification note(String id, String userId, Priority priority) {
        return new Notification(id, userId, priority, "hello");
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
