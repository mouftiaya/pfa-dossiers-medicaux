package ma.clinic.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ma.clinic.notification.dto.AlertDtos.AlertDetailDto;
import ma.clinic.notification.dto.AlertDtos.AlertDto;
import ma.clinic.notification.model.ActionType;
import ma.clinic.notification.model.Alert;
import ma.clinic.notification.model.AlertAction;
import ma.clinic.notification.model.AlertSeverity;
import ma.clinic.notification.model.AlertStatus;
import ma.clinic.notification.model.AlertType;
import ma.clinic.notification.model.Channel;
import ma.clinic.notification.model.DeliveryStatus;
import ma.clinic.notification.model.PatientNotification;
import org.junit.jupiter.api.Test;

class AlertMapperTest {

    private static final String DETAIL = "Glycémie à 312 mg/dL (à jeun) le 10/11/2026 08:30 : seuil critique dépassé.";

    private final Alert alert = new Alert(7L, AlertType.HIGH_GLUCOSE, AlertSeverity.CRITICAL, DETAIL);

    @Test
    void assistantGetsTheDetail() {
        AlertDto dto = AlertMapper.toDto(alert, 2, true);
        assertThat(dto.detail()).isEqualTo(DETAIL);
        assertThat(dto.actionsCount()).isEqualTo(2);
        assertThat(dto.patientId()).isEqualTo(7L);
        assertThat(dto.status()).isEqualTo(AlertStatus.NEW);
    }

    @Test
    void doctorDoesNotGetTheDetail() {
        AlertDto dto = AlertMapper.toDto(alert, 2, false);
        assertThat(dto.detail()).isNull();
        assertThat(dto.type()).isEqualTo(AlertType.HIGH_GLUCOSE);
        assertThat(dto.severity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    void detailDtoHidesDetailForDoctorButKeepsActions() {
        List<AlertAction> actions = List.of(new AlertAction(alert, "assistant1", ActionType.CONTACTED, "Appelé"));
        List<PatientNotification> notifications = List.of(
                new PatientNotification(1L, 7L, Channel.SMS, DeliveryStatus.SENT, "0600000000", "texte"));

        AlertDetailDto doctorView = AlertMapper.toDetailDto(alert, actions, notifications, false);
        assertThat(doctorView.detail()).isNull();
        assertThat(doctorView.actions()).hasSize(1);
        assertThat(doctorView.actions().get(0).actor()).isEqualTo("assistant1");
        assertThat(doctorView.notifications()).hasSize(1);
        assertThat(doctorView.notifications().get(0).channel()).isEqualTo(Channel.SMS);
        assertThat(doctorView.actionsCount()).isEqualTo(1);

        AlertDetailDto assistantView = AlertMapper.toDetailDto(alert, actions, notifications, true);
        assertThat(assistantView.detail()).isEqualTo(DETAIL);
    }

    @Test
    void patientMessageHasNoMedicalInformation() {
        String text = PatientNotificationService.buildMessage("Cabinet Test", "05 11 22 33 44", "Sara");
        assertThat(text).isEqualTo(
                "Bonjour Sara, Cabinet Test : merci de contacter le cabinet au 05 11 22 33 44 pour organiser un suivi.");
        assertThat(text.toLowerCase()).doesNotContain("glyc", "diab", "alerte", "mg/dl");
        assertThat(PatientNotificationService.buildMessage("Cabinet Test", "05", null)).startsWith("Bonjour, ");
    }

    @Test
    void deduplicationRule() {
        assertThat(AlertService.blocksNewAlert(AlertSeverity.WARNING, List.of())).isFalse();
        assertThat(AlertService.blocksNewAlert(AlertSeverity.WARNING, List.of(AlertSeverity.WARNING))).isTrue();
        assertThat(AlertService.blocksNewAlert(AlertSeverity.WARNING, List.of(AlertSeverity.CRITICAL))).isTrue();
        assertThat(AlertService.blocksNewAlert(AlertSeverity.CRITICAL, List.of(AlertSeverity.CRITICAL))).isTrue();
        assertThat(AlertService.blocksNewAlert(AlertSeverity.CRITICAL, List.of(AlertSeverity.WARNING))).isFalse();
    }
}
