package ma.clinic.results.controller;

import java.time.LocalDate;
import java.util.List;

import ma.clinic.results.dto.ResultDtos.EvolutionResponse;
import ma.clinic.results.dto.ResultDtos.ResultDto;
import ma.clinic.results.service.ResultService;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Patient portal: the PATIENT role only (see SecurityConfig). The patient file always comes from the
 * "patientId" claim of the token, never from the request, and only VALIDATED results are visible.
 */
@RestController
@RequestMapping("/api/results/me")
public class PatientResultController {

    private final ResultService service;

    public PatientResultController(ResultService service) {
        this.service = service;
    }

    @GetMapping
    public List<ResultDto> list(@AuthenticationPrincipal Jwt jwt) {
        return service.listValidatedFor(patientId(jwt));
    }

    @GetMapping("/{id}")
    public ResultDto get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.getValidatedFor(id, patientId(jwt));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        service.requireValidatedFor(id, patientId(jwt));
        return ResultController.fileResponse(service, id);
    }

    @GetMapping("/evolution")
    public EvolutionResponse evolution(@RequestParam String parameter,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                       @AuthenticationPrincipal Jwt jwt) {
        return service.evolution(patientId(jwt), parameter, from, to);
    }

    private static Long patientId(Jwt jwt) {
        Number id = jwt.getClaim("patientId");
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is not linked to a patient file");
        }
        return id.longValue();
    }
}
