package ma.clinic.ai.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** A diagnostic suggestion produced for one validated lab result, and the doctor's decision about it. */
@Entity
@Table(name = "ai_suggestions")
public class AiSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // References to other services (no foreign key: each service owns its own database)
    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private Long labResultId;

    private Long consultationId;

    @Column(nullable = false, length = 3000)
    private String suggestedDiagnosis;

    /** Share of the key parameters available for the analysis (0 to 1). NOT a probability. */
    @Column(nullable = false)
    private Double confidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionStatus status = SuggestionStatus.PENDING;

    /** The diagnosis kept by the doctor (the suggestion if approved, his own text if modified). */
    @Column(length = 2000)
    private String finalDiagnosis;

    @Column(nullable = false, updatable = false)
    private String requestedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private String decidedBy;

    private LocalDateTime decidedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    protected AiSuggestion() {
    }

    public AiSuggestion(Long patientId, Long labResultId, Long consultationId, String suggestedDiagnosis,
                        Double confidence, String requestedBy) {
        this.patientId = patientId;
        this.labResultId = labResultId;
        this.consultationId = consultationId;
        this.suggestedDiagnosis = suggestedDiagnosis;
        this.confidence = confidence;
        this.requestedBy = requestedBy;
    }

    /** Records the doctor's decision. */
    public void decide(SuggestionStatus newStatus, String finalDiagnosis, String doctor) {
        this.status = newStatus;
        this.finalDiagnosis = finalDiagnosis;
        this.decidedBy = doctor;
        this.decidedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public Long getLabResultId() {
        return labResultId;
    }

    public Long getConsultationId() {
        return consultationId;
    }

    public String getSuggestedDiagnosis() {
        return suggestedDiagnosis;
    }

    public Double getConfidence() {
        return confidence;
    }

    public SuggestionStatus getStatus() {
        return status;
    }

    public String getFinalDiagnosis() {
        return finalDiagnosis;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }
}
