package ua.crowpi.projects.p01;

import ua.crowpi.core.util.PropertiesLoader;

import java.util.Properties;

/**
 * Holds the runtime-tunable configuration for the Thermometer project.
 *
 * <p>All values are loaded from {@code thermometer.properties} which must be
 * present on the classpath (i.e. under {@code src/main/resources/}).
 * A {@link ua.crowpi.core.exception.ConfigException} is thrown at startup if
 * any required key is missing or malformed — this is intentional because a
 * misconfigured sensor is a deployment error, not a recoverable runtime condition.</p>
 *
 * <p>Properties file keys:</p>
 * <ul>
 *   <li>{@code alert.threshold.celsius} — temperature above which buzzer triggers</li>
 *   <li>{@code poll.interval.seconds}   — how often the DHT11 is read</li>
 *   <li>{@code log.file}                — path to the CSV output file</li>
 * </ul>
 */
public class AlertConfig {

    /** Temperature threshold in °C above which the buzzer fires. */
    private final double alertThresholdCelsius;

    /** Interval between consecutive DHT11 reads in seconds. */
    private final int pollIntervalSeconds;

    /** Path to the CSV log file (relative to the working directory). */
    private final String logFile;

    /**
     * Loads configuration from {@code thermometer.properties} on the classpath.
     *
     * <p>Uses {@link PropertiesLoader#load(String)} so the file is found both
     * when running inside a fat JAR and during normal Gradle test execution.</p>
     */
    public AlertConfig() {
        // Завантажуємо конфігурацію з classpath — файл розташований в src/main/resources/
        Properties props = PropertiesLoader.load("thermometer.properties");

        // Читаємо поріг температури як дійсне число
        this.alertThresholdCelsius = PropertiesLoader.getDouble(props, "alert.threshold.celsius");

        // Читаємо інтервал опитування як ціле число секунд
        this.pollIntervalSeconds   = PropertiesLoader.getInt(props, "poll.interval.seconds");

        // Читаємо шлях до CSV-файлу як рядок
        this.logFile               = PropertiesLoader.getString(props, "log.file");
    }

    /**
     * Returns the alert threshold temperature in degrees Celsius.
     *
     * <p>When the measured temperature crosses this value (in either direction),
     * the buzzer fires three short beeps.</p>
     *
     * @return alert threshold in °C
     */
    public double getAlertThresholdCelsius() {
        return alertThresholdCelsius;
    }

    /**
     * Returns how often the DHT11 sensor should be polled.
     *
     * @return poll interval in seconds (default 2)
     */
    public int getPollIntervalSeconds() {
        return pollIntervalSeconds;
    }

    /**
     * Returns the path to the CSV log file.
     *
     * <p>The path is relative to the JVM working directory, e.g.
     * {@code "logs/thermometer.csv"}. Parent directories are created automatically
     * by {@link CsvDataLogger} on the first write.</p>
     *
     * @return CSV file path string
     */
    public String getLogFile() {
        return logFile;
    }
}
