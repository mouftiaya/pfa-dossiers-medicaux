package ma.clinic.notification.detection;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Detection rules, configurable under "alerts.*" in application.yml.
 *
 * @param dedupHours         an open alert of the same patient and type younger than this blocks a new one
 * @param variationDelta     mg/dL difference between two consecutive measurements that is "unusual"
 * @param variationMaxHours  maximum time between those two measurements
 * @param persistentCount    number of recent measurements that must all be out of target for a persistent alert
 */
@ConfigurationProperties(prefix = "alerts")
public record AlertRules(@DefaultValue("6") int dedupHours,
                         @DefaultValue("100") double variationDelta,
                         @DefaultValue("6") int variationMaxHours,
                         @DefaultValue("5") int persistentCount) {

    public static AlertRules defaults() {
        return new AlertRules(6, 100.0, 6, 5);
    }
}
