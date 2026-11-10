package ma.clinic.consultation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiagnosisRequest(
        @NotBlank @Size(max = 2000) String diagnosis) {
}
