package ma.clinic.notification.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Default e-mail sender: only logs the message (works without any mail provider). */
@Component
@ConditionalOnProperty(prefix = "notifications.email", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public boolean send(String to, String subject, String body) {
        log.info("[EMAIL simulated] to={} subject={} body={}", to, subject, body);
        return true;
    }
}
