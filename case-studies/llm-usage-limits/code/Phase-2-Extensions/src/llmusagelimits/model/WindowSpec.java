package llmusagelimits.model;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

// Config for one window (EXT 3): which limit, how long, how big, and which models count against it.
public final class WindowSpec {
    private final LimitType type;
    private final Duration duration;
    private final long baseLimit;
    private final Set<Model> models;

    public WindowSpec(LimitType type, Duration duration, long baseLimit, Set<Model> models) {
        if (type == null || duration == null || duration.isNegative() || duration.isZero() || baseLimit <= 0
                || models == null || models.isEmpty()) {
            throw new IllegalArgumentException("invalid window spec");
        }
        this.type = type;
        this.duration = duration;
        this.baseLimit = baseLimit;
        this.models = Set.copyOf(EnumSet.copyOf(models));
    }

    public static WindowSpec allModels(LimitType type, Duration duration, long baseLimit) {
        return new WindowSpec(type, duration, baseLimit, EnumSet.allOf(Model.class));
    }

    public LimitType getType() { return type; }
    public Duration getDuration() { return duration; }
    public long getBaseLimit() { return baseLimit; }
    public Set<Model> getModels() { return models; }
}
