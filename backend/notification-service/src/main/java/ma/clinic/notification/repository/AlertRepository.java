package ma.clinic.notification.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import ma.clinic.notification.model.Alert;
import ma.clinic.notification.model.AlertStatus;
import ma.clinic.notification.model.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findAllByOrderByCreatedAtDescIdDesc();

    /** Used for de-duplication: open alerts of the same patient and type created after the given instant. */
    List<Alert> findByPatientIdAndTypeAndStatusInAndCreatedAtAfter(Long patientId, AlertType type,
                                                                   Collection<AlertStatus> statuses,
                                                                   LocalDateTime since);
}
