package ma.clinic.glucose.repository;

import java.util.Optional;

import ma.clinic.glucose.model.GlucoseTarget;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlucoseTargetRepository extends JpaRepository<GlucoseTarget, Long> {

    Optional<GlucoseTarget> findByPatientId(Long patientId);
}
