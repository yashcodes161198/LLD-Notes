package splitwise.service;

// Payer, participant or settle-up user is not in the group.
public class NotAGroupMemberException extends RuntimeException {
    public NotAGroupMemberException(String message) { super(message); }
}
