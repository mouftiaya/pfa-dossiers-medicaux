package ma.clinic.auth.controller;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

import jakarta.validation.Valid;
import ma.clinic.auth.client.PatientClient;
import ma.clinic.auth.dto.PatientAccountDtos.AccountDto;
import ma.clinic.auth.dto.PatientAccountDtos.CreateAccountRequest;
import ma.clinic.auth.model.AppUser;
import ma.clinic.auth.repository.UserRepository;
import ma.clinic.auth.service.AuditService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Patient portal accounts, reserved to the assistant (see SecurityConfig).
 * The assistant creates the account and gives the activation link to the patient, who then chooses a password.
 * Until then the account is disabled and its password is a random hash nobody knows.
 */
@RestController
@RequestMapping("/api/patient-accounts")
public class PatientAccountController {

    private static final long ACTIVATION_HOURS = 72;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final PatientClient patients;
    private final AuditService audit;
    private final String frontendUrl;

    public PatientAccountController(UserRepository users, PasswordEncoder passwordEncoder, PatientClient patients,
                                    AuditService audit,
                                    @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.patients = patients;
        this.audit = audit;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public AccountDto create(@Valid @RequestBody CreateAccountRequest request, Authentication authentication) {
        if (!patients.exists(request.patientId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        }
        if (users.findByPatientId(request.patientId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This patient already has an account");
        }
        String username = request.username().trim();
        if (users.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already used");
        }
        AppUser user = users.save(AppUser.forPatient(username,
                passwordEncoder.encode(UUID.randomUUID().toString()), request.patientId(),
                newToken(), LocalDateTime.now().plusHours(ACTIVATION_HOURS)));
        audit.log(authentication.getName(), "PATIENT_ACCOUNT_CREATED", username + " (patient " + request.patientId() + ")");
        return toDto(user);
    }

    @GetMapping("/by-patient/{patientId}")
    public AccountDto get(@PathVariable Long patientId) {
        return toDto(find(patientId));
    }

    /** A new link (the previous one stops working), e.g. when the first one expired or was lost. */
    @PostMapping("/by-patient/{patientId}/reset-activation")
    @Transactional
    public AccountDto resetActivation(@PathVariable Long patientId, Authentication authentication) {
        AppUser user = find(patientId);
        if (isActivated(user)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This account is already activated");
        }
        user.setActivation(newToken(), LocalDateTime.now().plusHours(ACTIVATION_HOURS));
        audit.log(authentication.getName(), "PATIENT_ACTIVATION_RESET", user.getUsername());
        return toDto(users.save(user));
    }

    private AppUser find(Long patientId) {
        return users.findByPatientId(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This patient has no account"));
    }

    private static boolean isActivated(AppUser user) {
        return user.getActivationToken() == null;
    }

    private AccountDto toDto(AppUser user) {
        boolean activated = isActivated(user);
        return new AccountDto(user.getPatientId(), user.getUsername(), activated,
                activated ? null : frontendUrl + "/activate?token=" + user.getActivationToken(),
                activated ? null : user.getActivationExpiresAt());
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
