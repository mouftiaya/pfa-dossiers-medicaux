package ma.clinic.results.config;

import ma.clinic.results.model.AnalysisParameter;
import ma.clinic.results.repository.AnalysisParameterRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Makes sure the catalogue of analysis parameters is complete (diabetology + cardiology) at every start:
 * a missing parameter is created, and an existing one that has no limits yet (database created by the
 * first version of the project) receives them. Reference ranges already stored are never overwritten.
 *
 * IMPORTANT: all the values below are common adult values used as a starting point.
 * They MUST be confirmed by the doctor of the cabinet before any real use:
 *  - ref range      = usual laboratory range (a diabetic patient's target can be stricter);
 *  - valid range    = physiologically possible values; outside it the value is INVALID (typing/extraction error);
 *  - critical limit = dangerous values; the doctor is alerted when a result containing one is validated.
 * Troponin and NT-proBNP depend on the laboratory's assay and unit: adapt them to the laboratory used.
 */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedParameters(AnalysisParameterRepository parameters) {
        return args -> {
            // code, label, unit, refMin, refMax, aliases (to find the value in the text), validMin, validMax, criticalLow, criticalHigh
            ensure(parameters, new AnalysisParameter("GLYCEMIA", "Glycémie à jeun", "g/L", 0.70, 1.10,
                    "glycemie a jeun;glycemie veineuse;glycemie;glucose", 0.10, 10.0, 0.50, 3.00));
            ensure(parameters, new AnalysisParameter("HBA1C", "HbA1c", "%", 4.0, 5.6,
                    "hba1c;hb a1c;hemoglobine glyquee;hemoglobine a1c", 2.0, 20.0, null, 14.0));
            ensure(parameters, new AnalysisParameter("CHOL_TOTAL", "Cholestérol total", "g/L", null, 2.00,
                    "cholesterol total;cholesterolemie totale;cholesterolemie", 0.30, 10.0, null, null));
            ensure(parameters, new AnalysisParameter("LDL", "LDL-cholestérol", "g/L", null, 1.60,
                    "ldl cholesterol;cholesterol ldl;ldl-c;ldl", 0.05, 8.0, null, null));
            ensure(parameters, new AnalysisParameter("TRIGLYCERIDES", "Triglycérides", "g/L", null, 1.50,
                    "triglycerides;triglyceridemie", 0.10, 30.0, null, 5.0));
            ensure(parameters, new AnalysisParameter("CREATININE", "Créatinine", "mg/L", 6.0, 13.0,
                    "creatinine;creatininemie", 1.0, 150.0, null, 50.0));
            // Cardiology
            ensure(parameters, new AnalysisParameter("HDL", "HDL-cholestérol", "g/L", 0.40, null,
                    "hdl cholesterol;cholesterol hdl;hdl-c;hdl", 0.05, 3.0, null, null));
            ensure(parameters, new AnalysisParameter("POTASSIUM", "Potassium", "mmol/L", 3.5, 5.0,
                    "potassium;kaliemie;potassemie", 1.5, 10.0, 2.5, 6.5));
            ensure(parameters, new AnalysisParameter("SODIUM", "Sodium", "mmol/L", 135.0, 145.0,
                    "sodium;natremie", 100.0, 180.0, 120.0, 160.0));
            ensure(parameters, new AnalysisParameter("UREA", "Urée", "g/L", 0.15, 0.45,
                    "uree;uremie;urea", 0.02, 5.0, null, null));
            ensure(parameters, new AnalysisParameter("TROPONIN", "Troponine", "ng/L", null, 14.0,
                    "troponine us;troponine;troponin;hs troponin", 0.0, 100000.0, null, 100.0));
            ensure(parameters, new AnalysisParameter("NT_PROBNP", "NT-proBNP", "pg/mL", null, 125.0,
                    "nt-probnp;nt probnp;nt pro bnp;probnp", 0.0, 70000.0, null, 5000.0));
        };
    }

    private void ensure(AnalysisParameterRepository parameters, AnalysisParameter wanted) {
        AnalysisParameter existing = parameters.findByCode(wanted.getCode()).orElse(null);
        if (existing == null) {
            parameters.save(wanted);
        } else if (existing.getValidMin() == null && existing.getValidMax() == null
                && existing.getCriticalLow() == null && existing.getCriticalHigh() == null) {
            existing.setLimits(wanted.getValidMin(), wanted.getValidMax(),
                    wanted.getCriticalLow(), wanted.getCriticalHigh());
            parameters.save(existing);
        }
    }
}
