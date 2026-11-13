package ma.clinic.glucose.logic;

/** Targets and thresholds of a patient, in mg/dL. Plain value object, no side effects. */
public record Thresholds(
        double criticalLow,
        double lowThreshold,
        double targetMin,
        double targetMax,
        double highThreshold,
        double criticalHigh,
        int absenceDays) {

    public static final Thresholds DEFAULTS = new Thresholds(54, 70, 70, 180, 250, 300, 3);

    /**
     * Returns null when the values are valid, otherwise a message describing the problem.
     * Valid means 0 &lt; criticalLow &lt; lowThreshold &lt;= targetMin &lt; targetMax &lt;= highThreshold &lt; criticalHigh
     * and absenceDays in 1..30.
     */
    public String validationError() {
        boolean ordered = 0 < criticalLow
                && criticalLow < lowThreshold
                && lowThreshold <= targetMin
                && targetMin < targetMax
                && targetMax <= highThreshold
                && highThreshold < criticalHigh;
        if (!ordered) {
            return "Thresholds must satisfy 0 < criticalLow < lowThreshold <= targetMin < targetMax <= highThreshold < criticalHigh";
        }
        if (absenceDays < 1 || absenceDays > 30) {
            return "absenceDays must be between 1 and 30";
        }
        return null;
    }

    public boolean isValid() {
        return validationError() == null;
    }
}
