package ma.clinic.notification.sender;

public interface EmailSender {

    /** Sends an e-mail. Must never throw: returns false when the message could not be sent. */
    boolean send(String to, String subject, String body);
}
