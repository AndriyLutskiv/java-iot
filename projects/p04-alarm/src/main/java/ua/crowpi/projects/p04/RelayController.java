package ua.crowpi.projects.p04;

import ua.crowpi.core.hardware.GpioFacade;

/**
 * Controls the CrowPi relay module via a single GPIO output pin.
 *
 * <p>The relay is used to switch an external load (e.g. an alarm buzzer or door lock)
 * on/off in response to alarm state changes. Setting the GPIO pin HIGH energises the
 * relay coil, closing the relay contact (active-HIGH wiring). Setting it LOW opens
 * the relay contact.</p>
 *
 * <p>On the CrowPi the relay is connected to BCM GPIO 21.</p>
 */
public class RelayController {

    /** BCM GPIO pin number connected to the relay coil input. */
    public static final int RELAY1_PIN = 21;

    // GPIO абстракція — дозволяє підмінити на MockGpioFacade в тестах
    private final GpioFacade gpio;

    // Відстежуємо поточний стан щоб не робити зайвих GPIO-записів при повторних викликах
    private boolean active;

    /**
     * Creates a RelayController using the provided GPIO facade.
     *
     * <p>The relay is initially deactivated (GPIO pin LOW).</p>
     *
     * @param gpio the GPIO facade to use for driving the relay pin
     */
    public RelayController(GpioFacade gpio) {
        this.gpio = gpio;
        this.active = false;
        // Явно встановлюємо LOW при ініціалізації — гарантуємо відомий стан при старті
        gpio.setOutput(RELAY1_PIN, false);
    }

    /**
     * Activates the relay by driving the GPIO pin HIGH.
     *
     * <p>Closes the relay contact, energising the connected load.
     * Calling this method when the relay is already active has no effect.</p>
     */
    public void activate() {
        if (!active) {
            // Вмикаємо реле лише якщо воно ще не активне — уникаємо зайвих GPIO операцій
            gpio.setOutput(RELAY1_PIN, true);
            active = true;
        }
    }

    /**
     * Deactivates the relay by driving the GPIO pin LOW.
     *
     * <p>Opens the relay contact, de-energising the connected load.
     * Calling this method when the relay is already inactive has no effect.</p>
     */
    public void deactivate() {
        if (active) {
            // Вимикаємо реле лише якщо воно активне — уникаємо зайвих GPIO операцій
            gpio.setOutput(RELAY1_PIN, false);
            active = false;
        }
    }

    /**
     * Returns whether the relay is currently active (energised).
     *
     * @return {@code true} if the relay contact is closed; {@code false} if open
     */
    public boolean isActive() {
        return active;
    }
}
