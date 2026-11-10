package ma.clinic.results.config;

import ma.clinic.results.model.AnalysisParameter;
import ma.clinic.results.repository.AnalysisParameterRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Seeds the catalogue of analysis parameters the first time the service starts on an empty database.
 *
 * IMPORTANT: the reference ranges below are common adult values used as a starting point.
 * They must be confirmed by the doctor of the cabinet before any real use
 * (target values for a diabetic patient can be stricter than the usual laboratory ranges).
 */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedParameters(AnalysisParameterRepository parameters) {
        return args -> {
            if (parameters.count() > 0) {
                return;
            }
            // code, label, unit, refMin, refMax, aliases (used to find the value in the document text)
            parameters.save(new AnalysisParameter("GLYCEMIA", "Glycémie à jeun", "g/L", 0.70, 1.10,
                    "glycemie a jeun;glycemie veineuse;glycemie;glucose"));
            parameters.save(new AnalysisParameter("HBA1C", "HbA1c", "%", 4.0, 5.6,
                    "hba1c;hb a1c;hemoglobine glyquee;hemoglobine a1c"));
            parameters.save(new AnalysisParameter("CHOL_TOTAL", "Cholestérol total", "g/L", null, 2.00,
                    "cholesterol total;cholesterolemie totale;cholesterolemie"));
            parameters.save(new AnalysisParameter("LDL", "LDL-cholestérol", "g/L", null, 1.60,
                    "ldl cholesterol;cholesterol ldl;ldl-c;ldl"));
            parameters.save(new AnalysisParameter("TRIGLYCERIDES", "Triglycérides", "g/L", null, 1.50,
                    "triglycerides;triglyceridemie"));
            parameters.save(new AnalysisParameter("CREATININE", "Créatinine", "mg/L", 6.0, 13.0,
                    "creatinine;creatininemie"));
        };
    }
}
