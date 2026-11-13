package ma.clinic.notification.service;

import ma.clinic.notification.client.PatientContactClient;
import ma.clinic.notification.client.PatientContactClient.Contact;
import ma.clinic.notification.config.NotificationProperties;
import ma.clinic.notification.dto.AlertDtos.ActionRequest;
import ma.clinic.notification.dto.AlertDtos.AlertDetailDto;
import ma.clinic.notification.dto.AlertDtos.EmailDraftDto;
import ma.clinic.notification.dto.AlertDtos.SendEmailRequest;
import ma.clinic.notification.model.Alert;
import ma.clinic.notification.model.ActionType;
import ma.clinic.notification.model.AlertStatus;
import ma.clinic.notification.model.Channel;
import ma.clinic.notification.model.DeliveryStatus;
import ma.clinic.notification.model.PatientNotification;
import ma.clinic.notification.repository.AlertRepository;
import ma.clinic.notification.repository.PatientNotificationRepository;
import ma.clinic.notification.sender.EmailSender;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * The assistant's side of an alert: a suggested e-mail she can edit, and the sending of it. Not transactional on
 * purpose (remote calls); the action is recorded through {@link AlertService#addAction}.
 */
@Service
public class OutreachService {

    private final AlertRepository alerts;
    private final AlertService alertService;
    private final PatientContactClient contactClient;
    private final PatientNotificationRepository notifications;
    private final EmailSender emailSender;
    private final NotificationProperties properties;

    public OutreachService(AlertRepository alerts, AlertService alertService, PatientContactClient contactClient,
                           PatientNotificationRepository notifications, EmailSender emailSender,
                           NotificationProperties properties) {
        this.alerts = alerts;
        this.alertService = alertService;
        this.contactClient = contactClient;
        this.notifications = notifications;
        this.emailSender = emailSender;
        this.properties = properties;
    }

    public EmailDraftDto draft(Long alertId) {
        Alert alert = find(alertId);
        Contact contact = contactClient.fetch(alert.getPatientId());
        String firstName = contact == null ? null : contact.firstName();
        return new EmailDraftDto(alert.getId(), alert.getPatientId(), blankToNull(contact == null ? null : contact.email()),
                blankToNull(contact == null ? null : contact.phone()),
                EmailDraftBuilder.subject(properties.clinicName()),
                EmailDraftBuilder.body(properties.clinicName(), properties.clinicPhone(), firstName));
    }

    public AlertDetailDto sendEmail(Long alertId, SendEmailRequest request, String actor, boolean includeDetail) {
        Alert alert = find(alertId);
        if (alert.getStatus() == AlertStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This alert is already resolved");
        }
        Contact contact = contactClient.fetch(alert.getPatientId());
        String to = blankToNull(contact == null ? null : contact.email());
        if (to == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce patient n'a pas d'adresse e-mail : contactez-le par téléphone.");
        }
        boolean sent;
        try {
            sent = emailSender.send(to, request.subject().trim(), request.body().trim());
        } catch (RuntimeException e) {
            sent = false;
        }
        notifications.save(new PatientNotification(alert.getId(), alert.getPatientId(), Channel.EMAIL,
                sent ? DeliveryStatus.SENT : DeliveryStatus.FAILED, to, request.body().trim()));
        if (!sent) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "L'e-mail n'a pas pu être envoyé.");
        }
        return alertService.addAction(alertId,
                new ActionRequest(ActionType.CONTACTED, "E-mail envoyé au patient"), actor, includeDetail);
    }

    private Alert find(Long id) {
        return alerts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
