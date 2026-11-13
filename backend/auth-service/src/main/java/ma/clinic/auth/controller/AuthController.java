package ma.clinic.auth.controller;

import jakarta.validation.Valid;
import java.time.LocalDateTime;

import ma.clinic.auth.dto.LoginRequest;
import ma.clinic.auth.dto.LoginResponse;
import ma.clinic.auth.dto.PatientAccountDtos.ActivateRequest;
import ma.clinic.auth.model.AppUser;
import ma.clinic.auth.repository.UserRepository;
import ma.clinic.auth.service.AuditService;
import ma.clinic.auth.service.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuditService audit;

    public AuthController(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokenService,
                          AuditService audit) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.audit = audit;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AppUser user = users.findByUsername(request.username())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElse(null);
        if (user == null) {
            audit.log(request.username(), "LOGIN_FAILED", "Invalid username or password");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        // Checked after the password so that a disabled account does not reveal that it exists
        if (!user.isActive()) {
            audit.log(user.getUsername(), "LOGIN_REFUSED", "Account disabled");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        audit.log(user.getUsername(), "LOGIN_SUCCESS", null);
        return new LoginResponse(tokenService.generate(user), user.getUsername(), user.getRole().name(),
                user.getPatientId());
    }

    /** Public: the patient chooses a password with the single-use link given by the assistant. */
    @PostMapping("/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void activate(@Valid @RequestBody ActivateRequest request) {
        AppUser user = users.findByActivationToken(request.token())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid activation link"));
        if (user.getActivationExpiresAt() == null || user.getActivationExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "This activation link has expired");
        }
        user.activate(passwordEncoder.encode(request.password()));
        users.save(user);
        audit.log(user.getUsername(), "PATIENT_ACCOUNT_ACTIVATED", null);
    }
}
