package ua.crowpi.projects.p01;

/**
 * Immutable value object that holds one DHT11 sensor measurement.
 *
 * <p>Every reading captures temperature in Celsius, relative humidity as a
 * percentage, and the ISO-8601 timestamp at which the measurement was taken.
 * The class is intentionally a simple POJO — no business logic lives here;
 * classification logic resides in {@link ThermalZone}.</p>
 */
public class TemperatureReading {

    /** Temperature in degrees Celsius as reported by the DHT11. */
    private final double temperatureC;

    /** Relative humidity percentage (0–100) as reported by the DHT11. */
    private final double humidity;

    /** ISO-8601 formatted timestamp of when this reading was created. */
    private final String timestamp;

    /**
     * Constructs a new {@code TemperatureReading} with all three fields.
     *
     * @param temperatureC temperature in degrees Celsius
     * @param humidity     relative humidity as a percentage (0–100)
     * @param timestamp    ISO-8601 timestamp string, e.g. {@code "2024-03-15T14:23:01"}
     */
    public TemperatureReading(double temperatureC, double humidity, String timestamp) {
        this.temperatureC = temperatureC;
        this.humidity     = humidity;
        this.timestamp    = timestamp;
    }

    /**
     * Returns the temperature in degrees Celsius.
     *
     * @return temperature value in °C
     */
    public double getTemperatureC() {
        return temperatureC;
    }

    /**
     * Returns the relative humidity percentage.
     *
     * @return humidity value between 0.0 and 100.0
     */
    public double getHumidity() {
        return humidity;
    }

    /**
     * Returns the ISO-8601 formatted timestamp when this reading was taken.
     *
     * @return timestamp string, never {@code null}
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Returns a human-readable string representation of this reading,
     * suitable for console output and debugging.
     *
     * @return formatted string with timestamp, temperature, and humidity
     */
    @Override
    public String toString() {
        // Форматуємо рядок зручний для читання у логах та консолі
        return String.format("TemperatureReading{ts='%s', temp=%.1f°C, humidity=%.1f%%}",
                timestamp, temperatureC, humidity);
    }
}
