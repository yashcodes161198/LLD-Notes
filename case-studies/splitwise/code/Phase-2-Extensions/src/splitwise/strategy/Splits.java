package splitwise.strategy;

import java.util.ArrayList;
import java.util.List;

import splitwise.model.Split;
import splitwise.service.InvalidSplitException;

// Helpers shared by the strategies.
final class Splits {
    private Splits() {}

    // Floor each share, then hand the leftover cents (fewer than n) one each to the first participants.
    // Without this, 100 cents / 3 = 33+33+33 and one cent vanishes from the ledger.
    static List<Split> proportional(long amountCents, List<String> participants, List<Long> weights) {
        long total = sum(weights);
        long[] shares = new long[participants.size()];
        long assigned = 0;
        for (int i = 0; i < shares.length; i++) {
            shares[i] = amountCents * weights.get(i) / total;
            assigned += shares[i];
        }
        for (int i = 0; assigned < amountCents; i++, assigned++) shares[i]++;
        List<Split> splits = new ArrayList<>();
        for (int i = 0; i < shares.length; i++) splits.add(new Split(participants.get(i), shares[i]));
        return splits;
    }

    static void requireValues(List<String> participants, List<Long> values) {
        if (values.size() != participants.size()) {
            throw new InvalidSplitException(values.size() + " values for " + participants.size() + " participants");
        }
        for (Long v : values) {
            if (v == null || v < 0) throw new InvalidSplitException("split values must be >= 0");
        }
    }

    static long sum(List<Long> values) {
        long s = 0;
        for (long v : values) s += v;
        return s;
    }
}
