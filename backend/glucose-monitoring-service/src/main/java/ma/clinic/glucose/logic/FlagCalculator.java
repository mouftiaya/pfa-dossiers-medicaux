package ma.clinic.glucose.logic;

import ma.clinic.glucose.model.GlucoseFlag;

/** Computes the flag of a glucose value against the patient's thresholds (mg/dL). */
public final class FlagCalculator {

    private FlagCalculator() {
    }

    public static GlucoseFlag compute(double value, Thresholds t) {
        if (value <= t.criticalLow()) {
            return GlucoseFlag.CRITICAL_LOW;
        }
        if (value < t.lowThreshold()) {
            return GlucoseFlag.LOW;
        }
        if (value >= t.criticalHigh()) {
            return GlucoseFlag.CRITICAL_HIGH;
        }
        if (value > t.highThreshold()) {
            return GlucoseFlag.HIGH;
        }
        if (value >= t.targetMin() && value <= t.targetMax()) {
            return GlucoseFlag.IN_TARGET;
        }
        return GlucoseFlag.OUT_OF_TARGET;
    }
}
