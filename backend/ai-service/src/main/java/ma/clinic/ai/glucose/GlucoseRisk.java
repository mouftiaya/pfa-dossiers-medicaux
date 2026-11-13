package ma.clinic.ai.glucose;

/** Outcome of the trend analysis for one patient that needs the clinic's attention. */
public record GlucoseRisk(
        Direction direction,
        int score,
        int windowDays,
        int daysWithData,
        int badWeeks,
        int judgedWeeks,
        Double average,
        String summary) {

    public enum Direction {
        /** Glucose stayed above the target for most of the period. */
        HIGH,
        /** Glucose kept dropping below the low threshold (hypoglycaemia) for most of the period. */
        LOW
    }
}
