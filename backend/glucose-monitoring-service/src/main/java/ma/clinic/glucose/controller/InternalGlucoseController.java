package ma.clinic.glucose.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import ma.clinic.glucose.dto.GlucoseDtos.WindowDto;
import ma.clinic.glucose.service.GlucoseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Read access for the AI service only (not routed by the gateway, protected by the X-Internal-Key header).
 * It returns aggregated figures, never individual measurements.
 */
@RestController
@RequestMapping("/internal/glucose")
public class InternalGlucoseController {

    private final GlucoseService service;
    private final String internalKey;

    public InternalGlucoseController(GlucoseService service,
                                     @Value("${internal.api-key:change-this-internal-key-in-production}") String internalKey) {
        this.service = service;
        this.internalKey = internalKey;
    }

    /** Ids of the patients who have at least one measurement. */
    @GetMapping("/patients")
    public List<Long> patients(@RequestHeader(value = "X-Internal-Key", required = false) String key) {
        checkKey(key);
        return service.patientIdsWithData();
    }

    @GetMapping("/patients/{patientId}/window")
    public WindowDto window(@PathVariable Long patientId,
                            @RequestParam(defaultValue = "42") int days,
                            @RequestHeader(value = "X-Internal-Key", required = false) String key) {
        checkKey(key);
        if (days < 7 || days > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "days must be between 7 and 180");
        }
        return service.window(patientId, days);
    }

    private void checkKey(String provided) {
        boolean ok = provided != null && MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8), internalKey.getBytes(StandardCharsets.UTF_8));
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal key");
        }
    }
}
