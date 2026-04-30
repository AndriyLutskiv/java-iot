package ua.crowpi.core.hardware;

/**
 * Abstraction layer over the Raspberry Pi GPIO subsystem (Pi4J v1.4).
 *
 * <p>All project code must interact with GPIO exclusively through this interface,
 * never through Pi4J classes directly. This allows the entire hardware layer to
 * be replaced with {@link ua.crowpi.core.mock.MockGpioFacade} when running on a
 * developer laptop with {@code --mock} mode.</p>
 *
 * <p>Pin numbers follow the <strong>BCM (Broadcom) numbering scheme</strong>
 * used throughout CrowPi documentation.</p>
 */
public interface GpioFacade {

    /**
     * Drives a GPIO output pin HIGH or LOW.
     *
     * @param pin  BCM GPIO pin number (e.g. 17, 18, 22 …)
     * @param high {@code true} to set 3.3 V; {@code false} to set 0 V
     */
    void setOutput(int pin, boolean high);

    /**
     * Switches a GPIO pin to input mode with pull-up resistance.
     *
     * <p>Required for single-wire bit-bang protocols (e.g. DHT11) where the
     * same pin is used as output for the start signal and then immediately
     * released to input so the sensor can drive it.  Calling this method
     * unprovisions any existing output on the pin before provisioning it as
     * a digital input with an internal pull-up.</p>
     *
     * @param pin BCM GPIO pin number
     */
    void setInput(int pin);

    /**
     * Reads the current digital level of a GPIO input pin.
     *
     * @param pin BCM GPIO pin number configured as input
     * @return {@code true} if the pin reads HIGH; {@code false} if LOW
     */
    boolean readInput(int pin);

    /**
     * Registers a listener that is notified whenever a GPIO input pin changes state.
     *
     * <p>Multiple listeners may be registered for the same pin. The listener is
     * called from a background thread managed by the facade — implementations
     * must be thread-safe.</p>
     *
     * @param pin      BCM GPIO pin number to monitor
     * @param listener callback invoked on each state change
     */
    void addListener(int pin, PinChangeListener listener);

    /**
     * Configures hardware PWM on a PWM-capable GPIO pin.
     *
     * <p>On real hardware only GPIO 18 (PWM0) and GPIO 19 (PWM1) support
     * hardware PWM via Pi4J on RPi 3. For software PWM use
     * {@link PwmFacade} instead.</p>
     *
     * @param pin         BCM GPIO pin number (18 or 19 for hardware PWM)
     * @param frequencyHz PWM carrier frequency in Hertz (e.g. 440 for musical A4)
     * @param dutyCycle   ratio of HIGH time, 0.0 (always off) … 1.0 (always on)
     */
    void pwm(int pin, int frequencyHz, float dutyCycle);

    /**
     * Releases all GPIO resources claimed by this facade and un-exports all pins.
     *
     * <p>Must be called from {@link ua.crowpi.core.CrowPiProject#shutdown()} to
     * avoid leaving pins in an undefined state after the JVM exits.</p>
     */
    void close();
}
