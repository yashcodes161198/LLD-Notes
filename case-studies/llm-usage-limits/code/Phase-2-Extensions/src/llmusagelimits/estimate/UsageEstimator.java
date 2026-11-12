package llmusagelimits.estimate;

import llmusagelimits.model.Model;

// EXT 2 (Strategy): how many reply tokens to reserve. Quality only changes the overshoot; settle keeps numbers right.
public interface UsageEstimator {
    long expectedReplyTokens(String userId, Model model);

    void record(String userId, Model model, long actualOutputTokens);
}
