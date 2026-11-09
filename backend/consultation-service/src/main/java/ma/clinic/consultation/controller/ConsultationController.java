package ma.clinic.consultation.controller;

import java.util.List;

import jakarta.validation.Valid;
import ma.clinic.consultation.dto.ConsultationRequest;
import ma.clinic.consultation.model.Consultation;
import ma.clinic.consultation.repository.ConsultationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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

@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {

    private final ConsultationRepository consultations;

    public ConsultationController(ConsultationRepository consultations) {
        this.consultations = consultations;
    }

    /** Visit history of one patient, most recent first. */
    @GetMapping
    public List<Consultation> listForPatient(@RequestParam Long patientId) {
        return consultations.findByPatientIdOrderByConsultationDateDescIdDesc(patientId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Consultation create(@Valid @RequestBody ConsultationRequest request, Authentication authentication) {
        Consultation consultation = new Consultation();
        consultation.setPatientId(request.patientId());
        consultation.setCreatedBy(authentication.getName()); // username from the JWT
        apply(consultation, request);
        return consultations.save(consultation);
    }

    @PutMapping("/{id}")
    public Consultation update(@PathVariable Long id, @Valid @RequestBody ConsultationRequest request) {
        Consultation consultation = find(id);
        apply(consultation, request);
        return consultations.save(consultation);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        consultations.delete(find(id));
    }

    private Consultation find(Long id) {
        return consultations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation not found"));
    }

    private void apply(Consultation consultation, ConsultationRequest request) {
        consultation.setConsultationDate(request.consultationDate());
        consultation.setReason(request.reason());
        consultation.setNotes(request.notes());
        consultation.setPrescribedAnalyses(request.prescribedAnalyses());
        consultation.setDiagnosis(request.diagnosis());
    }
}
