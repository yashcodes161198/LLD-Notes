package splitwise.strategy;

import splitwise.model.Split;
import splitwise.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EqualSplitStrategy implements SplitStrategy {
    @Override
    public List<Split> split(double totalAmount, List<User> participants, Map<User, Double> metadata) {
        // Work in whole cents so the shares always add back up to the total:
        // 100.00 / 3 -> 33.34, 33.33, 33.33 (the first `remainder` people pay one extra cent).
        long totalCents = Math.round(totalAmount * 100);
        long base = totalCents / participants.size();
        long remainder = totalCents % participants.size();

        List<Split> splits = new ArrayList<>();
        for (int i = 0; i < participants.size(); i++) {
            long cents = base + (i < remainder ? 1 : 0);
            splits.add(new Split(participants.get(i), cents / 100.0));
        }
        return splits;
    }
}
