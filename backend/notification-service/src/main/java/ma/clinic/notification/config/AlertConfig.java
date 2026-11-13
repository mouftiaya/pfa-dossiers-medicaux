package ma.clinic.notification.config;

import ma.clinic.notification.detection.AlertDetector;
import ma.clinic.notification.detection.AlertRules;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlertConfig {

    @Bean
    AlertDetector alertDetector(AlertRules rules) {
        return new AlertDetector(rules);
    }
}
