package ma.clinic.results.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import ma.clinic.results.model.EntryMode;
import ma.clinic.results.model.ReviewStatus;
import ma.clinic.results.model.ValueFlag;

/** Request and response objects of the results API. */
public final class ResultDtos {

    private ResultDtos() {
    }

    // ---------- requests ----------

    public record ValueInput(
            @NotNull Long parameterId,
            @NotNull @PositiveOrZero Double value) {
    }

    /** Manual entry of a whole result (no file). */
    public record ResultRequest(
            @NotNull Long patientId,
            Long consultationId,
            @NotNull @PastOrPresent LocalDate resultDate,
            @NotEmpty @Valid List<ValueInput> values) {
    }

    /** Replaces the values of a result still pending review. */
    public record ValuesRequest(
            @NotEmpty @Valid List<ValueInput> values) {
    }

    public record CorrectionRequest(
            @NotBlank @Size(max = 1000) String note) {
    }

    // ---------- responses ----------

    public record ParameterDto(
            Long id,
            String code,
            String label,
            String unit,
            Double refMin,
            Double refMax) {
    }

    public record ValueDto(
            Long id,
            ParameterDto parameter,
            Double value,
            ValueFlag flag) {
    }

    public record ResultDto(
            Long id,
            Long patientId,
            Long consultationId,
            LocalDate resultDate,
            String fileName,
            boolean hasFile,
            EntryMode entryMode,
            ReviewStatus status,
            String reviewNote,
            String uploadedBy,
            LocalDateTime uploadedAt,
            String validatedBy,
            LocalDateTime validatedAt,
            List<ValueDto> values) {
    }

    /** Answer to an upload: the created result and what the extraction found. */
    public record UploadResponse(
            ResultDto result,
            boolean extracted,
            int recognizedCount,
            String message) {
    }

    public record EvolutionPoint(
            Long resultId,
            LocalDate date,
            Double value,
            ValueFlag flag) {
    }

    public record EvolutionResponse(
            ParameterDto parameter,
            List<EvolutionPoint> points) {
    }

    /** One parameter compared across several results; change = last value - first value. */
    public record ComparisonRow(
            ParameterDto parameter,
            List<EvolutionPoint> points,
            Double change) {
    }
}
