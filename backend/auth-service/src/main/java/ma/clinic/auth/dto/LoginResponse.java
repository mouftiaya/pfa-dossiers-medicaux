package ma.clinic.auth.dto;

/** patientId is only set for PATIENT accounts (null for the staff). */
public record LoginResponse(String token, String username, String role, Long patientId) {
}
