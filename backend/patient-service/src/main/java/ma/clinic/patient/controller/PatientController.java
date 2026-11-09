package ma.clinic.patient.controller;

import java.util.List;

import jakarta.validation.Valid;
import ma.clinic.patient.dto.PatientRequest;
import ma.clinic.patient.model.Patient;
import ma.clinic.patient.repository.PatientRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientRepository patients;

    public PatientController(PatientRepository patients) {
        this.patients = patients;
    }

    @GetMapping
    public List<Patient> list(@RequestParam(required = false) String search) {
        if (search == null || search.isBlank()) {
            return patients.findAll(Sort.by("lastName", "firstName"));
        }
        return patients.search(search.trim());
    }

    @GetMapping("/{id}")
    public Patient get(@PathVariable Long id) {
        return find(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Patient create(@Valid @RequestBody PatientRequest request) {
        Patient patient = new Patient();
        apply(patient, request);
        return patients.save(patient);
    }

    @PutMapping("/{id}")
    public Patient update(@PathVariable Long id, @Valid @RequestBody PatientRequest request) {
        Patient patient = find(id);
        apply(patient, request);
        return patients.save(patient);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        patients.delete(find(id));
    }

    private Patient find(Long id) {
        return patients.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
    }

    private void apply(Patient patient, PatientRequest request) {
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setGender(request.gender());
        patient.setPhone(request.phone());
        patient.setDiabetesType(request.diabetesType());
        patient.setAllergies(request.allergies());
        patient.setMedicalHistory(request.medicalHistory());
    }
}
