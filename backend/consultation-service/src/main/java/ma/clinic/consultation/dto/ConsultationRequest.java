package ma.clinic.consultation.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record ConsultationRequest(
        @NotNull Long patientId,
        @NotNull @PastOrPresent LocalDate consultationDate,
        @NotBlank @Size(max = 500) String reason,
        @Size(max = 4000) String notes,
        @Size(max = 1000) String prescribedAnalyses,
        @Size(max = 2000) String diagnosis) {
}
