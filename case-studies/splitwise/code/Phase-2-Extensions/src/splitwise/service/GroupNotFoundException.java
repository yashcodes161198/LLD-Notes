package splitwise.service;

// No group with this id.
public class GroupNotFoundException extends RuntimeException {
    public GroupNotFoundException(String message) { super(message); }
}
