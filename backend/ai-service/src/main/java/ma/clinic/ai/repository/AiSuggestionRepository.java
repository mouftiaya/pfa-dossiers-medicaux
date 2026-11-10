package ma.clinic.ai.repository;

import java.util.List;

import ma.clinic.ai.model.AiSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSuggestionRepository extends JpaRepository<AiSuggestion, Long> {

    List<AiSuggestion> findByPatientIdOrderByCreatedAtDescIdDesc(Long patientId);

    List<AiSuggestion> findByLabResultIdOrderByCreatedAtDescIdDesc(Long labResultId);
}
