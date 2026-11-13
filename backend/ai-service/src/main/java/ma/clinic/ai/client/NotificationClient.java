package ma.clinic.ai.client;

import ma.clinic.ai.glucose.GlucoseRisk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Tells notification-service that a patient needs the assistant's attention (internal endpoint). */
@Component
public class NotificationClient {

    private final RestClient restClient;
    private final String internalKey;

    public NotificationClient(RestClient.Builder loadBalancedBuilder,
                              @Value("${internal.api-key:change-this-internal-key-in-production}") String internalKey) {
        this.restClient = loadBalancedBuilder.clone().baseUrl("http://notification-service").build();
        this.internalKey = internalKey;
    }

    /** @return the answer of the notification service ("created" or "duplicate") */
    public String sendRisk(Long patientId, GlucoseRisk risk) {
        AiRiskEvent event = new AiRiskEvent(patientId, risk.direction().name(), risk.score(), risk.windowDays(),
                risk.daysWithData(), risk.badWeeks(), risk.judgedWeeks(), risk.average(), risk.summary());
        RiskAck ack = restClient.post()
                .uri("/internal/events/ai-risk")
                .header("X-Internal-Key", internalKey)
                .body(event)
                .retrieve()
                .body(RiskAck.class);
        return ack == null || ack.result() == null ? "unknown" : ack.result();
    }

    public record AiRiskEvent(Long patientId, String direction, int score, int windowDays, int daysWithData,
                              int badWeeks, int judgedWeeks, Double average, String summary) {
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public record RiskAck(String result) {
    }
}
