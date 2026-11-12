package notificationrouter.preference;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import notificationrouter.model.Channel;
import notificationrouter.model.NotificationType;

// EXT 2: the old single-Channel map value, promoted to a class now that it has rules.
// Immutable: setPreference swaps the whole object, so a route() never sees half an update.
public final class UserPreference {
    private final Set<Channel> defaultChannels;
    private final Map<NotificationType, Set<Channel>> channelsByType;

    public UserPreference(Set<Channel> defaultChannels, Map<NotificationType, Set<Channel>> channelsByType) {
        if (defaultChannels == null || defaultChannels.isEmpty()) {
            throw new IllegalArgumentException("at least one default channel required");
        }
        if (channelsByType == null) throw new IllegalArgumentException("channelsByType required (may be empty)");
        this.defaultChannels = Collections.unmodifiableSet(EnumSet.copyOf(defaultChannels));
        Map<NotificationType, Set<Channel>> copy = new EnumMap<>(NotificationType.class);
        channelsByType.forEach((type, channels) -> copy.put(type, Collections.unmodifiableSet(
                channels.isEmpty() ? EnumSet.noneOf(Channel.class) : EnumSet.copyOf(channels))));
        this.channelsByType = Collections.unmodifiableMap(copy);
    }

    // The base requirement: one preferred channel for everything.
    public static UserPreference of(Channel channel) {
        if (channel == null) throw new IllegalArgumentException("invalid channel");
        return new UserPreference(EnumSet.of(channel), Map.of());
    }

    // An empty set means the user opted out of that type.
    public Set<Channel> channelsFor(NotificationType type) {
        return channelsByType.getOrDefault(type, defaultChannels);
    }
}
