package ma.clinic.results.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** The result of one analysis session brought back by a patient (one paper = one LabResult). */
@Entity
@Table(name = "lab_results")
public class LabResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // References to other services (no foreign key: each service owns its own database)
    @Column(nullable = false)
    private Long patientId;

    private Long consultationId;

    /** Date of the analysis (printed on the paper). */
    @Column(nullable = false)
    private LocalDate resultDate;

    /** Original name of the uploaded file, for display only. */
    private String fileName;

    /** Name of the file on disk (random, never the user's file name). */
    @Column(updatable = false)
    private String storedFileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryMode entryMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status = ReviewStatus.PENDING_REVIEW;

    /** Why the doctor asked for a correction (empty otherwise). */
    @Column(length = 1000)
    private String reviewNote;

    @Column(nullable = false, updatable = false)
    private String uploadedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    private String validatedBy;

    private LocalDateTime validatedAt;

    @OneToMany(mappedBy = "labResult", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<LabValue> values = new ArrayList<>();

    @PrePersist
    void onCreate() {
        this.uploadedAt = LocalDateTime.now();
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

    public Long getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(Long consultationId) {
        this.consultationId = consultationId;
    }

    public LocalDate getResultDate() {
        return resultDate;
    }

    public void setResultDate(LocalDate resultDate) {
        this.resultDate = resultDate;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public EntryMode getEntryMode() {
        return entryMode;
    }

    public void setEntryMode(EntryMode entryMode) {
        this.entryMode = entryMode;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public String getValidatedBy() {
        return validatedBy;
    }

    public void setValidatedBy(String validatedBy) {
        this.validatedBy = validatedBy;
    }

    public LocalDateTime getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(LocalDateTime validatedAt) {
        this.validatedAt = validatedAt;
    }

    public List<LabValue> getValues() {
        return values;
    }
}
