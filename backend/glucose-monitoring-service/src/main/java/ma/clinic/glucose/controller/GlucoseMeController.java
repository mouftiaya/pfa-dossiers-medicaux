package ma.clinic.glucose.controller;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import ma.clinic.glucose.dto.GlucoseDtos.MeasurementDto;
import ma.clinic.glucose.dto.GlucoseDtos.MeasurementRequest;
import ma.clinic.glucose.dto.GlucoseDtos.StatsDto;
import ma.clinic.glucose.dto.GlucoseDtos.TargetDto;
import ma.clinic.glucose.logic.StatsPeriod;
import ma.clinic.glucose.service.GlucoseService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Patient portal: the patientId ALWAYS comes from the JWT claim, never from the request. */
@RestController
@RequestMapping("/api/glucose/me")
public class GlucoseMeController {

    private final GlucoseService service;

    public GlucoseMeController(GlucoseService service) {
        this.service = service;
    }

    @GetMapping("/measurements")
    public List<MeasurementDto> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.list(patientId(jwt), from, to);
    }

    @PostMapping("/measurements")
    @ResponseStatus(HttpStatus.CREATED)
    public MeasurementDto create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MeasurementRequest request) {
        return service.create(patientId(jwt), request);
    }

    @PutMapping("/measurements/{id}")
    public MeasurementDto update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                 @Valid @RequestBody MeasurementRequest request) {
        return service.update(patientId(jwt), id, request);
    }

    @DeleteMapping("/measurements/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(patientId(jwt), id);
    }

    @GetMapping("/stats")
    public StatsDto stats(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "period") StatsPeriod period,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.stats(patientId(jwt), period, date);
    }

    @GetMapping("/target")
    public TargetDto target(@AuthenticationPrincipal Jwt jwt) {
        return service.getTarget(patientId(jwt));
    }

    private Long patientId(Jwt jwt) {
        Number n = jwt.getClaim("patientId");
        if (n == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token has no patientId");
        }
        return n.longValue();
    }
}
