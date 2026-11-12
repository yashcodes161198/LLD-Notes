package notificationrouter.model;

// Immutable value object created by the calling service (Req 2).
public final class Notification {
    private final String id;
    private final String userId;
    private final Priority priority;
    private final NotificationType type;
    private final String content;

    public Notification(String id, String userId, Priority priority, NotificationType type, String content) {
        if (isBlank(id) || isBlank(userId) || priority == null || type == null) {
            throw new IllegalArgumentException("invalid notification");
        }
        this.id = id;
        this.userId = userId;
        this.priority = priority;
        this.type = type;
        this.content = content;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public Priority getPriority() { return priority; }
    public NotificationType getType() { return type; }
    public String getContent() { return content; }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    @Override
    public String toString() { return id + "(" + userId + ", " + priority + ", " + type + ")"; }
}
