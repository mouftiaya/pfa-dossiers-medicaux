package ma.clinic.notification.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default SMS sender: only logs the message.
 * To use a real SMS provider (Twilio, Infobip, a local operator gateway...), create another class implementing
 * {@link SmsSender} that calls the provider API, catches every exception and returns false on failure, then
 * replace this bean (remove @Component here or mark the new one @Primary).
 */
@Component
public class LoggingSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public boolean send(String phoneNumber, String text) {
        log.info("[SMS simulated] to={} text={}", phoneNumber, text);
        return true;
    }
}
