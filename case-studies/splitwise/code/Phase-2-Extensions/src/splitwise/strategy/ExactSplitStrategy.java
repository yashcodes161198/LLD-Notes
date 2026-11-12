package splitwise.strategy;

import java.util.ArrayList;
import java.util.List;

import splitwise.model.Split;
import splitwise.service.InvalidSplitException;

// Values are each participant's share in cents.
public class ExactSplitStrategy implements SplitStrategy {
    @Override
    public List<Split> split(long amountCents, List<String> participants, List<Long> values) {
        Splits.requireValues(participants, values);
        long sum = Splits.sum(values);
        if (sum != amountCents) throw new InvalidSplitException("EXACT shares sum to " + sum + ", not " + amountCents);
        List<Split> splits = new ArrayList<>();
        for (int i = 0; i < participants.size(); i++) splits.add(new Split(participants.get(i), values.get(i)));
        return splits;
    }
}
