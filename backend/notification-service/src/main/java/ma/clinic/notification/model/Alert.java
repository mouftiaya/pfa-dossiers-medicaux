package ma.clinic.notification.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "alerts", indexes = @Index(name = "idx_alert_patient_created", columnList = "patient_id, created_at"))
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_status", nullable = false)
    private AlertStatus status;

    /** French text, may contain the measured value: hidden from doctors (privacy rule). */
    @Column(name = "detail", length = 500)
    private String detail;

    /** Set only for alerts raised by the AI trend analysis (0-100, how persistent the problem is). Nullable on purpose. */
    @Column(name = "ai_score")
    private Integer aiScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Alert() {
    }

    public Alert(Long patientId, AlertType type, AlertSeverity severity, String detail) {
        this.patientId = patientId;
        this.type = type;
        this.severity = severity;
        this.detail = detail;
        this.status = AlertStatus.NEW;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = AlertStatus.NEW;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getPatientId() { return patientId; }
    public AlertType getType() { return type; }
    public AlertSeverity getSeverity() { return severity; }
    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }
    public String getDetail() { return detail; }
    public Integer getAiScore() { return aiScore; }
    public void setAiScore(Integer aiScore) { this.aiScore = aiScore; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
