package splitwise.notifier;

// Sends a mobile push (e.g. FCM, APNs).
public class PushNotifier implements Notifier {
    @Override
    public void notify(String userId, String message) {
        // stub: real code would look up the user's device tokens and call the push service; a provider error should throw.
        System.out.println("[PUSH] to " + userId + ": " + message);
    }
}
