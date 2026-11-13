package ma.clinic.consultation.repository;

import java.util.List;

import ma.clinic.consultation.model.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    List<Consultation> findByPatientIdOrderByConsultationDateDescIdDesc(Long patientId);
}
