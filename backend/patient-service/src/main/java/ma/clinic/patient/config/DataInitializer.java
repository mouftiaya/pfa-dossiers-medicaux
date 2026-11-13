package ma.clinic.patient.config;

import java.time.LocalDate;

import ma.clinic.patient.model.DiabetesType;
import ma.clinic.patient.model.Patient;
import ma.clinic.patient.repository.PatientRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** DEV ONLY: a few fictional patients so the list is not empty on first launch. */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedPatients(PatientRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(sample("Salma", "Benali", LocalDate.of(1985, 3, 12), "F", "0600000001", DiabetesType.TYPE_2));
                repository.save(sample("Youssef", "El Amrani", LocalDate.of(2004, 9, 30), "M", "0600000002", DiabetesType.TYPE_1));
                repository.save(sample("Khadija", "Tazi", LocalDate.of(1992, 6, 5), "F", "0600000003", DiabetesType.GESTATIONAL));
            }
        };
    }

    private Patient sample(String firstName, String lastName, LocalDate birth, String gender, String phone, DiabetesType type) {
        Patient p = new Patient();
        p.setFirstName(firstName);
        p.setLastName(lastName);
        p.setDateOfBirth(birth);
        p.setGender(gender);
        p.setPhone(phone);
        p.setDiabetesType(type);
        return p;
    }
}
