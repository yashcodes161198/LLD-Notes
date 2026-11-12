package notificationrouter.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import notificationrouter.model.Channel;

// What a policy decided: send to every primary channel; only if ALL of them fail,
// try the fallbacks in order and stop at the first success.
public final class RoutePlan {
    private final Set<Channel> primary;
    private final List<Channel> fallbacks;

    private RoutePlan(Set<Channel> primary, List<Channel> fallbacks) {
        this.primary = primary;
        this.fallbacks = fallbacks;
    }

    public static RoutePlan of(Set<Channel> primary) {
        return withFallbacks(primary, List.of());
    }

    public static RoutePlan withFallbacks(Set<Channel> primary, List<Channel> fallbackOrder) {
        Set<Channel> p = primary.isEmpty() ? EnumSet.noneOf(Channel.class) : EnumSet.copyOf(primary);
        List<Channel> f = new ArrayList<>();
        for (Channel c : fallbackOrder) {
            if (!p.contains(c) && !f.contains(c)) f.add(c); // never try a channel twice
        }
        return new RoutePlan(Collections.unmodifiableSet(p), Collections.unmodifiableList(f));
    }

    public Set<Channel> getPrimary() { return primary; }
    public List<Channel> getFallbacks() { return fallbacks; }
}
