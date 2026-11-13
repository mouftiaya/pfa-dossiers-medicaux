package ma.clinic.glucose.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Glucose targets and alert thresholds (mg/dL) defined by the doctor for one patient. */
@Entity
@Table(name = "glucose_targets")
public class GlucoseTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false, unique = true)
    private Long patientId;

    @Column(nullable = false)
    private double targetMin;

    @Column(nullable = false)
    private double targetMax;

    @Column(nullable = false)
    private double lowThreshold;

    @Column(nullable = false)
    private double highThreshold;

    @Column(nullable = false)
    private double criticalLow;

    @Column(nullable = false)
    private double criticalHigh;

    @Column(nullable = false)
    private int absenceDays;

    @Column(nullable = false)
    private boolean validated;

    private String definedBy;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public double getTargetMin() {
        return targetMin;
    }

    public void setTargetMin(double targetMin) {
        this.targetMin = targetMin;
    }

    public double getTargetMax() {
        return targetMax;
    }

    public void setTargetMax(double targetMax) {
        this.targetMax = targetMax;
    }

    public double getLowThreshold() {
        return lowThreshold;
    }

    public void setLowThreshold(double lowThreshold) {
        this.lowThreshold = lowThreshold;
    }

    public double getHighThreshold() {
        return highThreshold;
    }

    public void setHighThreshold(double highThreshold) {
        this.highThreshold = highThreshold;
    }

    public double getCriticalLow() {
        return criticalLow;
    }

    public void setCriticalLow(double criticalLow) {
        this.criticalLow = criticalLow;
    }

    public double getCriticalHigh() {
        return criticalHigh;
    }

    public void setCriticalHigh(double criticalHigh) {
        this.criticalHigh = criticalHigh;
    }

    public int getAbsenceDays() {
        return absenceDays;
    }

    public void setAbsenceDays(int absenceDays) {
        this.absenceDays = absenceDays;
    }

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    public String getDefinedBy() {
        return definedBy;
    }

    public void setDefinedBy(String definedBy) {
        this.definedBy = definedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
