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
import jakarta.persistence.Table;

/** Trace of a message sent (or not sent) to a patient. The content never contains medical information. */
@Entity
@Table(name = "patient_notifications",
        indexes = @Index(name = "idx_pnotif_patient_channel", columnList = "patient_id, channel, created_at"))
public class PatientNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", nullable = false)
    private Long alertId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false)
    private DeliveryStatus status;

    @Column(name = "recipient")
    private String recipient;

    @Column(name = "content", length = 1000)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PatientNotification() {
    }

    public PatientNotification(Long alertId, Long patientId, Channel channel, DeliveryStatus status,
                               String recipient, String content) {
        this.alertId = alertId;
        this.patientId = patientId;
        this.channel = channel;
        this.status = status;
        this.recipient = recipient;
        this.content = content;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getAlertId() { return alertId; }
    public Long getPatientId() { return patientId; }
    public Channel getChannel() { return channel; }
    public DeliveryStatus getStatus() { return status; }
    public String getRecipient() { return recipient; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
