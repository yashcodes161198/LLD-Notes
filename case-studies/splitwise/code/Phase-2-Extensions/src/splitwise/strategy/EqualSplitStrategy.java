package splitwise.strategy;

import java.util.Collections;
import java.util.List;

import splitwise.model.Split;
import splitwise.service.InvalidSplitException;

public class EqualSplitStrategy implements SplitStrategy {
    @Override
    public List<Split> split(long amountCents, List<String> participants, List<Long> values) {
        if (!values.isEmpty()) throw new InvalidSplitException("EQUAL takes no values");
        return Splits.proportional(amountCents, participants, Collections.nCopies(participants.size(), 1L));
    }
}
