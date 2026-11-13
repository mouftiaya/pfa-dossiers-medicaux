package ma.clinic.notification.service;

import java.util.List;

import ma.clinic.notification.dto.AlertDtos.ActionDto;
import ma.clinic.notification.dto.AlertDtos.AlertDetailDto;
import ma.clinic.notification.dto.AlertDtos.AlertDto;
import ma.clinic.notification.dto.AlertDtos.NotificationDto;
import ma.clinic.notification.model.Alert;
import ma.clinic.notification.model.AlertAction;
import ma.clinic.notification.model.PatientNotification;

/**
 * Entity -> DTO mapping. Pure functions, unit tested.
 * Privacy rule: the {@code detail} text (which can contain the measured value) is only given when
 * {@code includeDetail} is true, i.e. never to a doctor.
 */
public final class AlertMapper {

    private AlertMapper() {
    }

    public static AlertDto toDto(Alert alert, int actionsCount, boolean includeDetail) {
        return new AlertDto(alert.getId(), alert.getPatientId(), alert.getType(), alert.getSeverity(),
                alert.getStatus(), alert.getCreatedAt(), alert.getUpdatedAt(),
                includeDetail ? alert.getDetail() : null, actionsCount, alert.getAiScore());
    }

    public static AlertDetailDto toDetailDto(Alert alert, List<AlertAction> actions,
                                             List<PatientNotification> notifications, boolean includeDetail) {
        List<ActionDto> actionDtos = actions.stream()
                .map(a -> new ActionDto(a.getId(), a.getActor(), a.getType(), a.getNote(), a.getCreatedAt()))
                .toList();
        List<NotificationDto> notificationDtos = notifications.stream()
                .map(n -> new NotificationDto(n.getChannel(), n.getStatus(), n.getCreatedAt()))
                .toList();
        return new AlertDetailDto(alert.getId(), alert.getPatientId(), alert.getType(), alert.getSeverity(),
                alert.getStatus(), alert.getCreatedAt(), alert.getUpdatedAt(),
                includeDetail ? alert.getDetail() : null, actions.size(), actionDtos, notificationDtos,
                alert.getAiScore());
    }
}
