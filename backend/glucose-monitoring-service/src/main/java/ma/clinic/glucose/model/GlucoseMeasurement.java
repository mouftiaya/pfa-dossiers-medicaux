package ma.clinic.glucose.model;

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

/** One blood glucose reading of a patient. The value is in mg/dL. */
@Entity
@Table(name = "glucose_measurements",
        indexes = @Index(name = "idx_glucose_patient_time", columnList = "patient_id, measured_at"))
public class GlucoseMeasurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    /** mg/dL ("value" is a reserved word in some databases, hence the column name). */
    @Column(name = "glucose_value", nullable = false)
    private Double value;

    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeasureContext context;

    @Column(length = 500)
    private String note;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public LocalDateTime getMeasuredAt() {
        return measuredAt;
    }

    public void setMeasuredAt(LocalDateTime measuredAt) {
        this.measuredAt = measuredAt;
    }

    public MeasureContext getContext() {
        return context;
    }

    public void setContext(MeasureContext context) {
        this.context = context;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
