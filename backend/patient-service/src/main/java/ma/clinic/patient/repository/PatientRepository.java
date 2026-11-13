package ma.clinic.patient.repository;

import java.util.List;

import ma.clinic.patient.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    @Query("""
            select p from Patient p
            where lower(p.firstName) like lower(concat('%', :q, '%'))
               or lower(p.lastName) like lower(concat('%', :q, '%'))
            order by p.lastName, p.firstName
            """)
    List<Patient> search(@Param("q") String q);
}
