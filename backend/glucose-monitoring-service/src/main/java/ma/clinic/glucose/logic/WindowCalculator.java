package ma.clinic.glucose.logic;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import ma.clinic.glucose.dto.GlucoseDtos.WeekDto;
import ma.clinic.glucose.dto.GlucoseDtos.WindowDto;
import ma.clinic.glucose.logic.StatsCalculator.Reading;

/**
 * Summary of a long window (e.g. the last 42 days) split in weeks, used by the AI service to look for
 * persistent trends. Pure functions (no database, no clock), easy to unit test. All values are in mg/dL.
 *
 * Definitions (relative to the patient's own thresholds):
 * - in target      : targetMin <= value <= targetMax
 * - above target   : value > targetMax
 * - below low      : value < lowThreshold (hypoglycaemia)
 * - critical high  : value >= criticalHigh, critical low : value <= criticalLow
 */
public final class WindowCalculator {

    private WindowCalculator() {
    }

    /** The window is the {@code days} days ending on {@code to} (included); weeks are 7-day buckets ending on {@code to}. */
    public static WindowDto compute(Long patientId, LocalDate from, LocalDate to, List<Reading> readings, Thresholds t) {
        int days = (int) ChronoUnit.DAYS.between(from, to) + 1;
        int weekCount = (days + 6) / 7;

        Acc total = new Acc();
        List<Acc> weeks = new ArrayList<>();
        for (int i = 0; i < weekCount; i++) {
            weeks.add(new Acc());
        }
        java.util.Set<LocalDate> daysWithData = new java.util.HashSet<>();

        for (Reading r : readings) {
            LocalDate day = r.at().toLocalDate();
            if (day.isBefore(from) || day.isAfter(to)) {
                continue;
            }
            daysWithData.add(day);
            total.add(r.value(), t);
            int weeksAgo = (int) (ChronoUnit.DAYS.between(day, to) / 7);
            weeks.get(weekCount - 1 - weeksAgo).add(r.value(), t);
        }

        List<WeekDto> weekDtos = new ArrayList<>();
        for (int i = 0; i < weekCount; i++) {
            LocalDate weekStart = to.minusDays(7L * (weekCount - i) - 1);
            Acc w = weeks.get(i);
            weekDtos.add(new WeekDto(weekStart, w.count, w.average(), w.percentAbove(), w.percentBelowLow()));
        }
        return new WindowDto(patientId, days, from, to, total.count, daysWithData.size(), total.average(),
                total.percentInTarget(), total.percentAbove(), total.percentBelowLow(),
                total.criticalHigh, total.criticalLow, weekDtos);
    }

    /** Running counters for one group of readings. */
    private static final class Acc {
        int count;
        double sum;
        int inTarget;
        int above;
        int belowLow;
        int criticalHigh;
        int criticalLow;

        void add(double value, Thresholds t) {
            count++;
            sum += value;
            if (value >= t.targetMin() && value <= t.targetMax()) {
                inTarget++;
            }
            if (value > t.targetMax()) {
                above++;
            }
            if (value < t.lowThreshold()) {
                belowLow++;
            }
            if (value >= t.criticalHigh()) {
                criticalHigh++;
            }
            if (value <= t.criticalLow()) {
                criticalLow++;
            }
        }

        Double average() {
            return count == 0 ? null : round1(sum / count);
        }

        Double percentInTarget() {
            return percent(inTarget);
        }

        Double percentAbove() {
            return percent(above);
        }

        Double percentBelowLow() {
            return percent(belowLow);
        }

        private Double percent(int n) {
            return count == 0 ? null : round1(100.0 * n / count);
        }
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
