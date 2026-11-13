package ma.clinic.ai.glucose;

import java.util.Locale;

import ma.clinic.ai.glucose.GlucoseRisk.Direction;
import ma.clinic.ai.glucose.GlucoseWindow.Week;

/**
 * Looks for a persistent trend in the weekly summaries of a patient. Pure logic, no Spring: easy to test and to
 * explain. It is a rule-based indicator, not a diagnosis: the doctor and the assistant decide what to do.
 *
 * Rules: with enough data (days, readings, weeks), a patient is "persistently high" when at least
 * persistentWeeksPercent of the judged weeks have weekHighPercent or more of their values above the target
 * (and the whole window agrees). "Persistently low" works the same way with the values below the low threshold.
 * If both are true, the direction with the larger share of bad weeks wins (hypoglycaemia first when equal).
 */
public final class GlucoseRiskAnalyzer {

    private GlucoseRiskAnalyzer() {
    }

    /** Returns the risk, or null when there is not enough data or no persistent problem. */
    public static GlucoseRisk analyze(GlucoseWindow w, GlucoseRiskProperties p) {
        if (w == null || w.weeks() == null || w.count() < p.getMinReadings() || w.daysWithData() < p.getMinDaysWithData()) {
            return null;
        }
        int judged = 0;
        int highWeeks = 0;
        int lowWeeks = 0;
        for (Week week : w.weeks()) {
            if (week.count() < p.getMinReadingsPerWeek()) {
                continue;
            }
            judged++;
            if (value(week.percentAboveTarget()) >= p.getWeekHighPercent()) {
                highWeeks++;
            }
            if (value(week.percentBelowLow()) >= p.getWeekLowPercent()) {
                lowWeeks++;
            }
        }
        if (judged < p.getMinWeeksWithData()) {
            return null;
        }
        double needed = judged * p.getPersistentWeeksPercent() / 100.0;
        boolean high = highWeeks >= needed && value(w.percentAboveTarget()) >= p.getWeekHighPercent();
        boolean low = lowWeeks >= needed && value(w.percentBelowLow()) >= p.getWeekLowPercent();
        if (!high && !low) {
            return null;
        }
        Direction direction = high && (!low || highWeeks > lowWeeks) ? Direction.HIGH : Direction.LOW;
        int bad = direction == Direction.HIGH ? highWeeks : lowWeeks;
        double share = direction == Direction.HIGH ? value(w.percentAboveTarget()) : value(w.percentBelowLow());
        int score = (int) Math.min(100, Math.round(share * 0.5 + 100.0 * bad / judged * 0.5));
        String what = direction == Direction.HIGH ? "au-dessus de la cible" : "sous le seuil bas";
        String summary = String.format(Locale.ROOT,
                "Sur %d jours (%d jours avec mesures), %d semaines sur %d avec une glycémie %s ; %.0f%% des valeurs concernées.",
                w.days(), w.daysWithData(), bad, judged, what, share);
        return new GlucoseRisk(direction, score, w.days(), w.daysWithData(), bad, judged, w.average(), summary);
    }

    private static double value(Double v) {
        return v == null ? 0 : v;
    }
}
