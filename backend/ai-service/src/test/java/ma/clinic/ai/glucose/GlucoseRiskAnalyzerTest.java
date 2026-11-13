package ma.clinic.ai.glucose;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import ma.clinic.ai.glucose.GlucoseRisk.Direction;
import ma.clinic.ai.glucose.GlucoseWindow.Week;
import org.junit.jupiter.api.Test;

class GlucoseRiskAnalyzerTest {

    private final GlucoseRiskProperties props = new GlucoseRiskProperties();

    /** Six weeks of 21 readings; above/low = percentages applied to every week. */
    private GlucoseWindow window(double above, double low, int weeks, int daysWithData) {
        List<Week> list = new ArrayList<>();
        for (int i = 0; i < weeks; i++) {
            list.add(new Week(LocalDate.of(2026, 10, 1).plusDays(7L * i), 21, 150.0, above, low));
        }
        return new GlucoseWindow(1L, 42, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 11),
                21 * weeks, daysWithData, 150.0, 100 - above - low, above, low, 0, 0, list);
    }

    @Test
    void persistentlyHighIsReported() {
        GlucoseRisk risk = GlucoseRiskAnalyzer.analyze(window(70, 0, 6, 42), props);
        assertNotNull(risk);
        assertEquals(Direction.HIGH, risk.direction());
        assertEquals(6, risk.badWeeks());
        assertEquals(85, risk.score());
    }

    @Test
    void persistentlyLowIsReported() {
        GlucoseRisk risk = GlucoseRiskAnalyzer.analyze(window(0, 30, 6, 42), props);
        assertNotNull(risk);
        assertEquals(Direction.LOW, risk.direction());
    }

    @Test
    void goodControlIsNotReported() {
        assertNull(GlucoseRiskAnalyzer.analyze(window(10, 3, 6, 42), props));
    }

    @Test
    void tooLittleDataIsNotReported() {
        assertNull(GlucoseRiskAnalyzer.analyze(window(80, 0, 6, 15), props));
        assertNull(GlucoseRiskAnalyzer.analyze(window(80, 0, 2, 42), props));
        assertNull(GlucoseRiskAnalyzer.analyze(null, props));
    }

    @Test
    void oneBadWeekIsNotAPersistentTrend() {
        List<Week> weeks = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            weeks.add(new Week(LocalDate.of(2026, 10, 1).plusDays(7L * i), 21, 150.0, i == 5 ? 90.0 : 10.0, 0.0));
        }
        GlucoseWindow w = new GlucoseWindow(1L, 42, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 11),
                126, 42, 150.0, 70.0, 20.0, 0.0, 0, 0, weeks);
        assertNull(GlucoseRiskAnalyzer.analyze(w, props));
    }

    @Test
    void highForFiveOfSixWeeksIsReported() {
        List<Week> weeks = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            weeks.add(new Week(LocalDate.of(2026, 10, 1).plusDays(7L * i), 21, 200.0, i == 0 ? 10.0 : 80.0, 0.0));
        }
        GlucoseWindow w = new GlucoseWindow(1L, 42, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 11),
                126, 42, 200.0, 25.0, 68.0, 0.0, 0, 0, weeks);
        GlucoseRisk risk = GlucoseRiskAnalyzer.analyze(w, props);
        assertNotNull(risk);
        assertEquals(5, risk.badWeeks());
    }
}
