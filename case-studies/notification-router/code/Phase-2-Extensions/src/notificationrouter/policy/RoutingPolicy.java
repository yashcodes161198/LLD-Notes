package notificationrouter.policy;

import java.util.Set;

import notificationrouter.model.Channel;
import notificationrouter.model.Notification;
import notificationrouter.preference.UserPreference;
import notificationrouter.router.NoPreferredChannelException;

// EXT 4 (Strategy): one policy per priority. Pure: same input, same plan, no side effects.
public interface RoutingPolicy {
    // preference is null when the user has none on file.
    RoutePlan plan(Notification notification, UserPreference preference);

    // Shared rule: no preference on file is an error; an empty set (opt-out) is not.
    static Set<Channel> preferredChannels(Notification notification, UserPreference preference) {
        if (preference == null) throw new NoPreferredChannelException(notification.getUserId());
        return preference.channelsFor(notification.getType());
    }
}
