package ma.clinic.results.extraction;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ma.clinic.results.model.AnalysisParameter;
import org.springframework.stereotype.Component;

/**
 * Looks in a text for the parameters of the catalogue only (glycemia, HbA1c, ...) and returns their values.
 * Everything else written on the paper is ignored.
 *
 * Rule: a parameter name (code, label or alias) followed, within 40 characters on the same line,
 * by a number. Limit: if a range is printed before the value on that line, the assistant corrects it at review. Accents and case are ignored; "1,05" and "1.05" are both accepted.
 */
@Component
public class ValueParser {

    /** @return parameter id -> value found (first occurrence), only for parameters that were found. */
    public Map<Long, Double> parse(String text, List<AnalysisParameter> catalogue) {
        String normalized = normalize(text);
        Map<Long, Double> found = new LinkedHashMap<>();
        for (AnalysisParameter parameter : catalogue) {
            Double value = findValue(normalized, namesOf(parameter));
            if (value != null) {
                found.put(parameter.getId(), value);
            }
        }
        return found;
    }

    /**
     * All the names of the parameter are first replaced by one marker, so that digits inside a name
     * (the "1" of "HbA1c") are never mistaken for the value: "hemoglobine glyquee (hba1c) : 7.8" becomes "@ (@) : 7.8".
     */
    private Double findValue(String text, List<String> names) {
        String marked = text;
        for (String name : names) { // longest first
            String nameRegex = Pattern.quote(name).replace(" ", "\\E\\s+\\Q");
            marked = marked.replaceAll("(?<![a-z0-9])" + nameRegex + "(?![a-z0-9])", "@");
        }
        Matcher matcher = Pattern.compile("@[^0-9\\n]{0,40}(\\d+(?:[.,]\\d+)?)").matcher(marked);
        return matcher.find() ? Double.parseDouble(matcher.group(1).replace(',', '.')) : null;
    }

    /** All names of a parameter, normalized, longest first (so "glycemie a jeun" is tried before "glycemie"). */
    private List<String> namesOf(AnalysisParameter parameter) {
        String aliases = parameter.getAliases() == null ? "" : parameter.getAliases();
        return Arrays.stream((parameter.getCode() + ";" + parameter.getLabel() + ";" + aliases).split(";"))
                .map(this::normalize)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
    }

    private String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ');
    }
}
