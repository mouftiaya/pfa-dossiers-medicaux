package ma.clinic.glucose.logic;

import java.time.LocalDateTime;

import ma.clinic.glucose.model.MeasureContext;

/** Validation rules of a measurement, without side effects. */
public final class MeasurementRules {

    public static final double MIN_VALUE = 20;
    public static final double MAX_VALUE = 800;
    public static final int FUTURE_TOLERANCE_MINUTES = 5;

    private MeasurementRules() {
    }

    /** Returns null when valid, otherwise the error message. */
    public static String validationError(Double value, LocalDateTime measuredAt, MeasureContext context,
                                         LocalDateTime now) {
        if (value == null || value.isNaN() || value < MIN_VALUE || value > MAX_VALUE) {
            return "value must be between 20 and 800 mg/dL";
        }
        if (measuredAt == null) {
            return "measuredAt is required";
        }
        if (measuredAt.isAfter(now.plusMinutes(FUTURE_TOLERANCE_MINUTES))) {
            return "measuredAt cannot be in the future";
        }
        if (context == null) {
            return "context is required";
        }
        return null;
    }
}
