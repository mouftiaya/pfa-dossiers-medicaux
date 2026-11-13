package ma.clinic.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.clinic.auth.model.Role;

public final class UserDtos {

    private UserDtos() {
    }

    public record UserResponse(Long id, String username, Role role, boolean active) {
    }

    public record CreateUserRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotNull Role role) {
    }

    /** Every field is optional: only the ones sent are changed. */
    public record UpdateUserRequest(
            Role role,
            Boolean active,
            @Size(min = 8, max = 100) String password) {
    }
}
