package ua.crowpi.projects.p09;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.exception.HardwareException;
import ua.crowpi.core.hardware.I2cFacade;
import ua.crowpi.core.hardware.SensorReader;

/**
 * Menu item [1]: reads the DHT11 sensor and displays temperature + humidity on the LCD.
 *
 * <p>The reading is done synchronously (blocking) because the menu is paused while
 * the user has this item selected. DHT11 requires ~18 ms to respond which is
 * imperceptible to the user.</p>
 */
public class TemperatureMenuItem implements MenuItem {

    private static final Logger LOG = LogManager.getLogger(TemperatureMenuItem.class);

    private final SensorReader<DhtReading> sensor;
    private final I2cFacade lcd;
    private final DeviceState deviceState;

    /**
     * Creates a TemperatureMenuItem.
     *
     * @param sensor      DHT11 reader (real or mock)
     * @param lcd         I2C LCD facade
     * @param deviceState shared runtime state (provides active profile name for display)
     */
    public TemperatureMenuItem(SensorReader<DhtReading> sensor,
                               I2cFacade lcd,
                               DeviceState deviceState) {
        this.sensor = sensor;
        this.lcd = lcd;
        this.deviceState = deviceState;
    }

    /** {@inheritDoc} */
    @Override
    public String getLabel() {
        return "TEMPERATURE     ";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads DHT11 and shows on LCD:</p>
     * <pre>
     *   TEMP: 23.4C H:61
     *   PROFILE: DAY
     * </pre>
     */
    @Override
    public void execute() {
        try {
            DhtReading reading = sensor.read();
            // Форматуємо рядки для LCD 16×2 (рівно 16 символів)
            String line1 = String.format("T:%4.1fC  H:%3.0f%%",
                    reading.getTemperatureC(), reading.getHumidity());
            String line2 = String.format("P:%-14s", deviceState.getActiveProfileName());
            LcdHelper.writeLine(lcd, 0, line1);
            LcdHelper.writeLine(lcd, 1, line2);
        } catch (HardwareException e) {
            LOG.error("DHT11 read error: {}", e.getMessage());
            LcdHelper.writeLine(lcd, 0, "SENSOR ERROR    ");
            LcdHelper.writeLine(lcd, 1, e.getMessage().substring(0,
                    Math.min(16, e.getMessage().length())));
        }
    }
}
