package ma.clinic.glucose.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.clinic.glucose.logic.StatsPeriod;
import ma.clinic.glucose.model.GlucoseFlag;
import ma.clinic.glucose.model.MeasureContext;

/** All values are in mg/dL. The range of "value" is checked in the service (400). */
public final class GlucoseDtos {

    private GlucoseDtos() {
    }

    public record MeasurementRequest(
            @NotNull Double value,
            @NotNull LocalDateTime measuredAt,
            @NotNull MeasureContext context,
            @Size(max = 500) String note) {
    }

    public record MeasurementDto(
            Long id,
            Double value,
            LocalDateTime measuredAt,
            MeasureContext context,
            String note,
            GlucoseFlag flag) {
    }

    public record TargetRequest(
            @NotNull Double targetMin,
            @NotNull Double targetMax,
            @NotNull Double lowThreshold,
            @NotNull Double highThreshold,
            @NotNull Double criticalLow,
            @NotNull Double criticalHigh,
            @NotNull Integer absenceDays) {
    }

    /** defined is false when the default values are used (no target stored for the patient). */
    public record TargetDto(
            Long patientId,
            double targetMin,
            double targetMax,
            double lowThreshold,
            double highThreshold,
            double criticalLow,
            double criticalHigh,
            int absenceDays,
            boolean defined,
            boolean validated,
            String definedBy,
            LocalDateTime updatedAt) {
    }

    public record StatsPoint(
            LocalDateTime at,
            Double average,
            Double minimum,
            Double maximum,
            int count) {
    }

    public record StatsDto(
            StatsPeriod period,
            LocalDate from,
            LocalDate to,
            int count,
            Double average,
            Double minimum,
            Double maximum,
            Double percentInTarget,
            int highCount,
            int lowCount,
            TargetDto target,
            List<StatsPoint> points) {
    }

    /** Deliberately contains no individual value (privacy rule). */
    public record SummaryDto(
            Long patientId,
            int periodDays,
            int count,
            Double average,
            Double percentInTarget,
            int highCount,
            int lowCount,
            LocalDateTime lastMeasurementAt,
            TargetDto target) {
    }

    /** One 7-day bucket of a window (weekStart = first day). Percentages are null when count is 0. */
    public record WeekDto(
            LocalDate weekStart,
            int count,
            Double average,
            Double percentAboveTarget,
            Double percentBelowLow) {
    }

    /**
     * Aggregated history of a patient over a long window, for the AI service (internal endpoint only).
     * Contains no individual value.
     */
    public record WindowDto(
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
            List<WeekDto> weeks) {
    }

    public record DemoResultDto(String profile, int days, int created) {
    }
}
