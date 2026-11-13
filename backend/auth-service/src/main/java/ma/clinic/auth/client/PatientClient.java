package ma.clinic.auth.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/** Asks patient-service (internal endpoint, internal key) whether a patient file exists. */
@Component
public class PatientClient {

    private final RestClient restClient;
    private final String internalKey;

    public PatientClient(RestClient.Builder loadBalancedBuilder,
                         @Value("${internal.api-key:change-this-internal-key-in-production}") String internalKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.restClient = loadBalancedBuilder.clone().baseUrl("http://patient-service").requestFactory(factory).build();
        this.internalKey = internalKey;
    }

    public boolean exists(Long patientId) {
        try {
            restClient.get()
                    .uri("/internal/patients/{id}/contact", patientId)
                    .header("X-Internal-Key", internalKey)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "patient-service is not reachable");
        }
    }
}
