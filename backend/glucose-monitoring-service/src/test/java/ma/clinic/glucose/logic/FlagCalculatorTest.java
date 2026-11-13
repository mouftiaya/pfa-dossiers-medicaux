package ma.clinic.glucose.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ma.clinic.glucose.model.GlucoseFlag;
import org.junit.jupiter.api.Test;

class FlagCalculatorTest {

    private static final Thresholds D = Thresholds.DEFAULTS;

    @Test
    void criticalLowIsInclusive() {
        assertEquals(GlucoseFlag.CRITICAL_LOW, FlagCalculator.compute(40, D));
        assertEquals(GlucoseFlag.CRITICAL_LOW, FlagCalculator.compute(54, D));
        assertEquals(GlucoseFlag.LOW, FlagCalculator.compute(54.1, D));
    }

    @Test
    void lowThresholdIsExclusive() {
        assertEquals(GlucoseFlag.LOW, FlagCalculator.compute(69.9, D));
        assertEquals(GlucoseFlag.IN_TARGET, FlagCalculator.compute(70, D));
    }

    @Test
    void targetBoundsAreInclusive() {
        assertEquals(GlucoseFlag.IN_TARGET, FlagCalculator.compute(100, D));
        assertEquals(GlucoseFlag.IN_TARGET, FlagCalculator.compute(180, D));
        assertEquals(GlucoseFlag.OUT_OF_TARGET, FlagCalculator.compute(180.1, D));
        assertEquals(GlucoseFlag.OUT_OF_TARGET, FlagCalculator.compute(250, D));
    }

    @Test
    void highAndCriticalHigh() {
        assertEquals(GlucoseFlag.HIGH, FlagCalculator.compute(250.1, D));
        assertEquals(GlucoseFlag.HIGH, FlagCalculator.compute(299.9, D));
        assertEquals(GlucoseFlag.CRITICAL_HIGH, FlagCalculator.compute(300, D));
        assertEquals(GlucoseFlag.CRITICAL_HIGH, FlagCalculator.compute(500, D));
    }

    @Test
    void outOfTargetBelowTargetMinWhenLowThresholdIsLower() {
        Thresholds t = new Thresholds(54, 70, 90, 180, 250, 300, 3);
        assertEquals(GlucoseFlag.OUT_OF_TARGET, FlagCalculator.compute(80, t));
        assertEquals(GlucoseFlag.IN_TARGET, FlagCalculator.compute(90, t));
    }
}
