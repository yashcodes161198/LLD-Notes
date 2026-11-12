package notificationrouter.policy;

import java.util.EnumSet;
import java.util.Set;

import notificationrouter.model.Channel;
import notificationrouter.model.Notification;
import notificationrouter.preference.UserPreference;

// URGENT: every channel (or a configured subset, e.g. without paid WHATSAPP). Ignores
// preferences and opt-outs, so callers allowed to send URGENT must be restricted upstream.
public class AllChannelsPolicy implements RoutingPolicy {
    private final Set<Channel> channels;

    public AllChannelsPolicy() { this(EnumSet.allOf(Channel.class)); }

    public AllChannelsPolicy(Set<Channel> channels) {
        if (channels == null || channels.isEmpty()) throw new IllegalArgumentException("channels required");
        this.channels = EnumSet.copyOf(channels);
    }

    @Override
    public RoutePlan plan(Notification notification, UserPreference preference) {
        return RoutePlan.of(channels);
    }
}
