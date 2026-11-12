package notificationrouter.router;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import notificationrouter.handler.ChannelHandler;
import notificationrouter.model.Channel;
import notificationrouter.model.DeliveryStatus;
import notificationrouter.model.Notification;

// Orchestrator. route() = resolveChannels (decide, pure) + dispatch (send, failures isolated).
public class NotificationRouter {
    private final Map<Channel, ChannelHandler> handlers;          // Req 5
    private final Map<String, Channel> preferences = new HashMap<>(); // Req 1: userId -> preferred channel

    public NotificationRouter(Map<Channel, ChannelHandler> handlers) {
        if (handlers == null) throw new IllegalArgumentException("handlers required");
        for (Channel channel : Channel.values()) {
            if (handlers.get(channel) == null) {
                throw new IllegalArgumentException("no handler for channel " + channel); // fail fast at startup
            }
        }
        this.handlers = new EnumMap<>(handlers); // defensive copy: later edits to the caller's map can't break us
    }

    // Req 1: newest value wins.
    public void setPreference(String userId, Channel channel) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("invalid user");
        if (channel == null) throw new IllegalArgumentException("invalid channel");
        preferences.put(userId, channel);
    }

    // Req 2-5. Throws only before anything is sent; after that, failures come back as FAILED entries.
    public Map<Channel, DeliveryStatus> route(Notification notification) {
        if (notification == null) throw new IllegalArgumentException("invalid notification");
        Set<Channel> channels = resolveChannels(notification);
        return dispatch(notification, channels);
    }

    // Req 3, 4. A Set makes "exactly once each" structural. URGENT is checked first so a
    // missing preference can never block it.
    private Set<Channel> resolveChannels(Notification notification) {
        if (notification.isUrgent()) return EnumSet.allOf(Channel.class);
        Channel preferred = preferences.get(notification.getUserId());
        if (preferred == null) throw new NoPreferredChannelException(notification.getUserId());
        return EnumSet.of(preferred);
    }

    // Req 5: one failing handler never stops the others.
    private Map<Channel, DeliveryStatus> dispatch(Notification notification, Set<Channel> channels) {
        Map<Channel, DeliveryStatus> results = new EnumMap<>(Channel.class);
        for (Channel channel : channels) {
            try {
                handlers.get(channel).send(notification);
                results.put(channel, DeliveryStatus.SENT);
            } catch (RuntimeException e) {
                results.put(channel, DeliveryStatus.FAILED);
                System.err.println("[warn] " + notification.getId() + " on " + channel + " failed: " + e.getMessage());
            }
        }
        return Collections.unmodifiableMap(results);
    }
}
