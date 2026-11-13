package ma.clinic.results.model;

public enum ValueFlag {
    NORMAL,
    LOW,
    HIGH,
    /** Dangerously low: the doctor is alerted when the result is validated. */
    CRITICAL_LOW,
    /** Dangerously high: the doctor is alerted when the result is validated. */
    CRITICAL_HIGH,
    /** Physiologically impossible (probable typing or extraction error): the result cannot be validated. */
    INVALID
}
