package ma.clinic.notification.sender;

public interface SmsSender {

    /** Sends an SMS. Must never throw: returns false when the message could not be sent. */
    boolean send(String phoneNumber, String text);
}
