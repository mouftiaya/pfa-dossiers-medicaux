package ma.clinic.glucose.logic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ThresholdsTest {

    @Test
    void defaultsAreValid() {
        assertTrue(Thresholds.DEFAULTS.isValid());
        assertNull(Thresholds.DEFAULTS.validationError());
    }

    @Test
    void equalityAllowedWhereContractSaysLessOrEqual() {
        // lowThreshold == targetMin and targetMax == highThreshold are allowed
        assertTrue(new Thresholds(50, 70, 70, 180, 180, 300, 1).isValid());
    }

    @Test
    void strictInequalitiesAreEnforced() {
        assertFalse(new Thresholds(0, 70, 70, 180, 250, 300, 3).isValid());     // criticalLow must be > 0
        assertFalse(new Thresholds(70, 70, 70, 180, 250, 300, 3).isValid());    // criticalLow < lowThreshold
        assertFalse(new Thresholds(54, 70, 60, 180, 250, 300, 3).isValid());    // lowThreshold <= targetMin
        assertFalse(new Thresholds(54, 70, 180, 180, 250, 300, 3).isValid());   // targetMin < targetMax
        assertFalse(new Thresholds(54, 70, 70, 180, 170, 300, 3).isValid());    // targetMax <= highThreshold
        assertFalse(new Thresholds(54, 70, 70, 180, 300, 300, 3).isValid());    // highThreshold < criticalHigh
    }

    @Test
    void absenceDaysMustBeBetween1And30() {
        assertFalse(new Thresholds(54, 70, 70, 180, 250, 300, 0).isValid());
        assertTrue(new Thresholds(54, 70, 70, 180, 250, 300, 1).isValid());
        assertTrue(new Thresholds(54, 70, 70, 180, 250, 300, 30).isValid());
        assertFalse(new Thresholds(54, 70, 70, 180, 250, 300, 31).isValid());
        assertNotNull(new Thresholds(54, 70, 70, 180, 250, 300, 31).validationError());
    }
}
