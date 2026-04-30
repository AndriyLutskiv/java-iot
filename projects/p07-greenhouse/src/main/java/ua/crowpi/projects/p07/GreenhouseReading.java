package ua.crowpi.projects.p07;

/**
 * Immutable data transfer object representing one measurement snapshot
 * from all greenhouse sensors at a single point in time.
 *
 * <p>Instances are created by {@link GreenhouseProject} after each polling
 * cycle and passed to {@link GreenhouseController}, {@link CsvDataLogger},
 * and the LCD/RGB display helpers.</p>
 */
public class GreenhouseReading {

    /** Temperature read from DHT11 sensor, in degrees Celsius. */
    private final double tempC;

    /** Relative humidity read from DHT11 sensor, in percent (0–100). */
    private final double humidity;

    /** Soil moisture level from MCP3008 ADC sensor, in percent (0–100). */
    private final int soilPercent;

    /** ISO-8601 timestamp string when this reading was taken. */
    private final String timestamp;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a new immutable GreenhouseReading with all four sensor values.
     *
     * @param tempC       temperature in Celsius from DHT11
     * @param humidity    relative humidity percentage from DHT11
     * @param soilPercent soil moisture percentage from MCP3008 (0=dry, 100=wet)
     * @param timestamp   formatted timestamp string, e.g. {@code "2026-03-20T14:35:00"}
     */
    public GreenhouseReading(double tempC, double humidity, int soilPercent, String timestamp) {
        this.tempC = tempC;
        this.humidity = humidity;
        this.soilPercent = soilPercent;
        this.timestamp = timestamp;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns the temperature value in degrees Celsius.
     *
     * @return temperature in Celsius
     */
    public double getTempC() {
        return tempC;
    }

    /**
     * Returns the relative humidity as a percentage.
     *
     * @return humidity percentage, 0–100
     */
    public double getHumidity() {
        return humidity;
    }

    /**
     * Returns the soil moisture level as a percentage.
     * Higher values indicate wetter soil (100 = fully saturated).
     *
     * @return soil moisture percentage, 0–100
     */
    public int getSoilPercent() {
        return soilPercent;
    }

    /**
     * Returns the timestamp when this reading was captured.
     *
     * @return ISO-8601 formatted timestamp string
     */
    public String getTimestamp() {
        return timestamp;
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    /**
     * Returns a human-readable string representation of this reading,
     * suitable for console logging and debugging.
     *
     * @return formatted string with all sensor values
     */
    @Override
    public String toString() {
        return "GreenhouseReading{"
                + "timestamp='" + timestamp + '\''
                + ", tempC=" + tempC
                + ", humidity=" + humidity
                + ", soilPercent=" + soilPercent
                + '}';
    }
}
