package ma.clinic.ai.client;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/** Reads a lab result from results-service, using the doctor's own token. */
@Component
public class ResultsClient {

    private final RestClient restClient;

    public ResultsClient(RestClient.Builder loadBalancedBuilder) {
        this.restClient = loadBalancedBuilder.clone().baseUrl("http://results-service").build();
    }

    public ResultSnapshot fetch(Long labResultId, String token) {
        try {
            return restClient.get()
                    .uri("/api/results/{id}", labResultId)
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve()
                    .body(ResultSnapshot.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab result not found");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "results-service returned an error");
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "results-service is not reachable");
        }
    }

    // Only the fields ai-service needs; the rest of the answer is ignored.
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResultSnapshot(Long id, Long patientId, Long consultationId, LocalDate resultDate,
                                 String status, List<ValueSnapshot> values) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ValueSnapshot(ParameterSnapshot parameter, Double value, String flag) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ParameterSnapshot(Long id, String code, String label, String unit, Double refMin, Double refMax) {
    }
}
