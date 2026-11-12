package llmusagelimits.estimate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import llmusagelimits.model.Model;

// EXT 2: per (user, model) moving average of reply size, seeded with a typical value.
public class MovingAverageEstimator implements UsageEstimator {
    private static final double ALPHA = 0.2;

    private final long initialTokens;
    private final Map<String, Double> averages = new ConcurrentHashMap<>();

    public MovingAverageEstimator(long initialTokens) {
        if (initialTokens < 0) throw new IllegalArgumentException("invalid reply size");
        this.initialTokens = initialTokens;
    }

    @Override
    public long expectedReplyTokens(String userId, Model model) {
        return Math.round(averages.getOrDefault(key(userId, model), (double) initialTokens));
    }

    @Override
    public void record(String userId, Model model, long actualOutputTokens) {
        averages.merge(key(userId, model), (1 - ALPHA) * initialTokens + ALPHA * actualOutputTokens,
                (old, ignored) -> (1 - ALPHA) * old + ALPHA * actualOutputTokens);
    }

    private static String key(String userId, Model model) { return userId + "|" + model; }
}
