package ma.clinic.glucose.logic;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ma.clinic.glucose.dto.GlucoseDtos.StatsDto;
import ma.clinic.glucose.dto.GlucoseDtos.StatsPoint;
import ma.clinic.glucose.dto.GlucoseDtos.TargetDto;
import ma.clinic.glucose.model.GlucoseFlag;

/** Statistics over a list of readings (mg/dL). Pure functions, easy to unit test. */
public final class StatsCalculator {

    private StatsCalculator() {
    }

    public record Reading(double value, LocalDateTime at) {
    }

    /** average, minimum, maximum and percentInTarget are null when count is 0. */
    public record Aggregate(
            int count,
            Double average,
            Double minimum,
            Double maximum,
            Double percentInTarget,
            int highCount,
            int lowCount) {
    }

    public static Aggregate aggregate(List<Reading> readings, Thresholds thresholds) {
        if (readings.isEmpty()) {
            return new Aggregate(0, null, null, null, null, 0, 0);
        }
        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        int inTarget = 0;
        int high = 0;
        int low = 0;
        for (Reading r : readings) {
            sum += r.value();
            min = Math.min(min, r.value());
            max = Math.max(max, r.value());
            GlucoseFlag flag = FlagCalculator.compute(r.value(), thresholds);
            switch (flag) {
                case IN_TARGET -> inTarget++;
                case HIGH, CRITICAL_HIGH -> high++;
                case LOW, CRITICAL_LOW -> low++;
                default -> {
                }
            }
        }
        int n = readings.size();
        return new Aggregate(n, round1(sum / n), min, max, round1(100.0 * inTarget / n), high, low);
    }

    /**
     * Builds the statistics of a period. The readings must already be restricted to the window
     * [anchor - (days - 1), anchor]; they may come in any order.
     */
    public static StatsDto compute(StatsPeriod period, LocalDate anchor, List<Reading> readings,
                                   Thresholds thresholds, TargetDto target) {
        LocalDate from = anchor.minusDays(period.days() - 1L);
        Aggregate agg = aggregate(readings, thresholds);
        List<Reading> sorted = new ArrayList<>(readings);
        sorted.sort(Comparator.comparing(Reading::at));

        List<StatsPoint> points = new ArrayList<>();
        if (period == StatsPeriod.DAY) {
            for (Reading r : sorted) {
                points.add(new StatsPoint(r.at(), r.value(), r.value(), r.value(), 1));
            }
        } else {
            Map<LocalDate, List<Reading>> byDay = new TreeMap<>();
            for (Reading r : sorted) {
                byDay.computeIfAbsent(r.at().toLocalDate(), d -> new ArrayList<>()).add(r);
            }
            for (Map.Entry<LocalDate, List<Reading>> e : byDay.entrySet()) {
                double sum = 0;
                double min = Double.MAX_VALUE;
                double max = -Double.MAX_VALUE;
                for (Reading r : e.getValue()) {
                    sum += r.value();
                    min = Math.min(min, r.value());
                    max = Math.max(max, r.value());
                }
                int n = e.getValue().size();
                points.add(new StatsPoint(e.getKey().atStartOfDay(), round1(sum / n), min, max, n));
            }
        }
        return new StatsDto(period, from, anchor, agg.count(), agg.average(), agg.minimum(), agg.maximum(),
                agg.percentInTarget(), agg.highCount(), agg.lowCount(), target, points);
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
