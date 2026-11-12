package notificationrouter.handler;

import notificationrouter.model.Notification;

// Sends over SMTP or an email API (e.g. SES, SendGrid).
public class EmailHandler implements ChannelHandler {
    @Override
    public void send(Notification notification) {
        // stub: real code would look up the user's email address and call the provider; a provider error should throw.
        System.out.println("[EMAIL] to " + notification.getUserId() + " (" + notification.getId() + "): " + notification.getContent());
    }
}
