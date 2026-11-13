package ma.clinic.notification.detection;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ma.clinic.notification.dto.EventDtos.AbsenceEvent;
import ma.clinic.notification.dto.EventDtos.GlucoseEvent;
import ma.clinic.notification.dto.EventDtos.LabCriticalEvent;
import ma.clinic.notification.dto.EventDtos.Point;
import ma.clinic.notification.dto.EventDtos.Thresholds;
import ma.clinic.notification.model.AlertSeverity;
import ma.clinic.notification.model.AlertType;

/**
 * Pure detection logic (no Spring, no database) so that it can be unit tested.
 * Glucose values are in mg/dL. The generated texts are in French.
 */
public class AlertDetector {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Used when an event carries no thresholds (same defaults as glucose-monitoring-service). */
    private static final Thresholds DEFAULT_THRESHOLDS = new Thresholds(54.0, 70.0, 70.0, 180.0, 250.0, 300.0);

    private final AlertRules rules;

    public AlertDetector(AlertRules rules) {
        this.rules = rules;
    }

    public List<DetectedAlert> detectGlucose(GlucoseEvent event) {
        List<DetectedAlert> alerts = new ArrayList<>();
        if (event == null || event.value() == null || event.measuredAt() == null) {
            return alerts;
        }
        Thresholds t = event.thresholds() != null ? event.thresholds() : DEFAULT_THRESHOLDS;
        double value = event.value();
        String measure = "Glycémie à " + format(value) + " mg/dL" + contextLabel(event.context())
                + " le " + event.measuredAt().format(DATE_FORMAT);

        if (t.criticalLow() != null && value <= t.criticalLow()) {
            alerts.add(new DetectedAlert(AlertType.LOW_GLUCOSE, AlertSeverity.CRITICAL,
                    measure + " : seuil critique bas atteint."));
        } else if (t.lowThreshold() != null && value < t.lowThreshold()) {
            alerts.add(new DetectedAlert(AlertType.LOW_GLUCOSE, AlertSeverity.WARNING,
                    measure + " : sous le seuil bas."));
        }

        if (t.criticalHigh() != null && value >= t.criticalHigh()) {
            alerts.add(new DetectedAlert(AlertType.HIGH_GLUCOSE, AlertSeverity.CRITICAL,
                    measure + " : seuil critique dépassé."));
        } else if (t.highThreshold() != null && value > t.highThreshold()) {
            alerts.add(new DetectedAlert(AlertType.HIGH_GLUCOSE, AlertSeverity.WARNING,
                    measure + " : au-dessus du seuil haut."));
        }

        Point previous = event.previous();
        if (previous != null && previous.value() != null && previous.measuredAt() != null) {
            Duration gap = Duration.between(previous.measuredAt(), event.measuredAt());
            double delta = Math.abs(value - previous.value());
            if (!gap.isNegative() && gap.compareTo(Duration.ofHours(rules.variationMaxHours())) <= 0
                    && delta >= rules.variationDelta()) {
                alerts.add(new DetectedAlert(AlertType.UNUSUAL_VARIATION, AlertSeverity.WARNING,
                        "Variation inhabituelle de la glycémie : " + format(previous.value()) + " mg/dL à "
                                + previous.measuredAt().format(DATE_FORMAT) + " puis " + format(value)
                                + " mg/dL à " + event.measuredAt().format(DATE_FORMAT) + "."));
            }
        }

        List<Point> recent = event.recent();
        if (recent != null && recent.size() >= rules.persistentCount()) {
            if (t.targetMax() != null && allMatch(recent, v -> v > t.targetMax())) {
                alerts.add(new DetectedAlert(AlertType.PERSISTENT_HIGH, AlertSeverity.WARNING,
                        "Glycémie au-dessus de la cible sur les " + recent.size()
                                + " dernières mesures (jusqu'au " + event.measuredAt().format(DATE_FORMAT) + ")."));
            }
            if (t.targetMin() != null && allMatch(recent, v -> v < t.targetMin())) {
                alerts.add(new DetectedAlert(AlertType.PERSISTENT_LOW, AlertSeverity.WARNING,
                        "Glycémie en dessous de la cible sur les " + recent.size()
                                + " dernières mesures (jusqu'au " + event.measuredAt().format(DATE_FORMAT) + ")."));
            }
        }
        return alerts;
    }

    public List<DetectedAlert> detectAbsence(AbsenceEvent event) {
        List<DetectedAlert> alerts = new ArrayList<>();
        if (event == null || event.daysWithoutMeasurement() == null || event.absenceDays() == null) {
            return alerts;
        }
        if (event.daysWithoutMeasurement() >= event.absenceDays()) {
            String last = event.lastMeasurementAt() == null
                    ? "aucune mesure enregistrée"
                    : "dernière mesure le " + event.lastMeasurementAt().format(DATE_FORMAT);
            alerts.add(new DetectedAlert(AlertType.NO_MEASUREMENT, AlertSeverity.WARNING,
                    "Aucune mesure de glycémie depuis " + event.daysWithoutMeasurement() + " jours ("
                            + last + ")."));
        }
        return alerts;
    }

    public List<DetectedAlert> detectLabCritical(LabCriticalEvent event) {
        List<DetectedAlert> alerts = new ArrayList<>();
        if (event == null) {
            return alerts;
        }
        String direction = "CRITICAL_LOW".equals(event.flag()) ? "valeur critique basse" : "valeur critique haute";
        String label = event.parameterLabel() == null ? "paramètre" : event.parameterLabel();
        String valueText = event.value() == null ? "" : " = " + format(event.value())
                + (event.unit() == null || event.unit().isBlank() ? "" : " " + event.unit());
        alerts.add(new DetectedAlert(AlertType.CRITICAL_LAB_RESULT, AlertSeverity.CRITICAL,
                "Résultat de laboratoire critique : " + label + valueText + " (" + direction + ")."));
        return alerts;
    }

    private static boolean allMatch(List<Point> points, java.util.function.DoublePredicate predicate) {
        for (Point p : points) {
            if (p == null || p.value() == null || !predicate.test(p.value())) {
                return false;
            }
        }
        return true;
    }

    private static String contextLabel(String context) {
        if (context == null) {
            return "";
        }
        return switch (context) {
            case "FASTING" -> " (à jeun)";
            case "BEFORE_MEAL" -> " (avant repas)";
            case "AFTER_MEAL" -> " (après repas)";
            case "BEDTIME" -> " (au coucher)";
            default -> "";
        };
    }

    static String format(double v) {
        if (v == Math.rint(v)) {
            return String.valueOf((long) v);
        }
        return String.format(Locale.ROOT, "%.1f", v);
    }
}
