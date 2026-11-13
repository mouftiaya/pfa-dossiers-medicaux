package ma.clinic.notification.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Real e-mail sender, active only when notifications.email.enabled=true (configure spring.mail.*). */
@Component
@ConditionalOnProperty(prefix = "notifications.email", name = "enabled", havingValue = "true")
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailSender(JavaMailSender mailSender,
                           @Value("${notifications.email.from:no-reply@clinic.local}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public boolean send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.warn("E-mail could not be sent: {}", e.getMessage());
            return false;
        }
    }
}
