package ma.clinic.auth.config;

import ma.clinic.auth.model.AppUser;
import ma.clinic.auth.model.Role;
import ma.clinic.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * DEV ONLY: creates demo accounts the first time the app starts on an empty database.
 * Remove or replace with a real user-management feature before any real use.
 */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedUsers(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (users.count() == 0) {
                users.save(new AppUser("doctor", encoder.encode("doctor123"), Role.DOCTOR));
                users.save(new AppUser("assistant", encoder.encode("assistant123"), Role.ASSISTANT));
                users.save(new AppUser("admin", encoder.encode("admin123"), Role.ADMIN));
            }
        };
    }
}
