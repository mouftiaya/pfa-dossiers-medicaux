package ma.clinic.glucose.client;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import ma.clinic.glucose.logic.Thresholds;
import ma.clinic.glucose.model.GlucoseMeasurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Best-effort, synchronous calls to notification-service (internal key, 2 s timeouts).
 * Every exception is logged and swallowed: a notification problem never fails a request or the job.
 */
@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);
    private static final int TIMEOUT_MS = 2000;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final RestClient restClient;
    private final String internalKey;

    public NotificationClient(RestClient.Builder loadBalancedBuilder,
                              @Value("${internal.api-key}") String internalKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);
        this.restClient = loadBalancedBuilder.clone()
                .baseUrl("http://notification-service")
                .requestFactory(factory)
                .build();
        this.internalKey = internalKey;
    }

    /**
     * Called after a measurement has been CREATED.
     *
     * @param priorNewestFirst up to 4 measurements strictly before the new one, newest first
     */
    public void notifyMeasurementCreated(GlucoseMeasurement current, Thresholds t,
                                         List<GlucoseMeasurement> priorNewestFirst) {
        try {
            Point previous = priorNewestFirst.isEmpty() ? null : point(priorNewestFirst.get(0));
            List<Point> recent = new ArrayList<>();
            for (int i = priorNewestFirst.size() - 1; i >= 0; i--) {
                recent.add(point(priorNewestFirst.get(i)));
            }
            recent.add(point(current));
            // Keep only the last 5, oldest first.
            if (recent.size() > 5) {
                recent = new ArrayList<>(recent.subList(recent.size() - 5, recent.size()));
            }
            ThresholdsPayload thresholds = new ThresholdsPayload(t.criticalLow(), t.lowThreshold(),
                    t.targetMin(), t.targetMax(), t.highThreshold(), t.criticalHigh());
            GlucoseEvent event = new GlucoseEvent(current.getPatientId(), current.getValue(),
                    format(current.getMeasuredAt()), current.getContext().name(), thresholds, previous, recent);
            post("/internal/events/glucose", event);
        } catch (Exception e) {
            log.warn("Could not notify notification-service of a new measurement: {}", e.toString());
        }
    }

    public void notifyAbsence(Long patientId, LocalDateTime lastMeasurementAt, long daysWithoutMeasurement,
                              int absenceDays) {
        try {
            AbsenceEvent event = new AbsenceEvent(patientId,
                    lastMeasurementAt == null ? null : format(lastMeasurementAt),
                    daysWithoutMeasurement, absenceDays);
            post("/internal/events/absence", event);
        } catch (Exception e) {
            log.warn("Could not notify notification-service of an absence (patient {}): {}", patientId,
                    e.toString());
        }
    }

    private void post(String uri, Object body) {
        restClient.post()
                .uri(uri)
                .header("X-Internal-Key", internalKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private static Point point(GlucoseMeasurement m) {
        return new Point(m.getValue(), format(m.getMeasuredAt()));
    }

    /** ISO local date-time without zone, e.g. 2026-11-10T08:30:00 (strings avoid any Jackson date setting). */
    private static String format(LocalDateTime dateTime) {
        return dateTime.truncatedTo(ChronoUnit.SECONDS).format(ISO);
    }

    public record Point(double value, String measuredAt) {
    }

    public record ThresholdsPayload(double criticalLow, double lowThreshold, double targetMin, double targetMax,
                                    double highThreshold, double criticalHigh) {
    }

    public record GlucoseEvent(Long patientId, double value, String measuredAt, String context,
                               ThresholdsPayload thresholds, Point previous, List<Point> recent) {
    }

    public record AbsenceEvent(Long patientId, String lastMeasurementAt, long daysWithoutMeasurement,
                               int absenceDays) {
    }
}
