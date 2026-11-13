package ma.clinic.glucose.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import ma.clinic.glucose.model.GlucoseMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GlucoseMeasurementRepository extends JpaRepository<GlucoseMeasurement, Long> {

    /** Measurements with from <= measuredAt < to, newest first. */
    @Query("select m from GlucoseMeasurement m where m.patientId = :patientId "
            + "and m.measuredAt >= :from and m.measuredAt < :to order by m.measuredAt desc")
    List<GlucoseMeasurement> findInRange(@Param("patientId") Long patientId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    Optional<GlucoseMeasurement> findByIdAndPatientId(Long id, Long patientId);

    /** The latest measurements strictly before the given instant, newest first. */
    List<GlucoseMeasurement> findTop4ByPatientIdAndMeasuredAtBeforeOrderByMeasuredAtDesc(
            Long patientId, LocalDateTime before);

    @Query("select max(m.measuredAt) from GlucoseMeasurement m where m.patientId = :patientId")
    LocalDateTime findLastMeasuredAt(@Param("patientId") Long patientId);

    @Query("select distinct m.patientId from GlucoseMeasurement m")
    List<Long> findDistinctPatientIds();
}
