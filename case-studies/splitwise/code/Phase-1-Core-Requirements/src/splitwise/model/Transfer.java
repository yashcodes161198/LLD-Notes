package splitwise.model;

import java.util.Objects;

// "from pays to amountCents": one line of a settle-up plan (Req 5). Compared in tests, so equals/hashCode.
public final class Transfer {
    private final String from;
    private final String to;
    private final long amountCents;

    public Transfer(String from, String to, long amountCents) {
        if (from == null || to == null || from.equals(to)) throw new IllegalArgumentException("invalid transfer");
        if (amountCents <= 0) throw new IllegalArgumentException("transfer must be positive");
        this.from = from;
        this.to = to;
        this.amountCents = amountCents;
    }

    public String getFrom() { return from; }
    public String getTo() { return to; }
    public long getAmountCents() { return amountCents; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Transfer)) return false;
        Transfer t = (Transfer) o;
        return from.equals(t.from) && to.equals(t.to) && amountCents == t.amountCents;
    }

    @Override
    public int hashCode() { return Objects.hash(from, to, amountCents); }

    @Override
    public String toString() { return from + "->" + to + ":" + amountCents; }
}
