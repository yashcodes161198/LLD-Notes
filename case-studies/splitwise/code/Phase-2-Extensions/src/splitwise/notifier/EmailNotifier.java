package splitwise.notifier;

// Sends through an email API (e.g. SES, SendGrid).
public class EmailNotifier implements Notifier {
    @Override
    public void notify(String userId, String message) {
        // stub: real code would look up the user's email address and call the provider; a provider error should throw.
        System.out.println("[EMAIL] to " + userId + ": " + message);
    }
}
