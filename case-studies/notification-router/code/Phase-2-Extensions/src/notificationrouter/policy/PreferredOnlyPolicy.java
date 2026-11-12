package notificationrouter.policy;

import notificationrouter.model.Notification;
import notificationrouter.preference.UserPreference;

// Base NORMAL rule: the user's preferred channel(s) for this type, nothing else.
public class PreferredOnlyPolicy implements RoutingPolicy {
    @Override
    public RoutePlan plan(Notification notification, UserPreference preference) {
        return RoutePlan.of(RoutingPolicy.preferredChannels(notification, preference));
    }
}
