package llmusagelimits.estimate;

import llmusagelimits.model.Model;

// Phase 1 behaviour: always the same typical reply size.
public class FixedEstimator implements UsageEstimator {
    private final long replyTokens;

    public FixedEstimator(long replyTokens) {
        if (replyTokens < 0) throw new IllegalArgumentException("invalid reply size");
        this.replyTokens = replyTokens;
    }

    @Override public long expectedReplyTokens(String userId, Model model) { return replyTokens; }

    @Override public void record(String userId, Model model, long actualOutputTokens) { }
}
