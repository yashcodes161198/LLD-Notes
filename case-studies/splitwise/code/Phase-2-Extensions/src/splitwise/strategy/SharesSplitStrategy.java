package splitwise.strategy;

import java.util.List;

import splitwise.model.Split;
import splitwise.service.InvalidSplitException;

// EXT 1: values are weights, e.g. 2:1:1 for a couple and two singles. Any positive total works.
public class SharesSplitStrategy implements SplitStrategy {
    @Override
    public List<Split> split(long amountCents, List<String> participants, List<Long> values) {
        Splits.requireValues(participants, values);
        if (Splits.sum(values) == 0) throw new InvalidSplitException("SHARES needs at least one share");
        return Splits.proportional(amountCents, participants, values);
    }
}
