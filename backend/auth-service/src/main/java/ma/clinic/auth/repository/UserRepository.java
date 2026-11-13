package ma.clinic.auth.repository;

import java.util.Optional;

import ma.clinic.auth.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByPatientId(Long patientId);

    Optional<AppUser> findByActivationToken(String activationToken);
}
