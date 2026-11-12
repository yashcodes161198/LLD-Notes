package notificationrouter.model;

// Immutable value object created by the calling service (Req 2).
public final class Notification {
    private final String id;
    private final String userId;
    private final Priority priority;
    private final String content;

    public Notification(String id, String userId, Priority priority, String content) {
        if (isBlank(id) || isBlank(userId) || priority == null) {
            throw new IllegalArgumentException("invalid notification");
        }
        this.id = id;
        this.userId = userId;
        this.priority = priority;
        this.content = content;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public Priority getPriority() { return priority; }
    public String getContent() { return content; }

    public boolean isUrgent() { return priority == Priority.URGENT; }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    @Override
    public String toString() { return id + "(" + userId + ", " + priority + ")"; }
}
