package ma.clinic.glucose.controller;

import ma.clinic.glucose.dto.GlucoseDtos.DemoResultDto;
import ma.clinic.glucose.logic.DemoHistoryGenerator.Profile;
import ma.clinic.glucose.service.GlucoseService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Test helper (patient portal, PATIENT role): fills the caller's own history with generated measurements.
 * Disabled with glucose.demo.enabled=false.
 */
@RestController
@RequestMapping("/api/glucose/me")
@ConditionalOnProperty(name = "glucose.demo.enabled", havingValue = "true", matchIfMissing = true)
public class GlucoseDemoController {

    private final GlucoseService service;

    public GlucoseDemoController(GlucoseService service) {
        this.service = service;
    }

    /** profile: STABLE, HIGH or LOW. Replaces the measurements of the last {@code days} days. */
    @PostMapping("/demo-history")
    public DemoResultDto generate(@AuthenticationPrincipal Jwt jwt,
                                  @RequestParam(defaultValue = "HIGH") Profile profile,
                                  @RequestParam(defaultValue = "60") int days) {
        Number id = jwt.getClaim("patientId");
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is not linked to a patient file");
        }
        if (days < 7 || days > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "days must be between 7 and 120");
        }
        return service.generateDemoHistory(id.longValue(), profile, days);
    }
}
