package ma.clinic.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Settings of the messages sent to patients ("notifications.*"). notifications.email.* is read by the e-mail senders. */
@ConfigurationProperties(prefix = "notifications")
public record NotificationProperties(@DefaultValue("Cabinet de diabétologie") String clinicName,
                                     @DefaultValue("05 00 00 00 00") String clinicPhone,
                                     @DefaultValue("360") long minIntervalMinutes) {
}
