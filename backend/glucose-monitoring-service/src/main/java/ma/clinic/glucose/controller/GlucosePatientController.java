package ma.clinic.glucose.controller;

import jakarta.validation.Valid;
import ma.clinic.glucose.dto.GlucoseDtos.SummaryDto;
import ma.clinic.glucose.dto.GlucoseDtos.TargetDto;
import ma.clinic.glucose.dto.GlucoseDtos.TargetRequest;
import ma.clinic.glucose.service.GlucoseService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Staff side: targets (doctor + assistant read, doctor write) and the aggregated summary (doctor). */
@RestController
@RequestMapping("/api/glucose/patients/{patientId}")
public class GlucosePatientController {

    private final GlucoseService service;

    public GlucosePatientController(GlucoseService service) {
        this.service = service;
    }

    @GetMapping("/target")
    public TargetDto getTarget(@PathVariable Long patientId) {
        return service.getTarget(patientId);
    }

    @PutMapping("/target")
    public TargetDto putTarget(@AuthenticationPrincipal Jwt jwt, @PathVariable Long patientId,
                               @Valid @RequestBody TargetRequest request) {
        return service.putTarget(patientId, request, jwt.getSubject());
    }

    @GetMapping("/summary")
    public SummaryDto summary(@PathVariable Long patientId) {
        return service.summary(patientId);
    }
}
