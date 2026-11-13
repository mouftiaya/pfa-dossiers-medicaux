package ma.clinic.glucose.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import ma.clinic.glucose.dto.GlucoseDtos.WindowDto;
import ma.clinic.glucose.logic.StatsCalculator.Reading;
import org.junit.jupiter.api.Test;

class WindowCalculatorTest {

    private static final LocalDate TO = LocalDate.of(2026, 11, 11);

    @Test
    void splitsInSevenDayWeeksAndCountsPercentages() {
        LocalDate from = TO.minusDays(41);
        List<Reading> readings = new ArrayList<>();
        // last week: 4 readings, 3 above the target (180), 1 in target
        readings.add(new Reading(200, TO.atTime(8, 0)));
        readings.add(new Reading(210, TO.minusDays(1).atTime(8, 0)));
        readings.add(new Reading(190, TO.minusDays(2).atTime(8, 0)));
        readings.add(new Reading(120, TO.minusDays(3).atTime(8, 0)));
        // first week: 1 reading below 70
        readings.add(new Reading(60, from.atTime(8, 0)));
        // outside the window: ignored
        readings.add(new Reading(500, from.minusDays(1).atTime(8, 0)));

        WindowDto w = WindowCalculator.compute(1L, from, TO, readings, Thresholds.DEFAULTS);

        assertEquals(42, w.days());
        assertEquals(6, w.weeks().size());
        assertEquals(5, w.count());
        assertEquals(5, w.daysWithData());
        assertEquals(4, w.weeks().get(5).count());
        assertEquals(75.0, w.weeks().get(5).percentAboveTarget());
        assertEquals(1, w.weeks().get(0).count());
        assertEquals(100.0, w.weeks().get(0).percentBelowLow());
        assertNull(w.weeks().get(2).average());
        assertEquals(60.0, w.percentAboveTarget());
    }
}
