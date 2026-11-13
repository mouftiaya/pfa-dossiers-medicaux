package ma.clinic.glucose.logic;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import ma.clinic.glucose.model.MeasureContext;

/**
 * Builds weeks of fake measurements so the trend analysis can be tested without waiting weeks.
 * Deterministic (no random generator): the same call always gives the same values. Values are in mg/dL.
 */
public final class DemoHistoryGenerator {

    private DemoHistoryGenerator() {
    }

    /** mean and spread: values are mean +/- spread, with the default targets (70-180 mg/dL). */
    public enum Profile {
        /** Everything between 80 and 170: in target. */
        STABLE(125, 45),
        /** Mostly between 170 and 310: persistently too high. */
        HIGH(240, 70),
        /** Between 45 and 125: about a third of the values below 70 (hypoglycaemia). */
        LOW(85, 40);

        final double mean;
        final double spread;

        Profile(double mean, double spread) {
            this.mean = mean;
            this.spread = spread;
        }
    }

    public record Generated(double value, LocalDateTime at, MeasureContext context) {
    }

    private record Slot(LocalTime time, MeasureContext context) {
    }

    private static final List<Slot> SLOTS = List.of(
            new Slot(LocalTime.of(7, 30), MeasureContext.FASTING),
            new Slot(LocalTime.of(13, 30), MeasureContext.AFTER_MEAL),
            new Slot(LocalTime.of(22, 0), MeasureContext.BEDTIME));

    /** Three measurements a day for the {@code days} days ending on today; none in the future. */
    public static List<Generated> generate(Profile profile, int days, LocalDateTime now) {
        List<Generated> result = new ArrayList<>();
        LocalDate today = now.toLocalDate();
        int n = 0;
        for (int d = days - 1; d >= 0; d--) {
            LocalDate day = today.minusDays(d);
            for (Slot slot : SLOTS) {
                LocalDateTime at = day.atTime(slot.time());
                n++;
                if (at.isAfter(now.minusMinutes(10))) {
                    continue;
                }
                double value = Math.round(Math.max(25, profile.mean + profile.spread * noise(n, profile.ordinal())));
                result.add(new Generated(value, at, slot.context()));
            }
        }
        return result;
    }

    /** Deterministic pseudo noise in [-1, 1). */
    static double noise(int i, int seed) {
        double x = Math.sin(i * 12.9898 + seed * 78.233) * 43758.5453;
        double fraction = x - Math.floor(x);
        return 2 * fraction - 1;
    }
}
