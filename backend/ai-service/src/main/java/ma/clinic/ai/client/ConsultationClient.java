package ma.clinic.ai.client;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/** Writes the diagnosis kept by the doctor into the consultation, using his own token. */
@Component
public class ConsultationClient {

    private final RestClient restClient;

    public ConsultationClient(RestClient.Builder loadBalancedBuilder) {
        this.restClient = loadBalancedBuilder.clone().baseUrl("http://consultation-service").build();
    }

    public void updateDiagnosis(Long consultationId, String diagnosis, String token) {
        try {
            restClient.put()
                    .uri("/api/consultations/{id}/diagnosis", consultationId)
                    .headers(h -> h.setBearerAuth(token))
                    .body(Map.of("diagnosis", diagnosis))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation not found");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "consultation-service returned an error");
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "consultation-service is not reachable");
        }
    }
}
