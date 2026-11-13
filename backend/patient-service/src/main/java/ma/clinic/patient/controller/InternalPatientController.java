package ma.clinic.patient.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import ma.clinic.patient.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Endpoints for the other services only (not routed by the gateway); protected by the shared internal key. */
@RestController
@RequestMapping("/internal/patients")
public class InternalPatientController {

    public record Contact(String firstName, String phone, String email) {
    }

    private final PatientRepository patients;
    private final String internalKey;

    public InternalPatientController(PatientRepository patients,
                                     @Value("${internal.api-key}") String internalKey) {
        this.patients = patients;
        this.internalKey = internalKey;
    }

    /** Also used by auth-service to check that a patient file exists before creating a portal account. */
    @GetMapping("/{id}/contact")
    public Contact contact(@PathVariable Long id,
                           @RequestHeader(value = "X-Internal-Key", required = false) String key) {
        requireKey(key);
        return patients.findById(id)
                .map(p -> new Contact(p.getFirstName(), p.getPhone(), p.getEmail()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
    }

    private void requireKey(String key) {
        boolean ok = key != null && MessageDigest.isEqual(
                key.getBytes(StandardCharsets.UTF_8), internalKey.getBytes(StandardCharsets.UTF_8));
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal key");
        }
    }
}
