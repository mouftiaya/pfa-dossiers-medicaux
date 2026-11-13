package ma.clinic.glucose;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Self-monitoring of blood glucose. All values are expressed in mg/dL. */
@SpringBootApplication
@EnableScheduling
public class GlucoseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GlucoseServiceApplication.class, args);
    }
}
