package ma.clinic.results.repository;

import java.util.List;
import java.util.Optional;

import ma.clinic.results.model.AnalysisParameter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisParameterRepository extends JpaRepository<AnalysisParameter, Long> {

    List<AnalysisParameter> findByActiveTrueOrderByLabel();

    Optional<AnalysisParameter> findByCodeAndActiveTrue(String code);

    Optional<AnalysisParameter> findByCode(String code);
}
