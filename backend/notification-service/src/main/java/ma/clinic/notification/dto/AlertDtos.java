package ma.clinic.notification.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.clinic.notification.model.ActionType;
import ma.clinic.notification.model.AlertSeverity;
import ma.clinic.notification.model.AlertStatus;
import ma.clinic.notification.model.AlertType;
import ma.clinic.notification.model.Channel;
import ma.clinic.notification.model.DeliveryStatus;

public final class AlertDtos {

    private AlertDtos() {
    }

    /** detail is null when the caller is a doctor (privacy rule). */
    public record AlertDto(Long id, Long patientId, AlertType type, AlertSeverity severity, AlertStatus status,
                           LocalDateTime createdAt, LocalDateTime updatedAt, String detail, int actionsCount,
                           Integer aiScore) {
    }

    public record ActionDto(Long id, String actor, ActionType type, String note, LocalDateTime createdAt) {
    }

    public record NotificationDto(Channel channel, DeliveryStatus status, LocalDateTime createdAt) {
    }

    public record AlertDetailDto(Long id, Long patientId, AlertType type, AlertSeverity severity, AlertStatus status,
                                 LocalDateTime createdAt, LocalDateTime updatedAt, String detail, int actionsCount,
                                 List<ActionDto> actions, List<NotificationDto> notifications,
                                 Integer aiScore) {
    }

    public record SummaryDto(long total, long open, long newCount, long inProgress, long resolved,
                             Map<String, Long> byType, Map<String, Long> bySeverity) {
    }

    public record ActionRequest(@NotNull ActionType type, @Size(max = 1000) String note) {
    }

    /** Suggested e-mail for the patient (assistants only). to is null when the patient has no e-mail address. */
    public record EmailDraftDto(Long alertId, Long patientId, String to, String phone, String subject, String body) {
    }

    public record SendEmailRequest(@NotBlank @Size(max = 150) String subject,
                                   @NotBlank @Size(max = 1000) String body) {
    }
}
