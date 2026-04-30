package ua.crowpi.projects.p09;

/**
 * A single measurement from the DHT11 temperature + humidity sensor.
 *
 * <p>Immutable value object — constructed once by {@link Dht11Reader} or
 * {@link ua.crowpi.core.mock.MockSensorReader} and passed around read-only.</p>
 */
public final class DhtReading {

    private final double temperatureC;
    private final double humidity;

    /**
     * Creates a DhtReading with the given sensor values.
     *
     * @param temperatureC temperature in degrees Celsius (typically 0–50 °C for DHT11)
     * @param humidity     relative humidity in percent (0–100 %)
     */
    public DhtReading(double temperatureC, double humidity) {
        this.temperatureC = temperatureC;
        this.humidity = humidity;
    }

    /**
     * Returns the measured temperature.
     *
     * @return temperature in °C
     */
    public double getTemperatureC() {
        return temperatureC;
    }

    /**
     * Returns the measured relative humidity.
     *
     * @return humidity in percent (0–100)
     */
    public double getHumidity() {
        return humidity;
    }

    @Override
    public String toString() {
        return String.format("DhtReading{temp=%.1f\u00b0C, humidity=%.0f%%}",
                temperatureC, humidity);
    }
}
