package splitwise.strategy;

import java.util.List;

import splitwise.model.Split;

// EXT 1: one implementation per SplitType. Participants are already validated (non-empty, unique, in the group).
// Must return shares that sum to exactly amountCents, or throw InvalidSplitException.
public interface SplitStrategy {
    List<Split> split(long amountCents, List<String> participants, List<Long> values);
}
