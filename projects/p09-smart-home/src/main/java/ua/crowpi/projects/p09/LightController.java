package ua.crowpi.projects.p09;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.hardware.GpioFacade;

/**
 * Controls the light load connected to CrowPi Relay channel 1 (GPIO 21).
 *
 * <p>Relay 1 on the CrowPi board is active-LOW: driving the GPIO pin HIGH opens
 * the relay (light OFF), driving it LOW closes the relay (light ON). This inversion
 * is handled transparently inside this class.</p>
 *
 * <p>The current state is tracked in memory so callers can check
 * {@link #isOn()} without reading the GPIO pin back.</p>
 */
public class LightController {

    private static final Logger LOG = LogManager.getLogger(LightController.class);

    /** BCM GPIO pin connected to Relay 1 on the CrowPi board. */
    public static final int RELAY1_PIN = 21;

    private final GpioFacade gpio;
    private boolean on;

    /**
     * Creates a LightController and drives the relay to the initial OFF state.
     *
     * @param gpio the GPIO facade to use for relay control
     */
    public LightController(GpioFacade gpio) {
        this.gpio = gpio;
        // Ініціалізуємо в стані OFF — реле не повинно замикатись без явної команди
        turnOff();
    }

    /**
     * Turns the light on (closes Relay 1).
     *
     * <p>If the light is already on, this method does nothing.</p>
     */
    public void turnOn() {
        if (!on) {
            // Реле CrowPi active-LOW: LOW = замкнуто = світло ввімкнено
            gpio.setOutput(RELAY1_PIN, false);
            on = true;
            LOG.info("Light ON (relay 1 closed)");
        }
    }

    /**
     * Turns the light off (opens Relay 1).
     *
     * <p>If the light is already off, this method does nothing.</p>
     */
    public void turnOff() {
        if (on || !isInitialized()) {
            // HIGH = розімкнуто = світло вимкнено
            gpio.setOutput(RELAY1_PIN, true);
            on = false;
            LOG.info("Light OFF (relay 1 opened)");
        }
    }

    /**
     * Toggles the light state (on → off, off → on).
     */
    public void toggle() {
        if (on) {
            turnOff();
        } else {
            turnOn();
        }
    }

    /**
     * Returns the current light state.
     *
     * @return {@code true} if the light relay is closed (light on)
     */
    public boolean isOn() {
        return on;
    }

    // Перший виклик turnOff() у конструкторі відбувається до встановлення on=false,
    // тому використовуємо цей прапор щоб різнити «щойно створений» стан
    private boolean initialized = false;

    private boolean isInitialized() {
        boolean was = initialized;
        initialized = true;
        return was;
    }
}
