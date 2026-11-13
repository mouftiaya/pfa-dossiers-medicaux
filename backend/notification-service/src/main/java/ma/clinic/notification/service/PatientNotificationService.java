package ma.clinic.notification.service;

import java.time.LocalDateTime;

import ma.clinic.notification.client.PatientContactClient;
import ma.clinic.notification.client.PatientContactClient.Contact;
import ma.clinic.notification.config.NotificationProperties;
import ma.clinic.notification.model.Alert;
import ma.clinic.notification.model.AlertSeverity;
import ma.clinic.notification.model.Channel;
import ma.clinic.notification.model.DeliveryStatus;
import ma.clinic.notification.model.PatientNotification;
import ma.clinic.notification.repository.PatientNotificationRepository;
import ma.clinic.notification.sender.EmailSender;
import ma.clinic.notification.sender.SmsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Sends the message to the patient after an alert was created (SMS and e-mail) and records one
 * {@link PatientNotification} row per channel. The text never contains medical information.
 */
@Service
public class PatientNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PatientNotificationService.class);

    private final PatientContactClient contactClient;
    private final PatientNotificationRepository repository;
    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final NotificationProperties properties;

    public PatientNotificationService(PatientContactClient contactClient, PatientNotificationRepository repository,
                                      EmailSender emailSender, SmsSender smsSender,
                                      NotificationProperties properties) {
        this.contactClient = contactClient;
        this.repository = repository;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
        this.properties = properties;
    }

    /** Notifies the patient of a freshly created alert. Never throws. */
    public void notifyPatient(Alert alert) {
        try {
            Contact contact = contactClient.fetch(alert.getPatientId());
            String firstName = contact == null ? null : contact.firstName();
            String phone = contact == null ? null : blankToNull(contact.phone());
            String email = contact == null ? null : blankToNull(contact.email());
            String text = buildMessage(properties.clinicName(), properties.clinicPhone(), firstName);
            dispatch(alert, Channel.SMS, phone, text);
            dispatch(alert, Channel.EMAIL, email, text);
        } catch (Exception e) {
            log.warn("Patient notification for alert {} failed: {}", alert.getId(), e.getMessage());
        }
    }

    private void dispatch(Alert alert, Channel channel, String recipient, String text) {
        try {
            Long patientId = alert.getPatientId();
            if (recipient == null) {
                repository.save(new PatientNotification(alert.getId(), patientId, channel,
                        DeliveryStatus.SKIPPED_NO_CONTACT, null, null));
                return;
            }
            if (alert.getSeverity() != AlertSeverity.CRITICAL) {
                LocalDateTime since = LocalDateTime.now().minusMinutes(properties.minIntervalMinutes());
                if (repository.existsByPatientIdAndChannelAndStatusAndCreatedAtAfter(
                        patientId, channel, DeliveryStatus.SENT, since)) {
                    repository.save(new PatientNotification(alert.getId(), patientId, channel,
                            DeliveryStatus.THROTTLED, recipient, text));
                    return;
                }
            }
            boolean sent;
            try {
                sent = channel == Channel.SMS
                        ? smsSender.send(recipient, text)
                        : emailSender.send(recipient, properties.clinicName() + " : merci de nous contacter", text);
            } catch (Exception e) {
                log.warn("Sending on channel {} failed: {}", channel, e.getMessage());
                sent = false;
            }
            repository.save(new PatientNotification(alert.getId(), patientId, channel,
                    sent ? DeliveryStatus.SENT : DeliveryStatus.FAILED, recipient, text));
        } catch (Exception e) {
            log.warn("Could not record the {} notification of alert {}: {}", channel, alert.getId(), e.getMessage());
        }
    }

    /** Message for the patient. Deliberately generic: no value, no mention of glucose, diabetes or an alert. */
    public static String buildMessage(String clinicName, String clinicPhone, String firstName) {
        String greeting = firstName == null || firstName.isBlank() ? "Bonjour" : "Bonjour " + firstName.trim();
        return greeting + ", " + clinicName + " : merci de contacter le cabinet au " + clinicPhone
                + " pour organiser un suivi.";
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
