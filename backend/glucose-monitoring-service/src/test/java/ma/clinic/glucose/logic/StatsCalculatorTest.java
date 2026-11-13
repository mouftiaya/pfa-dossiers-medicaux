package ma.clinic.glucose.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import ma.clinic.glucose.dto.GlucoseDtos.StatsDto;
import ma.clinic.glucose.dto.GlucoseDtos.StatsPoint;
import ma.clinic.glucose.logic.StatsCalculator.Aggregate;
import ma.clinic.glucose.logic.StatsCalculator.Reading;
import org.junit.jupiter.api.Test;

class StatsCalculatorTest {

    private static final Thresholds D = Thresholds.DEFAULTS;
    private static final LocalDate ANCHOR = LocalDate.of(2026, 11, 10);

    // Deliberately not in chronological order.
    private static List<Reading> readings() {
        return List.of(
                new Reading(300, LocalDateTime.of(2026, 11, 10, 20, 0)),   // CRITICAL_HIGH
                new Reading(100, LocalDateTime.of(2026, 11, 9, 8, 0)),     // IN_TARGET
                new Reading(60, LocalDateTime.of(2026, 11, 10, 7, 0)),     // LOW
                new Reading(200, LocalDateTime.of(2026, 11, 9, 12, 0)));   // OUT_OF_TARGET
    }

    @Test
    void aggregateOfEmptyListIsNullExceptCounts() {
        Aggregate a = StatsCalculator.aggregate(List.of(), D);
        assertEquals(0, a.count());
        assertNull(a.average());
        assertNull(a.minimum());
        assertNull(a.maximum());
        assertNull(a.percentInTarget());
        assertEquals(0, a.highCount());
        assertEquals(0, a.lowCount());
    }

    @Test
    void aggregateComputesPercentInTargetAndCounts() {
        Aggregate a = StatsCalculator.aggregate(readings(), D);
        assertEquals(4, a.count());
        assertEquals(165.0, a.average());
        assertEquals(60.0, a.minimum());
        assertEquals(300.0, a.maximum());
        assertEquals(25.0, a.percentInTarget());   // only 100 is IN_TARGET
        assertEquals(1, a.highCount());
        assertEquals(1, a.lowCount());
    }

    @Test
    void percentInTargetIsRoundedToOneDecimal() {
        List<Reading> r = List.of(
                new Reading(100, LocalDateTime.of(2026, 11, 10, 8, 0)),
                new Reading(110, LocalDateTime.of(2026, 11, 10, 9, 0)),
                new Reading(400, LocalDateTime.of(2026, 11, 10, 10, 0)));
        assertEquals(66.7, StatsCalculator.aggregate(r, D).percentInTarget());
    }

    @Test
    void weekAggregatesPerCalendarDayOldestFirst() {
        StatsDto s = StatsCalculator.compute(StatsPeriod.WEEK, ANCHOR, readings(), D, null);
        assertEquals(LocalDate.of(2026, 11, 4), s.from());
        assertEquals(ANCHOR, s.to());
        assertEquals(4, s.count());
        assertEquals(2, s.points().size());

        StatsPoint d1 = s.points().get(0);
        assertEquals(LocalDateTime.of(2026, 11, 9, 0, 0), d1.at());
        assertEquals(150.0, d1.average());
        assertEquals(100.0, d1.minimum());
        assertEquals(200.0, d1.maximum());
        assertEquals(2, d1.count());

        StatsPoint d2 = s.points().get(1);
        assertEquals(LocalDateTime.of(2026, 11, 10, 0, 0), d2.at());
        assertEquals(180.0, d2.average());
        assertEquals(60.0, d2.minimum());
        assertEquals(300.0, d2.maximum());
        assertEquals(2, d2.count());
    }

    @Test
    void monthWindowIs30DaysEndingOnAnchor() {
        StatsDto s = StatsCalculator.compute(StatsPeriod.MONTH, ANCHOR, readings(), D, null);
        assertEquals(LocalDate.of(2026, 10, 12), s.from());
        assertEquals(ANCHOR, s.to());
    }

    @Test
    void dayHasOnePointPerMeasurementOldestFirst() {
        StatsDto s = StatsCalculator.compute(StatsPeriod.DAY, ANCHOR, readings(), D, null);
        assertEquals(ANCHOR, s.from());
        assertEquals(4, s.points().size());
        assertEquals(LocalDateTime.of(2026, 11, 9, 8, 0), s.points().get(0).at());
        assertEquals(100.0, s.points().get(0).average());
        assertEquals(1, s.points().get(0).count());
        assertEquals(LocalDateTime.of(2026, 11, 10, 20, 0), s.points().get(3).at());
    }

    @Test
    void emptyPeriodHasNoPointsAndNullStats() {
        StatsDto s = StatsCalculator.compute(StatsPeriod.WEEK, ANCHOR, List.of(), D, null);
        assertEquals(0, s.count());
        assertNull(s.average());
        assertNull(s.percentInTarget());
        assertEquals(0, s.points().size());
    }
}
