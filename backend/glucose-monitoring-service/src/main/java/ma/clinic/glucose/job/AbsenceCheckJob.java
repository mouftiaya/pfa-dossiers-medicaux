package ma.clinic.glucose.job;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import ma.clinic.glucose.client.NotificationClient;
import ma.clinic.glucose.logic.Thresholds;
import ma.clinic.glucose.model.GlucoseTarget;
import ma.clinic.glucose.repository.GlucoseMeasurementRepository;
import ma.clinic.glucose.repository.GlucoseTargetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Daily check: tells notification-service about patients who stopped measuring their glucose. */
@Component
public class AbsenceCheckJob {

    private static final Logger log = LoggerFactory.getLogger(AbsenceCheckJob.class);

    private final GlucoseMeasurementRepository measurements;
    private final GlucoseTargetRepository targets;
    private final NotificationClient notifications;

    public AbsenceCheckJob(GlucoseMeasurementRepository measurements, GlucoseTargetRepository targets,
                           NotificationClient notifications) {
        this.measurements = measurements;
        this.targets = targets;
        this.notifications = notifications;
    }

    @Scheduled(cron = "${glucose.absence-check-cron:0 0 9 * * *}")
    public void checkAbsences() {
        try {
            run(LocalDateTime.now());
        } catch (Exception e) {
            log.warn("Absence check failed: {}", e.toString());
        }
    }

    void run(LocalDateTime now) {
        // Every patient that has a target or at least one measurement.
        Set<Long> patientIds = new LinkedHashSet<>(measurements.findDistinctPatientIds());
        for (GlucoseTarget g : targets.findAll()) {
            patientIds.add(g.getPatientId());
        }
        for (Long patientId : patientIds) {
            try {
                checkPatient(patientId, now);
            } catch (Exception e) {
                log.warn("Absence check failed for patient {}: {}", patientId, e.toString());
            }
        }
    }

    private void checkPatient(Long patientId, LocalDateTime now) {
        Optional<GlucoseTarget> target = targets.findByPatientId(patientId);
        int absenceDays = target.map(GlucoseTarget::getAbsenceDays).orElse(Thresholds.DEFAULTS.absenceDays());
        LocalDateTime last = measurements.findLastMeasuredAt(patientId);
        LocalDateTime since = last;
        if (since == null) {
            since = target.map(GlucoseTarget::getUpdatedAt).orElse(null);
        }
        if (since == null) {
            return;
        }
        long days = ChronoUnit.DAYS.between(since, now);
        if (days >= absenceDays) {
            notifications.notifyAbsence(patientId, last, days, absenceDays);
        }
    }
}
