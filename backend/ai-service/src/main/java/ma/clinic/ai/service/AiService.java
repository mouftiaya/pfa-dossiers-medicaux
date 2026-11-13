package ma.clinic.ai.service;

import java.util.List;

import ma.clinic.ai.client.ConsultationClient;
import ma.clinic.ai.client.ResultsClient;
import ma.clinic.ai.client.ResultsClient.ResultSnapshot;
import ma.clinic.ai.dto.AiDtos.AbnormalValue;
import ma.clinic.ai.dto.AiDtos.AnalysisRequest;
import ma.clinic.ai.dto.AiDtos.AnalysisResponse;
import ma.clinic.ai.dto.AiDtos.DecisionRequest;
import ma.clinic.ai.engine.DiagnosisEngine;
import ma.clinic.ai.engine.DiagnosisEngine.Analysis;
import ma.clinic.ai.model.AiSuggestion;
import ma.clinic.ai.model.SuggestionStatus;
import ma.clinic.ai.repository.AiSuggestionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AiService {

    private final AiSuggestionRepository suggestions;
    private final ResultsClient resultsClient;
    private final ConsultationClient consultationClient;
    private final DiagnosisEngine engine;

    public AiService(AiSuggestionRepository suggestions, ResultsClient resultsClient,
                     ConsultationClient consultationClient, DiagnosisEngine engine) {
        this.suggestions = suggestions;
        this.resultsClient = resultsClient;
        this.consultationClient = consultationClient;
        this.engine = engine;
    }

    /** Detects the abnormal values of a VALIDATED result and stores a suggestion waiting for the doctor. */
    @Transactional
    public AnalysisResponse analyse(AnalysisRequest request, String token, String doctor) {
        ResultSnapshot result = resultsClient.fetch(request.labResultId(), token);
        if (!"VALIDATED".equals(result.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The result must be validated by the doctor before the AI analysis");
        }
        Analysis analysis = engine.analyse(result.values());
        List<AbnormalValue> abnormal = result.values().stream()
                .filter(v -> !"NORMAL".equals(v.flag()))
                .map(v -> new AbnormalValue(v.parameter().code(), v.parameter().label(), v.value(),
                        v.parameter().unit(), v.flag()))
                .toList();

        Long consultationId = request.consultationId() != null ? request.consultationId() : result.consultationId();
        AiSuggestion saved = suggestions.save(new AiSuggestion(result.patientId(), result.id(), consultationId,
                analysis.text(), analysis.confidence(), doctor));
        return new AnalysisResponse(saved.getId(), saved.getLabResultId(), abnormal,
                saved.getSuggestedDiagnosis(), saved.getConfidence(), saved.getStatus());
    }

    @Transactional(readOnly = true)
    public List<AiSuggestion> forPatient(Long patientId) {
        return suggestions.findByPatientIdOrderByCreatedAtDescIdDesc(patientId);
    }

    @Transactional(readOnly = true)
    public List<AiSuggestion> forResult(Long labResultId) {
        return suggestions.findByLabResultIdOrderByCreatedAtDescIdDesc(labResultId);
    }

    /** The doctor approves, modifies or rejects a pending suggestion. His decision is final. */
    @Transactional
    public AiSuggestion decide(Long id, DecisionRequest request, String token, String doctor) {
        AiSuggestion suggestion = suggestions.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Suggestion not found"));
        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A decision was already recorded");
        }

        switch (request.decision()) {
            case REJECT -> suggestion.decide(SuggestionStatus.REJECTED, null, doctor);
            case APPROVE -> {
                keepInConsultation(suggestion, suggestion.getSuggestedDiagnosis(), token);
                suggestion.decide(SuggestionStatus.APPROVED, suggestion.getSuggestedDiagnosis(), doctor);
            }
            case MODIFY -> {
                String text = request.finalDiagnosis();
                if (text == null || text.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "finalDiagnosis is required to modify a suggestion");
                }
                keepInConsultation(suggestion, text.trim(), token);
                suggestion.decide(SuggestionStatus.MODIFIED, text.trim(), doctor);
            }
        }
        return suggestions.save(suggestion);
    }

    /** Writes the kept diagnosis into the consultation (if the result is linked to one). */
    private void keepInConsultation(AiSuggestion suggestion, String diagnosis, String token) {
        if (suggestion.getConsultationId() != null) {
            consultationClient.updateDiagnosis(suggestion.getConsultationId(), diagnosis, token);
        }
    }
}
