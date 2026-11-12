package notificationrouter.handler;

import notificationrouter.model.Notification;

// The seam to the real senders (SMTP, SMS gateway, APNs/FCM). Implementations are out of scope.
// Throwing any RuntimeException means "this channel failed".
public interface ChannelHandler {
    void send(Notification notification);
}
