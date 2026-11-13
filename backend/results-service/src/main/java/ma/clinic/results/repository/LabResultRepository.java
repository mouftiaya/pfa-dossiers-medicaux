package ma.clinic.results.repository;

import java.util.List;

import ma.clinic.results.model.LabResult;
import ma.clinic.results.model.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabResultRepository extends JpaRepository<LabResult, Long> {

    List<LabResult> findByPatientIdOrderByResultDateDescIdDesc(Long patientId);

    /** The official results only (patient portal). */
    List<LabResult> findByPatientIdAndStatusOrderByResultDateDescIdDesc(Long patientId, ReviewStatus status);
}
