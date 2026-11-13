package ma.clinic.ai.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.clinic.ai.model.SuggestionStatus;

public final class AiDtos {

    private AiDtos() {
    }

    public enum Decision {
        APPROVE,
        MODIFY,
        REJECT
    }

    /** consultationId is optional: if absent, the one stored on the lab result is used. */
    public record AnalysisRequest(
            @NotNull Long labResultId,
            Long consultationId) {
    }

    /** finalDiagnosis is required when the decision is MODIFY. */
    public record DecisionRequest(
            @NotNull Decision decision,
            @Size(max = 2000) String finalDiagnosis) {
    }

    public record AbnormalValue(
            String code,
            String label,
            Double value,
            String unit,
            String flag) {
    }

    public record AnalysisResponse(
            Long suggestionId,
            Long labResultId,
            List<AbnormalValue> abnormalValues,
            String suggestedDiagnosis,
            Double confidence,
            SuggestionStatus status) {
    }
}
