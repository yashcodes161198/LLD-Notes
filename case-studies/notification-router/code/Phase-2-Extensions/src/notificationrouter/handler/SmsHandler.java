package notificationrouter.handler;

import notificationrouter.model.Notification;

// Sends through an SMS gateway (e.g. Twilio).
public class SmsHandler implements ChannelHandler {
    @Override
    public void send(Notification notification) {
        // stub: real code would look up the user's phone number and call the provider; a provider error should throw.
        System.out.println("[SMS] to " + notification.getUserId() + " (" + notification.getId() + "): " + notification.getContent());
    }
}
