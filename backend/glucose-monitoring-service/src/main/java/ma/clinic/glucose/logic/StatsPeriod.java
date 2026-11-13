package ma.clinic.glucose.logic;

/** Statistics window: the number of days ending on the anchor day (included). */
public enum StatsPeriod {
    DAY(1),
    WEEK(7),
    MONTH(30);

    private final int days;

    StatsPeriod(int days) {
        this.days = days;
    }

    public int days() {
        return days;
    }
}
