package ma.clinic.ai.glucose;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the glucose trend analysis (prefix ai.glucose-risk in application.yml).
 * Nothing is sent after a single measurement: the AI looks at weeks of history and only reports a patient
 * whose glucose has stayed out of range for most of that time.
 */
@ConfigurationProperties(prefix = "ai.glucose-risk")
public class GlucoseRiskProperties {

    /** Turns the scheduled analysis on or off (the manual button keeps working). */
    private boolean enabled = true;
    /** Length of the analysed history, in days (42 = 6 weeks). */
    private int windowDays = 42;
    /** Minimum number of distinct days with at least one measurement before any conclusion is drawn. */
    private int minDaysWithData = 28;
    /** Minimum number of measurements in the window. */
    private int minReadings = 40;
    /** Minimum number of weeks (out of the window) containing enough data to be judged. */
    private int minWeeksWithData = 4;
    /** A week counts as "too high" when at least this share of its values is above the target (percent). */
    private double weekHighPercent = 50;
    /** A week counts as "too low" when at least this share of its values is below the low threshold (percent). */
    private double weekLowPercent = 15;
    /** Share of the judged weeks that must be bad for the trend to be called persistent (percent). */
    private double persistentWeeksPercent = 75;
    /** A week needs at least this number of measurements to be judged. */
    private int minReadingsPerWeek = 5;
    /** Cron expression of the scheduled run (default: every day at 02:00). */
    private String cron = "0 0 2 * * *";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getWindowDays() {
        return windowDays;
    }

    public void setWindowDays(int windowDays) {
        this.windowDays = windowDays;
    }

    public int getMinDaysWithData() {
        return minDaysWithData;
    }

    public void setMinDaysWithData(int minDaysWithData) {
        this.minDaysWithData = minDaysWithData;
    }

    public int getMinReadings() {
        return minReadings;
    }

    public void setMinReadings(int minReadings) {
        this.minReadings = minReadings;
    }

    public int getMinWeeksWithData() {
        return minWeeksWithData;
    }

    public void setMinWeeksWithData(int minWeeksWithData) {
        this.minWeeksWithData = minWeeksWithData;
    }

    public double getWeekHighPercent() {
        return weekHighPercent;
    }

    public void setWeekHighPercent(double weekHighPercent) {
        this.weekHighPercent = weekHighPercent;
    }

    public double getWeekLowPercent() {
        return weekLowPercent;
    }

    public void setWeekLowPercent(double weekLowPercent) {
        this.weekLowPercent = weekLowPercent;
    }

    public double getPersistentWeeksPercent() {
        return persistentWeeksPercent;
    }

    public void setPersistentWeeksPercent(double persistentWeeksPercent) {
        this.persistentWeeksPercent = persistentWeeksPercent;
    }

    public int getMinReadingsPerWeek() {
        return minReadingsPerWeek;
    }

    public void setMinReadingsPerWeek(int minReadingsPerWeek) {
        this.minReadingsPerWeek = minReadingsPerWeek;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }
}
