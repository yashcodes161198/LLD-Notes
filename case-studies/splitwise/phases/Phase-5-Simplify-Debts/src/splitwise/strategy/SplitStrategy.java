package splitwise.strategy;

import splitwise.model.Split;
import splitwise.model.User;

import java.util.List;
import java.util.Map;

public interface SplitStrategy {
    // metadata carries per-user inputs a strategy may need (e.g. percentages); EQUAL ignores it.
    List<Split> split(double totalAmount, List<User> participants, Map<User, Double> metadata);
}
