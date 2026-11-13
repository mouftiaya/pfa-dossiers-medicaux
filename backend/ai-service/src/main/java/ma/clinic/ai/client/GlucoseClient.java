package ma.clinic.ai.client;

import java.util.List;

import ma.clinic.ai.glucose.GlucoseWindow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Reads aggregated glucose history from glucose-monitoring-service (internal endpoints, X-Internal-Key). */
@Component
public class GlucoseClient {

    private final RestClient restClient;
    private final String internalKey;

    public GlucoseClient(RestClient.Builder loadBalancedBuilder,
                         @Value("${internal.api-key:change-this-internal-key-in-production}") String internalKey) {
        this.restClient = loadBalancedBuilder.clone().baseUrl("http://glucose-monitoring-service").build();
        this.internalKey = internalKey;
    }

    public List<Long> patientIds() {
        Long[] ids = restClient.get()
                .uri("/internal/glucose/patients")
                .header("X-Internal-Key", internalKey)
                .retrieve()
                .body(Long[].class);
        return ids == null ? List.of() : List.of(ids);
    }

    public GlucoseWindow window(Long patientId, int days) {
        return restClient.get()
                .uri("/internal/glucose/patients/{id}/window?days={days}", patientId, days)
                .header("X-Internal-Key", internalKey)
                .retrieve()
                .body(GlucoseWindow.class);
    }
}
