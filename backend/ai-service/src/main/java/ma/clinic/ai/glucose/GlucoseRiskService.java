package ma.clinic.ai.glucose;

import java.util.ArrayList;
import java.util.List;

import ma.clinic.ai.client.GlucoseClient;
import ma.clinic.ai.client.NotificationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Goes through the patients who have glucose measurements, looks for a persistent trend over several weeks and,
 * when it finds one, tells notification-service so that the assistant is alerted. A failure for one patient never
 * stops the others.
 */
@Service
public class GlucoseRiskService {

    private static final Logger log = LoggerFactory.getLogger(GlucoseRiskService.class);

    private final GlucoseClient glucose;
    private final NotificationClient notifications;
    private final GlucoseRiskProperties properties;

    public GlucoseRiskService(GlucoseClient glucose, NotificationClient notifications, GlucoseRiskProperties properties) {
        this.glucose = glucose;
        this.notifications = notifications;
        this.properties = properties;
    }

    /** Scheduled run (cron in ai.glucose-risk.cron, default every day at 02:00). */
    @Scheduled(cron = "${ai.glucose-risk.cron:0 0 2 * * *}")
    public void scheduledRun() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            RunReport report = run();
            log.info("Glucose trend analysis: {} patients analysed, {} at risk, {} alerts created, {} errors",
                    report.analysed(), report.atRisk(), report.alertsCreated(), report.errors());
        } catch (RuntimeException e) {
            log.warn("Glucose trend analysis failed: {}", e.getMessage());
        }
    }

    /** Runs the analysis now (also used by the manual button). */
    public RunReport run() {
        List<Long> ids = glucose.patientIds();
        int analysed = 0;
        int atRisk = 0;
        int created = 0;
        int errors = 0;
        List<Finding> findings = new ArrayList<>();
        for (Long id : ids) {
            try {
                GlucoseWindow window = glucose.window(id, properties.getWindowDays());
                analysed++;
                GlucoseRisk risk = GlucoseRiskAnalyzer.analyze(window, properties);
                if (risk == null) {
                    continue;
                }
                atRisk++;
                String result = notifications.sendRisk(id, risk);
                if ("created".equals(result)) {
                    created++;
                }
                findings.add(new Finding(id, risk.direction().name(), risk.score(), result));
            } catch (RuntimeException e) {
                errors++;
                log.warn("Glucose trend analysis failed for patient {}: {}", id, e.getMessage());
            }
        }
        return new RunReport(ids.size(), analysed, atRisk, created, errors, findings);
    }

    public record Finding(Long patientId, String direction, int score, String result) {
    }

    public record RunReport(int patients, int analysed, int atRisk, int alertsCreated, int errors, List<Finding> findings) {
    }
}
