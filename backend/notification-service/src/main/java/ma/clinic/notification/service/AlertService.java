package ma.clinic.notification.service;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ma.clinic.notification.detection.AlertDetector;
import ma.clinic.notification.detection.AlertRules;
import ma.clinic.notification.detection.DetectedAlert;
import ma.clinic.notification.dto.AlertDtos.ActionRequest;
import ma.clinic.notification.dto.AlertDtos.AlertDetailDto;
import ma.clinic.notification.dto.AlertDtos.AlertDto;
import ma.clinic.notification.dto.AlertDtos.SummaryDto;
import ma.clinic.notification.dto.EventDtos.AbsenceEvent;
import ma.clinic.notification.dto.EventDtos.AiRiskEvent;
import ma.clinic.notification.dto.EventDtos.GlucoseEvent;
import ma.clinic.notification.dto.EventDtos.LabCriticalEvent;
import ma.clinic.notification.model.ActionType;
import ma.clinic.notification.model.Alert;
import ma.clinic.notification.model.AlertAction;
import ma.clinic.notification.model.AlertSeverity;
import ma.clinic.notification.model.AlertStatus;
import ma.clinic.notification.model.AlertType;
import ma.clinic.notification.repository.AlertActionRepository;
import ma.clinic.notification.repository.AlertRepository;
import ma.clinic.notification.repository.PatientNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);
    private static final Set<AlertStatus> OPEN = Set.of(AlertStatus.NEW, AlertStatus.IN_PROGRESS);

    private final AlertRepository alerts;
    private final AlertActionRepository actions;
    private final PatientNotificationRepository notifications;
    private final AlertDetector detector;
    private final AlertRules rules;
    private final PatientNotificationService patientNotifier;
    private final boolean instantGlucoseEnabled;
    private final int aiDedupDays;

    public AlertService(AlertRepository alerts, AlertActionRepository actions,
                        PatientNotificationRepository notifications, AlertDetector detector, AlertRules rules,
                        PatientNotificationService patientNotifier,
                        @Value("${alerts.instant-glucose-enabled:false}") boolean instantGlucoseEnabled,
                        @Value("${alerts.ai-dedup-days:30}") int aiDedupDays) {
        this.alerts = alerts;
        this.actions = actions;
        this.notifications = notifications;
        this.detector = detector;
        this.rules = rules;
        this.patientNotifier = patientNotifier;
        this.instantGlucoseEnabled = instantGlucoseEnabled;
        this.aiDedupDays = aiDedupDays;
    }

    // ---------------------------------------------------------------- events

    /**
     * Returns the number of alerts created. By default (alerts.instant-glucose-enabled=false) a single measurement
     * never raises an alert: the AI service watches the weeks of history instead (see {@link #handleAiRisk}).
     */
    public int handleGlucose(GlucoseEvent event) {
        requirePatient(event == null ? null : event.patientId());
        if (!instantGlucoseEnabled) {
            return 0;
        }
        return createAlerts(event.patientId(), detector.detectGlucose(event));
    }

    public int handleAbsence(AbsenceEvent event) {
        requirePatient(event == null ? null : event.patientId());
        return createAlerts(event.patientId(), detector.detectAbsence(event));
    }

    public int handleLabCritical(LabCriticalEvent event) {
        requirePatient(event == null ? null : event.patientId());
        return createAlerts(event.patientId(), detector.detectLabCritical(event));
    }

    /**
     * Alert raised by the AI trend analysis. The patient is NOT contacted automatically: the assistant decides
     * (call or e-mail). Returns "created", or "duplicate" when an open AI alert of the same kind already exists.
     */
    public String handleAiRisk(AiRiskEvent event) {
        requirePatient(event == null ? null : event.patientId());
        AlertType type = "LOW".equals(event.direction()) ? AlertType.PERSISTENT_LOW : AlertType.PERSISTENT_HIGH;
        int score = event.score() == null ? 0 : Math.max(0, Math.min(100, event.score()));
        LocalDateTime since = LocalDateTime.now().minusDays(aiDedupDays);
        boolean alreadyOpen = alerts.findByPatientIdAndTypeAndStatusInAndCreatedAtAfter(
                        event.patientId(), type, OPEN, since).stream()
                .anyMatch(a -> a.getAiScore() != null);
        if (alreadyOpen) {
            return "duplicate";
        }
        String detail = truncate("Analyse IA : " + (event.summary() == null ? "tendance persistante" : event.summary()), 500);
        Alert alert = new Alert(event.patientId(), type, aiSeverity(score), detail);
        alert.setAiScore(score);
        alerts.save(alert);
        return "created";
    }

    /** From this score the AI alert is CRITICAL (more urgent for the assistant), otherwise WARNING. */
    public static AlertSeverity aiSeverity(int score) {
        return score >= 80 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
    }

    private static void requirePatient(Long patientId) {
        if (patientId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patientId is required");
        }
    }

    /** Not transactional on purpose: the calls to patient-service must not hold a database connection. */
    private int createAlerts(Long patientId, List<DetectedAlert> detected) {
        int created = 0;
        for (DetectedAlert d : detected) {
            LocalDateTime since = LocalDateTime.now().minusHours(rules.dedupHours());
            List<Alert> open = alerts.findByPatientIdAndTypeAndStatusInAndCreatedAtAfter(
                    patientId, d.type(), OPEN, since);
            if (blocksNewAlert(d.severity(), open.stream().map(Alert::getSeverity).toList())) {
                log.debug("Alert {} for patient {} skipped (duplicate)", d.type(), patientId);
                continue;
            }
            Alert alert = alerts.save(new Alert(patientId, d.type(), d.severity(), truncate(d.detail(), 500)));
            created++;
            patientNotifier.notifyPatient(alert);
        }
        return created;
    }

    /**
     * De-duplication rule: an open alert (same patient and type, younger than the dedup window) blocks the new one,
     * unless the new one is CRITICAL and every existing one is only a WARNING.
     */
    public static boolean blocksNewAlert(AlertSeverity newSeverity, List<AlertSeverity> existingOpenSeverities) {
        if (existingOpenSeverities.isEmpty()) {
            return false;
        }
        return !(newSeverity == AlertSeverity.CRITICAL
                && existingOpenSeverities.stream().noneMatch(s -> s == AlertSeverity.CRITICAL));
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    // ---------------------------------------------------------------- queries

    @Transactional(readOnly = true)
    public List<AlertDto> list(AlertStatus status, Long patientId, AlertSeverity severity, boolean includeDetail) {
        Map<Long, Integer> counts = actionCounts();
        return alerts.findAllByOrderByCreatedAtDescIdDesc().stream()
                .filter(a -> status == null || a.getStatus() == status)
                .filter(a -> patientId == null || patientId.equals(a.getPatientId()))
                .filter(a -> severity == null || a.getSeverity() == severity)
                .map(a -> AlertMapper.toDto(a, counts.getOrDefault(a.getId(), 0), includeDetail))
                .toList();
    }

    @Transactional(readOnly = true)
    public SummaryDto summary(Long patientId) {
        List<Alert> all = alerts.findAllByOrderByCreatedAtDescIdDesc().stream()
                .filter(a -> patientId == null || patientId.equals(a.getPatientId()))
                .toList();
        long newCount = 0;
        long inProgress = 0;
        long resolved = 0;
        Map<AlertType, Long> byType = new EnumMap<>(AlertType.class);
        Map<AlertSeverity, Long> bySeverity = new EnumMap<>(AlertSeverity.class);
        for (Alert a : all) {
            switch (a.getStatus()) {
                case NEW -> newCount++;
                case IN_PROGRESS -> inProgress++;
                case RESOLVED -> resolved++;
            }
            byType.merge(a.getType(), 1L, Long::sum);
            bySeverity.merge(a.getSeverity(), 1L, Long::sum);
        }
        Map<String, Long> typeMap = new LinkedHashMap<>();
        for (AlertType t : AlertType.values()) {
            typeMap.put(t.name(), byType.getOrDefault(t, 0L));
        }
        Map<String, Long> severityMap = new LinkedHashMap<>();
        for (AlertSeverity s : AlertSeverity.values()) {
            severityMap.put(s.name(), bySeverity.getOrDefault(s, 0L));
        }
        return new SummaryDto(all.size(), newCount + inProgress, newCount, inProgress, resolved, typeMap, severityMap);
    }

    @Transactional(readOnly = true)
    public AlertDetailDto detail(Long id, boolean includeDetail) {
        return buildDetail(find(id), includeDetail);
    }

    // ---------------------------------------------------------------- actions

    /** Records an action of an assistant on an alert and returns the updated alert. */
    @Transactional
    public AlertDetailDto addAction(Long id, ActionRequest request, String actor, boolean includeDetail) {
        Alert alert = find(id);
        if (alert.getStatus() == AlertStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This alert is already resolved");
        }
        String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
        if (request.type() == ActionType.NOTE && note == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A note is required for the action NOTE");
        }
        actions.save(new AlertAction(alert, actor, request.type(), note));
        if (request.type() == ActionType.RESOLVED) {
            alert.setStatus(AlertStatus.RESOLVED);
        } else if (alert.getStatus() == AlertStatus.NEW) {
            alert.setStatus(AlertStatus.IN_PROGRESS);
        }
        alerts.save(alert);
        return buildDetail(alert, includeDetail);
    }

    // ---------------------------------------------------------------- helpers

    private Alert find(Long id) {
        return alerts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
    }

    private AlertDetailDto buildDetail(Alert alert, boolean includeDetail) {
        return AlertMapper.toDetailDto(alert,
                actions.findByAlertIdOrderByCreatedAtAscIdAsc(alert.getId()),
                notifications.findByAlertIdOrderByCreatedAtAscIdAsc(alert.getId()),
                includeDetail);
    }

    private Map<Long, Integer> actionCounts() {
        Map<Long, Integer> counts = new HashMap<>();
        for (Object[] row : actions.countActionsGroupedByAlert()) {
            counts.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
        }
        return counts;
    }
}
