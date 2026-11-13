package ma.clinic.notification.detection;

import ma.clinic.notification.model.AlertSeverity;
import ma.clinic.notification.model.AlertType;

/** An alert found by the detector, not yet persisted. */
public record DetectedAlert(AlertType type, AlertSeverity severity, String detail) {
}
