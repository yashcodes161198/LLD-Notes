package notificationrouter.router;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import notificationrouter.handler.ChannelHandler;
import notificationrouter.model.Channel;
import notificationrouter.model.DeliveryStatus;
import notificationrouter.model.Notification;
import notificationrouter.model.Priority;
import notificationrouter.policy.RoutePlan;
import notificationrouter.policy.RoutingPolicy;
import notificationrouter.preference.UserPreference;

// Orchestrator. route() = resolve a plan (policy for the priority) + dispatch (send, failures isolated).
// Adding a channel or a priority never edits this class.
public class NotificationRouter {
    private final Map<Channel, ChannelHandler> handlers;
    private final Map<Priority, RoutingPolicy> policies;
    private final Map<String, UserPreference> preferences = new ConcurrentHashMap<>(); // route + setPreference may race

    public NotificationRouter(Map<Channel, ChannelHandler> handlers, Map<Priority, RoutingPolicy> policies) {
        if (handlers == null || policies == null) throw new IllegalArgumentException("handlers and policies required");
        for (Channel channel : Channel.values()) {
            if (handlers.get(channel) == null) throw new IllegalArgumentException("no handler for channel " + channel);
        }
        for (Priority priority : Priority.values()) {
            if (policies.get(priority) == null) throw new IllegalArgumentException("no policy for priority " + priority);
        }
        this.handlers = new EnumMap<>(handlers);
        this.policies = new EnumMap<>(policies);
    }

    // Base Req 1: one channel for everything (kept so existing callers don't change).
    public void setPreference(String userId, Channel channel) {
        setPreference(userId, UserPreference.of(channel));
    }

    // EXT 2: several channels / per-type channels. Newest value wins.
    public void setPreference(String userId, UserPreference preference) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("invalid user");
        if (preference == null) throw new IllegalArgumentException("invalid preference");
        preferences.put(userId, preference);
    }

    // Throws only before anything is sent; after that, failures come back as FAILED entries.
    // An empty result means the user opted out of this notification type.
    public Map<Channel, DeliveryStatus> route(Notification notification) {
        if (notification == null) throw new IllegalArgumentException("invalid notification");
        RoutePlan plan = resolvePlan(notification);
        return dispatch(notification, plan);
    }

    private RoutePlan resolvePlan(Notification notification) {
        UserPreference preference = preferences.get(notification.getUserId()); // read once
        return policies.get(notification.getPriority()).plan(notification, preference);
    }

    private Map<Channel, DeliveryStatus> dispatch(Notification notification, RoutePlan plan) {
        Map<Channel, DeliveryStatus> results = new LinkedHashMap<>(); // keeps attempt order, shows the fallback path
        for (Channel channel : plan.getPrimary()) {
            results.put(channel, send(channel, notification));
        }
        if (!results.containsValue(DeliveryStatus.SENT)) {
            for (Channel channel : plan.getFallbacks()) {
                DeliveryStatus status = send(channel, notification);
                results.put(channel, status);
                if (status == DeliveryStatus.SENT) break; // stop at the first success
            }
        }
        return Collections.unmodifiableMap(results);
    }

    private DeliveryStatus send(Channel channel, Notification notification) {
        try {
            handlers.get(channel).send(notification);
            return DeliveryStatus.SENT;
        } catch (RuntimeException e) {
            System.err.println("[warn] " + notification.getId() + " on " + channel + " failed: " + e.getMessage());
            return DeliveryStatus.FAILED;
        }
    }
}
