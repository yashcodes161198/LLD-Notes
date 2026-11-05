package splitwise.strategy;

import splitwise.model.Split;
import splitwise.model.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class PercentageSplitStrategy implements SplitStrategy {
    @Override
    public List<Split> split(double totalAmount, List<User> participants, Map<User, Double> metadata) {
        if (metadata == null || !metadata.keySet().equals(new HashSet<>(participants)))
            throw new IllegalArgumentException("Give a percentage for every participant, and only for them");
        double totalPercent = metadata.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(totalPercent - 100.0) > 1e-6) throw new IllegalArgumentException("Total percent should be 100");

        List<Split> splits = new ArrayList<>();
        for (User user : participants) {
            splits.add(new Split(user, totalAmount * metadata.get(user) / 100));
        }
        return splits;
    }
}
