package ma.clinic.results.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Tells notification-service that a validated result contains a critical value. Best effort: never throws. */
@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    /** flag is CRITICAL_LOW or CRITICAL_HIGH. */
    public record LabCriticalEvent(Long patientId, Long resultId, String parameterLabel, Double value,
                                   String unit, String flag) {
    }

    private final RestClient restClient;
    private final String internalKey;

    public NotificationClient(RestClient.Builder loadBalancedBuilder,
                              @Value("${internal.api-key:change-this-internal-key-in-production}") String internalKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);
        this.restClient = loadBalancedBuilder.clone()
                .baseUrl("http://notification-service")
                .requestFactory(factory)
                .build();
        this.internalKey = internalKey;
    }

    public void labCritical(LabCriticalEvent event) {
        try {
            restClient.post()
                    .uri("/internal/events/lab-critical")
                    .header("X-Internal-Key", internalKey)
                    .body(event)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Critical result {} could not be sent to notification-service: {}", event.resultId(), e.getMessage());
        }
    }
}
