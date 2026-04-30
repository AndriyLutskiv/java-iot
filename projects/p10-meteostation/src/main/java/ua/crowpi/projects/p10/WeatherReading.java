package ua.crowpi.projects.p10;

/**
 * Immutable POJO representing a single weather station reading captured at one moment in time.
 *
 * <p>Each instance holds the raw sensor values from the DHT11 (temperature and humidity),
 * the sound sensor (noisy flag), the tilt sensor (wind detected flag), and an ISO-8601
 * timestamp string produced at capture time.</p>
 *
 * <p>Instances are created by {@link MeteostationProject} inside the scheduled polling task
 * and stored in the {@link RingBuffer} for later trend analysis and report generation.</p>
 */
public class WeatherReading {

    /** Temperature measured by DHT11, in degrees Celsius. */
    private final double tempC;

    /** Relative humidity measured by DHT11, as a percentage (0–100). */
    private final double humidity;

    /** {@code true} if the sound sensor detected noise above threshold during this reading. */
    private final boolean noisy;

    /** {@code true} if the tilt sensor indicated wind / tilt was detected. */
    private final boolean windDetected;

    /** ISO-8601 timestamp string marking when this reading was taken, e.g. {@code "2025-06-01T14:30:00"}. */
    private final String timestamp;

    /**
     * Constructs a new WeatherReading with all sensor values.
     *
     * @param tempC         temperature in degrees Celsius from DHT11
     * @param humidity      relative humidity percentage (0–100) from DHT11
     * @param noisy         {@code true} if sound sensor detected noise
     * @param windDetected  {@code true} if tilt sensor detected wind / tilt
     * @param timestamp     ISO-8601 timestamp string of when the reading was taken
     */
    public WeatherReading(double tempC, double humidity,
                          boolean noisy, boolean windDetected,
                          String timestamp) {
        this.tempC = tempC;
        this.humidity = humidity;
        this.noisy = noisy;
        this.windDetected = windDetected;
        // Захист від null-рядка — зберігаємо порожній рядок замість NPE
        this.timestamp = (timestamp != null) ? timestamp : "";
    }

    /**
     * Returns the temperature in degrees Celsius.
     *
     * @return temperature value from the DHT11 sensor
     */
    public double getTempC() {
        return tempC;
    }

    /**
     * Returns the relative humidity as a percentage.
     *
     * @return humidity value (0–100) from the DHT11 sensor
     */
    public double getHumidity() {
        return humidity;
    }

    /**
     * Returns whether ambient noise was detected during this reading interval.
     *
     * @return {@code true} if the sound sensor was triggered
     */
    public boolean isNoisy() {
        return noisy;
    }

    /**
     * Returns whether wind or tilt was detected during this reading interval.
     *
     * @return {@code true} if the tilt sensor was triggered
     */
    public boolean isWindDetected() {
        return windDetected;
    }

    /**
     * Returns the ISO-8601 timestamp string for when this reading was captured.
     *
     * @return timestamp string, never {@code null} (may be empty if none was provided)
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Returns a human-readable summary of this reading for logging and debugging.
     *
     * @return formatted string with all fields
     */
    @Override
    public String toString() {
        return String.format(
                "WeatherReading{ts='%s', tempC=%.1f, humidity=%.0f, noisy=%b, wind=%b}",
                timestamp, tempC, humidity, noisy, windDetected);
    }
}
