package ma.clinic.ai.controller;

import java.util.List;

import jakarta.validation.Valid;
import ma.clinic.ai.dto.AiDtos.AnalysisRequest;
import ma.clinic.ai.dto.AiDtos.AnalysisResponse;
import ma.clinic.ai.dto.AiDtos.DecisionRequest;
import ma.clinic.ai.model.AiSuggestion;
import ma.clinic.ai.service.AiService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService service;

    public AiController(AiService service) {
        this.service = service;
    }

    /** Asks for the analysis of a validated lab result: abnormal values + diagnostic suggestion. */
    @PostMapping("/analyses")
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisResponse analyse(@Valid @RequestBody AnalysisRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.analyse(request, jwt.getTokenValue(), jwt.getSubject());
    }

    /** Suggestions of a patient (patientId) or of one lab result (labResultId). */
    @GetMapping("/suggestions")
    public List<AiSuggestion> suggestions(@RequestParam(required = false) Long patientId,
                                          @RequestParam(required = false) Long labResultId) {
        if (patientId != null && labResultId == null) {
            return service.forPatient(patientId);
        }
        if (labResultId != null && patientId == null) {
            return service.forResult(labResultId);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Give either patientId or labResultId");
    }

    /** The doctor approves, modifies or rejects a suggestion. */
    @PutMapping("/suggestions/{id}/decision")
    public AiSuggestion decide(@PathVariable Long id, @Valid @RequestBody DecisionRequest request,
                               @AuthenticationPrincipal Jwt jwt) {
        return service.decide(id, request, jwt.getTokenValue(), jwt.getSubject());
    }
}
