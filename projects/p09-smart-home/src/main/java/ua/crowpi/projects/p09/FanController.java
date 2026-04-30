package ua.crowpi.projects.p09;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.hardware.GpioFacade;

/**
 * Controls the fan load connected to CrowPi Relay channel 2 (GPIO 20).
 *
 * <p>Same active-LOW relay logic as {@link LightController}: GPIO LOW = relay closed
 * (fan running), GPIO HIGH = relay open (fan stopped).</p>
 *
 * <p>Also called by {@link AutoTempControl} from a background thread — {@link #isOn()}
 * reads a {@code volatile} field and is safe to call from any thread without locking.</p>
 */
public class FanController {

    private static final Logger LOG = LogManager.getLogger(FanController.class);

    /** BCM GPIO pin connected to Relay 2 on the CrowPi board. */
    public static final int RELAY2_PIN = 20;

    private final GpioFacade gpio;
    private volatile boolean on;

    /**
     * Creates a FanController and drives the relay to the initial OFF state.
     *
     * @param gpio the GPIO facade to use for relay control
     */
    public FanController(GpioFacade gpio) {
        this.gpio = gpio;
        // Явна ініціалізація — реле не повинно замикатись без команди
        gpio.setOutput(RELAY2_PIN, true); // HIGH = розімкнуто
        this.on = false;
    }

    /**
     * Turns the fan on (closes Relay 2).
     *
     * <p>Thread-safe: called by both the menu thread and {@link AutoTempControl}.</p>
     */
    public void turnOn() {
        if (!on) {
            gpio.setOutput(RELAY2_PIN, false); // LOW = замкнуто
            on = true;
            LOG.info("Fan ON (relay 2 closed)");
        }
    }

    /**
     * Turns the fan off (opens Relay 2).
     *
     * <p>Thread-safe: called by both the menu thread and {@link AutoTempControl}.</p>
     */
    public void turnOff() {
        if (on) {
            gpio.setOutput(RELAY2_PIN, true); // HIGH = розімкнуто
            on = false;
            LOG.info("Fan OFF (relay 2 opened)");
        }
    }

    /**
     * Toggles the fan state.
     */
    public void toggle() {
        if (on) {
            turnOff();
        } else {
            turnOn();
        }
    }

    /**
     * Returns the current fan state.
     *
     * <p>Thread-safe: reads a {@code volatile} field.</p>
     *
     * @return {@code true} if the fan relay is closed (fan running)
     */
    public boolean isOn() {
        return on;
    }
}
