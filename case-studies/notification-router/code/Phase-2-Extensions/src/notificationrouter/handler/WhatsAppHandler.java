package notificationrouter.handler;

import notificationrouter.model.Notification;

// Sends through the WhatsApp Business API (paid per message).
public class WhatsAppHandler implements ChannelHandler {
    @Override
    public void send(Notification notification) {
        // stub: real code would look up the user's WhatsApp number and call the provider; a provider error should throw.
        System.out.println("[WHATSAPP] to " + notification.getUserId() + " (" + notification.getId() + "): " + notification.getContent());
    }
}
