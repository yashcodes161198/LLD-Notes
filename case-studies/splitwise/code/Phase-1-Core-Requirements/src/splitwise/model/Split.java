package splitwise.model;

// One participant's share of an expense, in cents. Immutable.
public final class Split {
    private final String userId;
    private final long amountCents;

    public Split(String userId, long amountCents) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("invalid user");
        if (amountCents < 0) throw new IllegalArgumentException("negative share for " + userId);
        this.userId = userId;
        this.amountCents = amountCents;
    }

    public String getUserId() { return userId; }
    public long getAmountCents() { return amountCents; }

    @Override
    public String toString() { return userId + ":" + amountCents; }
}
