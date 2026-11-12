package llmusagelimits.plan;

// EXT 1: limits come from the user's plan, as a multiplier on each window's base limit.
public enum Plan {
    FREE(1), PRO(5), MAX(20);

    private final int multiplier;

    Plan(int multiplier) { this.multiplier = multiplier; }

    public int getMultiplier() { return multiplier; }
}
