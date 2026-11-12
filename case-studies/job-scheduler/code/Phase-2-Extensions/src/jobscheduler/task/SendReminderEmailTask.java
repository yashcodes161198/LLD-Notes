package jobscheduler.task;

// Sends one reminder email. Not idempotent (a retry would send it twice), so schedule it AT_MOST_ONCE.
public class SendReminderEmailTask implements Task {
    private final String to;

    public SendReminderEmailTask(String to) {
        if (to == null || to.isBlank()) throw new IllegalArgumentException("invalid recipient");
        this.to = to;
    }

    @Override
    public void run(String executionKey) {
        // stub: real code would call the email provider (e.g. SES); a provider error should throw.
        System.out.println("[EMAIL] reminder to " + to + " (" + executionKey + ")");
    }
}
