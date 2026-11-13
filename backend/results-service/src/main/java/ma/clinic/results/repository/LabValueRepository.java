package ma.clinic.results.repository;

import java.time.LocalDate;
import java.util.List;

import ma.clinic.results.model.LabValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LabValueRepository extends JpaRepository<LabValue, Long> {

    /** Validated values of one parameter for one patient, oldest first (for the evolution graph). */
    @Query("""
            select v from LabValue v
            join fetch v.labResult r
            join fetch v.parameter p
            where r.patientId = :patientId
              and p.code = :code
              and r.status = ma.clinic.results.model.ReviewStatus.VALIDATED
              and r.resultDate >= :from and r.resultDate <= :to
            order by r.resultDate asc, r.id asc
            """)
    List<LabValue> findValidatedSeries(@Param("patientId") Long patientId,
                                       @Param("code") String code,
                                       @Param("from") LocalDate from,
                                       @Param("to") LocalDate to);
}
