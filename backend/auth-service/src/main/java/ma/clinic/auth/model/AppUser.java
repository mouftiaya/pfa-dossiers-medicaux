package ma.clinic.auth.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password; // BCrypt hash, never the raw password

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // The default lets "ddl-auto: update" add this column to an existing table that already has users
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    /** Only for PATIENT accounts: the patient file (patient-service) this account belongs to. */
    @Column(unique = true)
    private Long patientId;

    /** Single-use activation link token; null once the patient has chosen a password. */
    @Column(unique = true, length = 100)
    private String activationToken;

    private LocalDateTime activationExpiresAt;

    protected AppUser() {
    }

    public AppUser(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    /** A patient account: disabled and with an unusable password until the patient activates it. */
    public static AppUser forPatient(String username, String unusablePasswordHash, Long patientId,
                                     String activationToken, LocalDateTime activationExpiresAt) {
        AppUser user = new AppUser(username, unusablePasswordHash, Role.PATIENT);
        user.active = false;
        user.patientId = patientId;
        user.activationToken = activationToken;
        user.activationExpiresAt = activationExpiresAt;
        return user;
    }

    public Long getPatientId() {
        return patientId;
    }

    public String getActivationToken() {
        return activationToken;
    }

    public LocalDateTime getActivationExpiresAt() {
        return activationExpiresAt;
    }

    public void setActivation(String token, LocalDateTime expiresAt) {
        this.activationToken = token;
        this.activationExpiresAt = expiresAt;
    }

    /** The patient chose a password: the link is consumed and the account is enabled. */
    public void activate(String passwordHash) {
        this.password = passwordHash;
        this.active = true;
        this.activationToken = null;
        this.activationExpiresAt = null;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
