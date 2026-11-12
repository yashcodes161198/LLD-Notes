package notificationrouter.policy;

import java.util.List;
import java.util.Set;

import notificationrouter.model.Channel;
import notificationrouter.model.Notification;
import notificationrouter.preference.UserPreference;

// EXT 3: preferred channel(s) first; if all fail, walk an explicit fallback order.
// Product decision: opt it in per priority, since users picked their channel for a reason.
public class PreferredWithFallbackPolicy implements RoutingPolicy {
    private final List<Channel> fallbackOrder;

    public PreferredWithFallbackPolicy(List<Channel> fallbackOrder) {
        if (fallbackOrder == null || fallbackOrder.isEmpty() || fallbackOrder.stream().anyMatch(c -> c == null)) {
            throw new IllegalArgumentException("fallback order required");
        }
        this.fallbackOrder = List.copyOf(fallbackOrder);
    }

    @Override
    public RoutePlan plan(Notification notification, UserPreference preference) {
        Set<Channel> preferred = RoutingPolicy.preferredChannels(notification, preference);
        if (preferred.isEmpty()) return RoutePlan.of(preferred); // opted out: never fall back into a channel
        return RoutePlan.withFallbacks(preferred, fallbackOrder);
    }
}
