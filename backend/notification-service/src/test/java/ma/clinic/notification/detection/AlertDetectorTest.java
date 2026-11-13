package ma.clinic.notification.detection;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import ma.clinic.notification.dto.EventDtos.AbsenceEvent;
import ma.clinic.notification.dto.EventDtos.GlucoseEvent;
import ma.clinic.notification.dto.EventDtos.LabCriticalEvent;
import ma.clinic.notification.dto.EventDtos.Point;
import ma.clinic.notification.dto.EventDtos.Thresholds;
import ma.clinic.notification.model.AlertSeverity;
import ma.clinic.notification.model.AlertType;
import org.junit.jupiter.api.Test;

class AlertDetectorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 8, 30);
    private static final Thresholds T = new Thresholds(54.0, 70.0, 70.0, 180.0, 250.0, 300.0);

    private final AlertDetector detector = new AlertDetector(AlertRules.defaults());

    private static GlucoseEvent event(double value, Point previous, List<Point> recent) {
        return new GlucoseEvent(1L, value, NOW, "FASTING", T, previous, recent);
    }

    private static GlucoseEvent event(double value) {
        return event(value, null, null);
    }

    private static Point point(double value, LocalDateTime at) {
        return new Point(value, at);
    }

    private static List<Point> recent(double... values) {
        return java.util.Arrays.stream(values).mapToObj(v -> point(v, NOW.minusHours(1))).toList();
    }

    private List<DetectedAlert> detect(GlucoseEvent e) {
        return detector.detectGlucose(e);
    }

    // ---- low glucose

    @Test
    void criticalLowAtThreshold() {
        List<DetectedAlert> r = detect(event(54));
        assertThat(r).hasSize(1);
        assertThat(r.get(0).type()).isEqualTo(AlertType.LOW_GLUCOSE);
        assertThat(r.get(0).severity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    void warningLowJustAboveCriticalLow() {
        List<DetectedAlert> r = detect(event(54.1));
        assertThat(r).hasSize(1);
        assertThat(r.get(0).type()).isEqualTo(AlertType.LOW_GLUCOSE);
        assertThat(r.get(0).severity()).isEqualTo(AlertSeverity.WARNING);
    }

    @Test
    void warningLowJustBelowLowThreshold() {
        List<DetectedAlert> r = detect(event(69.9));
        assertThat(r).hasSize(1);
        assertThat(r.get(0).severity()).isEqualTo(AlertSeverity.WARNING);
    }

    @Test
    void noLowAlertAtLowThreshold() {
        assertThat(detect(event(70))).isEmpty();
    }

    // ---- high glucose

    @Test
    void noHighAlertAtHighThreshold() {
        assertThat(detect(event(250))).isEmpty();
    }

    @Test
    void warningHighJustAboveHighThreshold() {
        List<DetectedAlert> r = detect(event(250.1));
        assertThat(r).hasSize(1);
        assertThat(r.get(0).type()).isEqualTo(AlertType.HIGH_GLUCOSE);
        assertThat(r.get(0).severity()).isEqualTo(AlertSeverity.WARNING);
    }

    @Test
    void warningHighJustBelowCriticalHigh() {
        List<DetectedAlert> r = detect(event(299.9));
        assertThat(r).hasSize(1);
        assertThat(r.get(0).severity()).isEqualTo(AlertSeverity.WARNING);
    }

    @Test
    void criticalHighAtThreshold() {
        List<DetectedAlert> r = detect(event(300));
        assertThat(r).hasSize(1);
        assertThat(r.get(0).type()).isEqualTo(AlertType.HIGH_GLUCOSE);
        assertThat(r.get(0).severity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    void inTargetValueGivesNothing() {
        assertThat(detect(event(120))).isEmpty();
    }

    @Test
    void detailTextIsFrenchAndContainsValueAndDate() {
        DetectedAlert a = detect(event(312, null, null)).get(0);
        assertThat(a.detail()).isEqualTo(
                "Glycémie à 312 mg/dL (à jeun) le 10/11/2026 08:30 : seuil critique dépassé.");
    }

    @Test
    void defaultThresholdsUsedWhenEventHasNone() {
        GlucoseEvent e = new GlucoseEvent(1L, 320.0, NOW, null, null, null, null);
        List<DetectedAlert> r = detector.detectGlucose(e);
        assertThat(r).hasSize(1);
        assertThat(r.get(0).severity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    void incompleteEventGivesNothing() {
        assertThat(detector.detectGlucose(null)).isEmpty();
        assertThat(detector.detectGlucose(new GlucoseEvent(1L, null, NOW, null, T, null, null))).isEmpty();
        assertThat(detector.detectGlucose(new GlucoseEvent(1L, 300.0, null, null, T, null, null))).isEmpty();
    }

    // ---- unusual variation

    @Test
    void variationAtExactlyHundredWithinSixHours() {
        GlucoseEvent e = event(170, point(70, NOW.minusHours(6)), null);
        List<DetectedAlert> r = detect(e);
        assertThat(r).extracting(DetectedAlert::type).containsExactly(AlertType.UNUSUAL_VARIATION);
        assertThat(r.get(0).severity()).isEqualTo(AlertSeverity.WARNING);
    }

    @Test
    void noVariationBelowHundred() {
        GlucoseEvent e = event(169.9, point(70, NOW.minusHours(1)), null);
        assertThat(detect(e)).isEmpty();
    }

    @Test
    void noVariationWhenGapExceedsSixHours() {
        GlucoseEvent e = event(170, point(70, NOW.minusHours(6).minusMinutes(1)), null);
        assertThat(detect(e)).isEmpty();
    }

    @Test
    void variationAlsoOnDrop() {
        GlucoseEvent e = event(100, point(200, NOW.minusHours(2)), null);
        assertThat(detect(e)).extracting(DetectedAlert::type).containsExactly(AlertType.UNUSUAL_VARIATION);
    }

    @Test
    void previousInTheFutureIsIgnored() {
        GlucoseEvent e = event(100, point(300, NOW.plusHours(1)), null);
        assertThat(detect(e)).isEmpty();
    }

    // ---- persistent

    @Test
    void persistentHighWithFiveValuesAboveTargetMax() {
        GlucoseEvent e = event(190, null, recent(185, 190, 200, 181, 190));
        List<DetectedAlert> r = detect(e);
        assertThat(r).extracting(DetectedAlert::type).containsExactly(AlertType.PERSISTENT_HIGH);
        assertThat(r.get(0).severity()).isEqualTo(AlertSeverity.WARNING);
    }

    @Test
    void noPersistentHighWhenOneValueEqualsTargetMax() {
        GlucoseEvent e = event(190, null, recent(185, 190, 180, 181, 190));
        assertThat(detect(e)).isEmpty();
    }

    @Test
    void noPersistentHighWithFourValues() {
        GlucoseEvent e = event(190, null, recent(185, 190, 200, 190));
        assertThat(detect(e)).isEmpty();
    }

    @Test
    void persistentLowWithFiveValuesBelowTargetMin() {
        GlucoseEvent e = event(65, null, recent(60, 65, 69, 66, 65));
        List<DetectedAlert> r = detect(e);
        // 65 is also below the low threshold: LOW_GLUCOSE warning + PERSISTENT_LOW
        assertThat(r).extracting(DetectedAlert::type)
                .containsExactlyInAnyOrder(AlertType.LOW_GLUCOSE, AlertType.PERSISTENT_LOW);
    }

    @Test
    void noPersistentLowWhenOneValueEqualsTargetMin() {
        GlucoseEvent e = event(120, null, recent(60, 65, 70, 66, 65));
        assertThat(detect(e)).isEmpty();
    }

    @Test
    void persistentHighUsesMoreThanFiveValues() {
        GlucoseEvent e = event(190, null, recent(190, 190, 190, 190, 190, 190));
        assertThat(detect(e)).extracting(DetectedAlert::type).containsExactly(AlertType.PERSISTENT_HIGH);
    }

    @Test
    void nullValueInRecentBreaksPersistence() {
        List<Point> recent = List.of(point(190, NOW), point(190, NOW), new Point(null, NOW), point(190, NOW),
                point(190, NOW));
        assertThat(detect(event(190, null, recent))).isEmpty();
    }

    // ---- combination

    @Test
    void severalAlertsCanBeRaisedByOneMeasurement() {
        GlucoseEvent e = event(320, point(100, NOW.minusHours(1)), recent(260, 270, 280, 300, 320));
        assertThat(detect(e)).extracting(DetectedAlert::type)
                .containsExactlyInAnyOrder(AlertType.HIGH_GLUCOSE, AlertType.UNUSUAL_VARIATION,
                        AlertType.PERSISTENT_HIGH);
    }

    // ---- absence

    @Test
    void absenceAtExactlyAbsenceDays() {
        List<DetectedAlert> r = detector.detectAbsence(new AbsenceEvent(1L, NOW.minusDays(3), 3, 3));
        assertThat(r).hasSize(1);
        assertThat(r.get(0).type()).isEqualTo(AlertType.NO_MEASUREMENT);
        assertThat(r.get(0).severity()).isEqualTo(AlertSeverity.WARNING);
    }

    @Test
    void noAbsenceBelowAbsenceDays() {
        assertThat(detector.detectAbsence(new AbsenceEvent(1L, NOW.minusDays(2), 2, 3))).isEmpty();
    }

    @Test
    void absenceWithoutAnyMeasurementIsDescribed() {
        List<DetectedAlert> r = detector.detectAbsence(new AbsenceEvent(1L, null, 5, 3));
        assertThat(r).hasSize(1);
        assertThat(r.get(0).detail()).contains("aucune mesure enregistrée");
    }

    @Test
    void incompleteAbsenceEventGivesNothing() {
        assertThat(detector.detectAbsence(null)).isEmpty();
        assertThat(detector.detectAbsence(new AbsenceEvent(1L, null, null, 3))).isEmpty();
        assertThat(detector.detectAbsence(new AbsenceEvent(1L, null, 4, null))).isEmpty();
    }

    // ---- lab critical

    @Test
    void labCriticalIsAlwaysCritical() {
        List<DetectedAlert> r = detector.detectLabCritical(
                new LabCriticalEvent(1L, 9L, "Potassium", 6.8, "mmol/L", "CRITICAL_HIGH"));
        assertThat(r).hasSize(1);
        assertThat(r.get(0).type()).isEqualTo(AlertType.CRITICAL_LAB_RESULT);
        assertThat(r.get(0).severity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(r.get(0).detail()).contains("Potassium").contains("6.8 mmol/L").contains("haute");
    }

    @Test
    void labCriticalLowDirection() {
        List<DetectedAlert> r = detector.detectLabCritical(
                new LabCriticalEvent(1L, 9L, "Potassium", 2.1, "mmol/L", "CRITICAL_LOW"));
        assertThat(r.get(0).detail()).contains("basse");
    }

    @Test
    void labCriticalNullEventGivesNothing() {
        assertThat(detector.detectLabCritical(null)).isEmpty();
    }
}
