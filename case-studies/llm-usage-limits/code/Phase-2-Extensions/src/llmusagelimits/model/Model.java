package llmusagelimits.model;

// Req 2: a bigger model burns the limit faster. Weights are illustrative.
public enum Model {
    HAIKU(1), SONNET(2), OPUS(5);

    private final int weight;

    Model(int weight) { this.weight = weight; }

    public int getWeight() { return weight; }
}
