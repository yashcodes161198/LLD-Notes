package notificationrouter.handler;

import notificationrouter.model.Notification;

// Sends a mobile push through APNs / FCM.
public class PushHandler implements ChannelHandler {
    @Override
    public void send(Notification notification) {
        // stub: real code would look up the user's device tokens and call the provider; a provider error should throw.
        System.out.println("[PUSH] to " + notification.getUserId() + " (" + notification.getId() + "): " + notification.getContent());
    }
}
