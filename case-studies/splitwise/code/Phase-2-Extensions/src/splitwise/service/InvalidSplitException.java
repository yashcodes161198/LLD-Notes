package splitwise.service;

// The split values don't add up (EXACT cents != amount, PERCENT != 100%) or don't match the participants.
public class InvalidSplitException extends RuntimeException {
    public InvalidSplitException(String message) { super(message); }
}
