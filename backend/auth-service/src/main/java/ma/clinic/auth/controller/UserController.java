package ma.clinic.auth.controller;

import java.util.List;

import jakarta.validation.Valid;
import ma.clinic.auth.dto.UserDtos.CreateUserRequest;
import ma.clinic.auth.dto.UserDtos.UpdateUserRequest;
import ma.clinic.auth.dto.UserDtos.UserResponse;
import ma.clinic.auth.model.AppUser;
import ma.clinic.auth.model.Role;
import ma.clinic.auth.repository.UserRepository;
import ma.clinic.auth.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** User management, reserved to the administrator (see SecurityConfig). */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public UserController(UserRepository users, PasswordEncoder passwordEncoder, AuditService audit) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @GetMapping
    public List<UserResponse> list() {
        return users.findAll().stream().map(UserController::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request, Authentication authentication) {
        if (request.role() == Role.PATIENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Patient accounts are created by the assistant from the patient file");
        }
        String username = request.username().trim();
        if (users.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already used");
        }
        AppUser user = users.save(new AppUser(username, passwordEncoder.encode(request.password()), request.role()));
        audit.log(authentication.getName(), "USER_CREATED", username + " (" + request.role() + ")");
        return toResponse(user);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request,
                               Authentication authentication) {
        AppUser user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        boolean isSelf = user.getUsername().equals(authentication.getName());

        // Patient accounts keep their role and their own password (activation link); the admin may only enable/disable
        boolean patientAccount = user.getRole() == Role.PATIENT;
        if (patientAccount && ((request.role() != null && request.role() != Role.PATIENT)
                || request.password() != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A patient account can only be enabled or disabled here");
        }
        if (!patientAccount && request.role() == Role.PATIENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A staff account cannot become a patient account");
        }
        // An administrator cannot lock himself out
        if (isSelf && Boolean.FALSE.equals(request.active())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot disable your own account");
        }
        if (isSelf && request.role() != null && request.role() != user.getRole()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot change your own role");
        }

        if (request.role() != null && request.role() != user.getRole()) {
            audit.log(authentication.getName(), "USER_ROLE_CHANGED",
                    user.getUsername() + ": " + user.getRole() + " -> " + request.role());
            user.setRole(request.role());
        }
        if (request.active() != null && request.active() != user.isActive()) {
            audit.log(authentication.getName(), request.active() ? "USER_ENABLED" : "USER_DISABLED",
                    user.getUsername());
            user.setActive(request.active());
        }
        if (request.password() != null) {
            user.setPassword(passwordEncoder.encode(request.password()));
            audit.log(authentication.getName(), "USER_PASSWORD_RESET", user.getUsername());
        }
        return toResponse(users.save(user));
    }

    private static UserResponse toResponse(AppUser u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getRole(), u.isActive());
    }
}
