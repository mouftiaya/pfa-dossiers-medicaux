package ma.clinic.results.repository;

import java.util.List;

import ma.clinic.results.model.LabResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabResultRepository extends JpaRepository<LabResult, Long> {

    List<LabResult> findByPatientIdOrderByResultDateDescIdDesc(Long patientId);
}
