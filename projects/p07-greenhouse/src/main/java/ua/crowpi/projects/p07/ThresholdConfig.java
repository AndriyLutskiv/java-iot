package ua.crowpi.projects.p07;

import ua.crowpi.core.util.PropertiesLoader;

import java.util.Properties;

/**
 * Configuration holder for the automatic greenhouse controller.
 *
 * <p>Loads all tunable parameters from {@code greenhouse.properties} on the classpath
 * (placed in {@code src/main/resources/}) using {@link PropertiesLoader}.
 * All fields are immutable after construction — configuration cannot change
 * while the project is running.</p>
 *
 * <p>Default property file content:</p>
 * <pre>
 *   temp.max.celsius=30
 *   soil.moisture.min.percent=40
 *   poll.interval.seconds=30
 *   log.file=logs/greenhouse.csv
 *   pump.relay.pin=21
 *   fan.relay.pin=20
 * </pre>
 */
public class ThresholdConfig {

    /** Maximum temperature in Celsius before the fan activates. */
    private final double tempMaxCelsius;

    /** Minimum soil moisture percentage before the pump activates. */
    private final int soilMinPercent;

    /** Sensor polling interval in seconds for the scheduled executor. */
    private final int pollIntervalSeconds;

    /** Filesystem path (relative or absolute) for the CSV data log. */
    private final String logFile;

    /** BCM GPIO pin number for the pump relay (Relay 1). */
    private final int pumpRelayPin;

    /** BCM GPIO pin number for the fan relay (Relay 2). */
    private final int fanRelayPin;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Loads greenhouse configuration from {@code greenhouse.properties} on the classpath.
     *
     * <p>Throws {@link ua.crowpi.core.exception.ConfigException} (unchecked) if
     * any required property is missing or cannot be parsed.</p>
     */
    public ThresholdConfig() {
        // Завантажуємо властивості з classpath — Gradle копіює resources/ в JAR
        Properties props = PropertiesLoader.load("greenhouse.properties");

        // Зчитуємо всі параметри з перевіркою типів через PropertiesLoader
        this.tempMaxCelsius     = PropertiesLoader.getDouble(props, "temp.max.celsius");
        this.soilMinPercent     = PropertiesLoader.getInt(props, "soil.moisture.min.percent");
        this.pollIntervalSeconds = PropertiesLoader.getInt(props, "poll.interval.seconds");
        this.logFile            = PropertiesLoader.getString(props, "log.file");
        this.pumpRelayPin       = PropertiesLoader.getInt(props, "pump.relay.pin");
        this.fanRelayPin        = PropertiesLoader.getInt(props, "fan.relay.pin");
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns the maximum allowed temperature in Celsius.
     * If this threshold is exceeded, the fan will be activated.
     *
     * @return maximum temperature in Celsius
     */
    public double getTempMaxCelsius() {
        return tempMaxCelsius;
    }

    /**
     * Returns the minimum soil moisture percentage.
     * If the soil drops below this value, the pump will be activated.
     *
     * @return minimum soil moisture in percent
     */
    public int getSoilMinPercent() {
        return soilMinPercent;
    }

    /**
     * Returns the sensor polling interval in seconds.
     * Used by {@link GreenhouseProject} to configure the {@link java.util.concurrent.ScheduledExecutorService}.
     *
     * @return polling interval in seconds
     */
    public int getPollIntervalSeconds() {
        return pollIntervalSeconds;
    }

    /**
     * Returns the path to the CSV log file where sensor data is appended.
     *
     * @return relative or absolute file path string
     */
    public String getLogFile() {
        return logFile;
    }

    /**
     * Returns the BCM GPIO pin number connected to the pump relay (Relay 1).
     *
     * @return BCM GPIO pin number for the pump relay
     */
    public int getPumpRelayPin() {
        return pumpRelayPin;
    }

    /**
     * Returns the BCM GPIO pin number connected to the fan relay (Relay 2).
     *
     * @return BCM GPIO pin number for the fan relay
     */
    public int getFanRelayPin() {
        return fanRelayPin;
    }
}
