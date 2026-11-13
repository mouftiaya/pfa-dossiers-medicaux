package ma.clinic.ai.glucose;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Aggregated glucose history of one patient, as returned by glucose-monitoring-service (no individual value). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GlucoseWindow(
        Long patientId,
        int days,
        LocalDate from,
        LocalDate to,
        int count,
        int daysWithData,
        Double average,
        Double percentInTarget,
        Double percentAboveTarget,
        Double percentBelowLow,
        int criticalHighCount,
        int criticalLowCount,
        List<Week> weeks) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Week(LocalDate weekStart, int count, Double average, Double percentAboveTarget, Double percentBelowLow) {
    }
}
