package ma.clinic.notification.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Bodies of the internal events received from the other services. */
public final class EventDtos {

    private EventDtos() {
    }

    /** Event posted by glucose-monitoring-service after a measurement is created (unit: mg/dL). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GlucoseEvent(Long patientId, Double value, LocalDateTime measuredAt, String context,
                               Thresholds thresholds, Point previous, List<Point> recent) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Thresholds(Double criticalLow, Double lowThreshold, Double targetMin, Double targetMax,
                             Double highThreshold, Double criticalHigh) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Point(Double value, LocalDateTime measuredAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AbsenceEvent(Long patientId, LocalDateTime lastMeasurementAt, Integer daysWithoutMeasurement,
                               Integer absenceDays) {
    }

    /** flag: CRITICAL_LOW or CRITICAL_HIGH. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LabCriticalEvent(Long patientId, Long resultId, String parameterLabel, Double value, String unit,
                                   String flag) {
    }

    /** Posted by ai-service when the trend analysis finds a persistent problem. direction: HIGH or LOW. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiRiskEvent(Long patientId, String direction, Integer score, Integer windowDays,
                              Integer daysWithData, Integer badWeeks, Integer judgedWeeks, Double average,
                              String summary) {
    }
}
