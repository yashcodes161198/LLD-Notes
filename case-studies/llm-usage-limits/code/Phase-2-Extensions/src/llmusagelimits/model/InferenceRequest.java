package llmusagelimits.model;

import java.util.List;

// Immutable value object (Req 1). Validates itself, so an invalid request can't exist.
public final class InferenceRequest {
    private final String userId;
    private final Model model;
    private final List<String> messages;

    public InferenceRequest(String userId, Model model, List<String> messages) {
        if (userId == null || userId.isBlank() || model == null || messages == null || messages.isEmpty()
                || messages.stream().anyMatch(m -> m == null)) {
            throw new IllegalArgumentException("invalid request");
        }
        this.userId = userId;
        this.model = model;
        this.messages = List.copyOf(messages);
    }

    public String getUserId() { return userId; }
    public Model getModel() { return model; }
    public List<String> getMessages() { return messages; }

    public int totalChars() {
        int chars = 0;
        for (String m : messages) chars += m.length();
        return chars;
    }
}
