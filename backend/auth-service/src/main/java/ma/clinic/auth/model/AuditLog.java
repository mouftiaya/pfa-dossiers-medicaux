package ma.clinic.auth.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One line of the activity journal (who did what, when). */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Who did it (the username; for a failed login, the name that was tried). */
    @Column(nullable = false, updatable = false)
    private String username;

    @Column(nullable = false, updatable = false, length = 50)
    private String action;

    @Column(updatable = false, length = 500)
    private String details;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AuditLog() {
    }

    public AuditLog(String username, String action, String details) {
        this.username = truncate(username, 255);
        this.action = truncate(action, 50);
        this.details = truncate(details, 500);
        this.createdAt = LocalDateTime.now();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
