package notificationrouter.policy;

import java.util.EnumSet;
import java.util.Set;

import notificationrouter.model.Channel;
import notificationrouter.model.Notification;
import notificationrouter.preference.UserPreference;

// EXT 4: HIGH = preferred channel(s) plus PUSH. Respects opt-out.
public class PreferredPlusPushPolicy implements RoutingPolicy {
    @Override
    public RoutePlan plan(Notification notification, UserPreference preference) {
        Set<Channel> preferred = RoutingPolicy.preferredChannels(notification, preference);
        if (preferred.isEmpty()) return RoutePlan.of(preferred);
        Set<Channel> channels = EnumSet.copyOf(preferred);
        channels.add(Channel.PUSH);
        return RoutePlan.of(channels);
    }
}
