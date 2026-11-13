package ma.clinic.notification.repository;

import java.util.List;

import ma.clinic.notification.model.AlertAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AlertActionRepository extends JpaRepository<AlertAction, Long> {

    List<AlertAction> findByAlertIdOrderByCreatedAtAscIdAsc(Long alertId);

    long countByAlertId(Long alertId);

    /** Rows of {alertId, number of actions} for every alert that has at least one action. */
    @Query("select a.alert.id, count(a) from AlertAction a group by a.alert.id")
    List<Object[]> countActionsGroupedByAlert();
}
