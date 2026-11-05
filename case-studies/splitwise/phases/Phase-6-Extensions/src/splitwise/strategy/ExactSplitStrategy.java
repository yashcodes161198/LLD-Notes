package splitwise.strategy;

import splitwise.model.Split;
import splitwise.model.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

// "Unequal" split: the caller states each person's exact share in metadata.
public class ExactSplitStrategy implements SplitStrategy {
    @Override
    public List<Split> split(double totalAmount, List<User> participants, Map<User, Double> metadata) {
        if (metadata == null || !metadata.keySet().equals(new HashSet<>(participants)))
            throw new IllegalArgumentException("Give an amount for every participant, and only for them");
        double sum = metadata.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(sum - totalAmount) > 0.005)
            throw new IllegalArgumentException("Exact amounts add up to " + sum + ", not " + totalAmount);

        List<Split> splits = new ArrayList<>();
        for (User user : participants) {
            splits.add(new Split(user, metadata.get(user)));
        }
        return splits;
    }
}
