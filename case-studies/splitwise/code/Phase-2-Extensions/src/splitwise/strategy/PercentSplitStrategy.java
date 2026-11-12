package splitwise.strategy;

import java.util.List;

import splitwise.model.Split;
import splitwise.service.InvalidSplitException;

// Values are basis points (10000 = 100%), so 33.33% is exact.
public class PercentSplitStrategy implements SplitStrategy {
    private static final long FULL_PERCENT_BP = 10_000;

    @Override
    public List<Split> split(long amountCents, List<String> participants, List<Long> values) {
        Splits.requireValues(participants, values);
        long sum = Splits.sum(values);
        if (sum != FULL_PERCENT_BP) throw new InvalidSplitException("PERCENT must total 10000 bp, got " + sum);
        return Splits.proportional(amountCents, participants, values);
    }
}
