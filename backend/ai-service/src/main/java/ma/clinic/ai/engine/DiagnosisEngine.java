package ma.clinic.ai.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ma.clinic.ai.client.ResultsClient.ValueSnapshot;
import org.springframework.stereotype.Component;

/**
 * Rule-based interpretation of the validated values (glycemia, HbA1c, lipids, creatinine).
 *
 * This is deliberately simple and explainable: each sentence of the suggestion comes from one visible rule.
 * It is a help to interpretation only; the medical decision belongs to the doctor.
 * The thresholds are common adult reference values and must be confirmed by the doctor of the cabinet.
 * To use a real model later, replace this class: the rest of the service only needs analyse(...).
 */
@Component
public class DiagnosisEngine {

    public record Analysis(String text, double confidence) {
    }

    public Analysis analyse(List<ValueSnapshot> values) {
        Map<String, ValueSnapshot> byCode = new HashMap<>();
        for (ValueSnapshot v : values) {
            if ("INVALID".equals(v.flag())) {
                continue; // an impossible value is never interpreted (such a result cannot be validated anyway)
            }
            byCode.put(v.parameter().code(), v);
        }
        List<String> findings = new ArrayList<>();

        Double hba1c = valueOf(byCode, "HBA1C");
        Double glycemia = valueOf(byCode, "GLYCEMIA");

        if (hba1c != null) {
            if (hba1c <= 7.0) {
                findings.add(String.format(Locale.FRANCE,
                        "HbA1c à %.1f %% : équilibre glycémique satisfaisant (objectif habituel ≤ 7 %%).", hba1c));
            } else if (hba1c <= 8.5) {
                findings.add(String.format(Locale.FRANCE,
                        "HbA1c à %.1f %% : équilibre glycémique insuffisant, adaptation du traitement à envisager.",
                        hba1c));
            } else {
                findings.add(String.format(Locale.FRANCE,
                        "HbA1c à %.1f %% : diabète mal équilibré, révision du traitement à discuter.", hba1c));
            }
        }
        if (glycemia != null) {
            if (glycemia < 0.70) {
                findings.add(String.format(Locale.FRANCE,
                        "Glycémie à %.2f g/L : hypoglycémie, rechercher une cause (traitement, repas, effort).", glycemia));
            } else if (glycemia <= 1.10) {
                findings.add(String.format(Locale.FRANCE, "Glycémie à jeun à %.2f g/L : dans la norme.", glycemia));
            } else if (glycemia < 1.26) {
                findings.add(String.format(Locale.FRANCE,
                        "Glycémie à jeun à %.2f g/L : hyperglycémie modérée.", glycemia));
            } else {
                findings.add(String.format(Locale.FRANCE,
                        "Glycémie à jeun à %.2f g/L : hyperglycémie marquée (≥ 1,26 g/L).", glycemia));
            }
        }
        if (hba1c != null && glycemia != null && hba1c > 8.5 && glycemia >= 1.26) {
            findings.add("HbA1c et glycémie élevées ensemble : tableau cohérent avec un diabète déséquilibré.");
        }
        if (isHigh(byCode, "LDL")) {
            findings.add("LDL-cholestérol élevé : risque cardiovasculaire à évaluer.");
        }
        if (isHigh(byCode, "TRIGLYCERIDES")) {
            findings.add("Triglycérides élevés : dyslipidémie à prendre en compte.");
        }
        if (isHigh(byCode, "CHOL_TOTAL")) {
            findings.add("Cholestérol total élevé : bilan lipidique à compléter.");
        }
        if (isHigh(byCode, "CREATININE")) {
            findings.add("Créatinine élevée : fonction rénale à explorer (atteinte rénale diabétique possible).");
        }

        if (findings.isEmpty()) {
            return new Analysis("Aucune suggestion : les paramètres disponibles ne permettent pas d'interprétation.",
                    0.0);
        }
        // Share of the key parameters present: HbA1c (50 %), glycemia (30 %), any other parameter (20 %)
        double confidence = (hba1c != null ? 0.5 : 0.0)
                + (glycemia != null ? 0.3 : 0.0)
                + (byCode.size() > (hba1c != null ? 1 : 0) + (glycemia != null ? 1 : 0) ? 0.2 : 0.0);
        return new Analysis(String.join("\n", findings), confidence);
    }

    private Double valueOf(Map<String, ValueSnapshot> byCode, String code) {
        ValueSnapshot v = byCode.get(code);
        return v == null ? null : v.value();
    }

    private boolean isHigh(Map<String, ValueSnapshot> byCode, String code) {
        ValueSnapshot v = byCode.get(code);
        return v != null && ("HIGH".equals(v.flag()) || "CRITICAL_HIGH".equals(v.flag()));
    }
}
