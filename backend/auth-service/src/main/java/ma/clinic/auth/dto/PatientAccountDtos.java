package ma.clinic.auth.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class PatientAccountDtos {

    private PatientAccountDtos() {
    }

    public record CreateAccountRequest(
            @NotNull Long patientId,
            @NotBlank @Size(min = 3, max = 50)
            @Pattern(regexp = "[a-z0-9._-]+", message = "Lowercase letters, digits, '.', '_' and '-' only") String username) {
    }

    /** activationUrl and activationExpiresAt are null once the account is activated. */
    public record AccountDto(
            Long patientId,
            String username,
            boolean activated,
            String activationUrl,
            LocalDateTime activationExpiresAt) {
    }

    public record ActivateRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 100) String password) {
    }
}
