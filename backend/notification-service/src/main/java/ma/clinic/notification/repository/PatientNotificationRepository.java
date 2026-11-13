package ma.clinic.notification.repository;

import java.time.LocalDateTime;
import java.util.List;

import ma.clinic.notification.model.Channel;
import ma.clinic.notification.model.DeliveryStatus;
import ma.clinic.notification.model.PatientNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientNotificationRepository extends JpaRepository<PatientNotification, Long> {

    List<PatientNotification> findByAlertIdOrderByCreatedAtAscIdAsc(Long alertId);

    /** Used for throttling: was a message already sent on this channel to this patient after the given instant? */
    boolean existsByPatientIdAndChannelAndStatusAndCreatedAtAfter(Long patientId, Channel channel,
                                                                  DeliveryStatus status, LocalDateTime since);
}
