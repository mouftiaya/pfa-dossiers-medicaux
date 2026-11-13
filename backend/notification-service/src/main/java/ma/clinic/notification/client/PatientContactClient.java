package ma.clinic.notification.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Reads the contact details of a patient from patient-service (internal endpoint, internal key). */
@Component
public class PatientContactClient {

    private static final Logger log = LoggerFactory.getLogger(PatientContactClient.class);

    private final RestClient restClient;
    private final String internalKey;

    public PatientContactClient(RestClient.Builder loadBalancedBuilder,
                                @Value("${internal.api-key:change-this-internal-key-in-production}") String internalKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.restClient = loadBalancedBuilder.clone()
                .baseUrl("http://patient-service")
                .requestFactory(factory)
                .build();
        this.internalKey = internalKey;
    }

    /** Returns the contact, or null when the patient is unknown or patient-service fails (never throws). */
    public Contact fetch(Long patientId) {
        try {
            return restClient.get()
                    .uri("/internal/patients/{id}/contact", patientId)
                    .header("X-Internal-Key", internalKey)
                    .retrieve()
                    .body(Contact.class);
        } catch (Exception e) {
            log.warn("Contact of patient {} could not be read: {}", patientId, e.getMessage());
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(String firstName, String phone, String email) {
    }
}
