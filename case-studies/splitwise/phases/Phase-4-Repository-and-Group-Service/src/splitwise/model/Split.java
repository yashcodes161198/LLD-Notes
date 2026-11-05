package splitwise.model;

// One participant's share of an expense: "who" plus "how much".
public class Split {
    private final User user;
    private double amount;

    public Split(User user, double amount) {
        this.user = user;
        this.amount = amount;
    }

    public User getUser() { return user; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    @Override
    public String toString() { return String.format("%s=%.2f", user.getName(), amount); }
}
