package ma.clinic.patient.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import ma.clinic.patient.model.DiabetesType;

public record PatientRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull @Past LocalDate dateOfBirth,
        @Size(max = 20) String gender,
        @Size(max = 30) String phone,
        DiabetesType diabetesType,
        @Size(max = 1000) String allergies,
        @Size(max = 4000) String medicalHistory) {
}
