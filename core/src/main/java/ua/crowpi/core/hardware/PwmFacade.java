package ua.crowpi.core.hardware;

/**
 * High-level abstraction for software PWM (Pulse-Width Modulation) on GPIO pins.
 *
 * <p>While {@link GpioFacade#pwm(int, int, float)} covers hardware PWM on dedicated
 * PWM pins (GPIO 18/19), this facade provides a uniform API for software-emulated
 * PWM on any GPIO pin — used by projects that need multiple simultaneous PWM channels
 * (e.g. RGB LED with three independent duty cycles).</p>
 *
 * <p>On real hardware the implementation wraps Pi4J's
 * {@code GpioPinPwmOutput} with software PWM. In mock mode,
 * {@link ua.crowpi.core.mock.MockPwmFacade} logs all calls to stdout.</p>
 */
public interface PwmFacade {

    /**
     * Starts (or updates) PWM output on the specified pin.
     *
     * @param pin         BCM GPIO pin number
     * @param frequencyHz PWM frequency in Hertz (typical range 50–20 000 Hz)
     * @param dutyCycle   ratio of HIGH time, 0.0 (always off) … 1.0 (always on)
     */
    void setPwm(int pin, int frequencyHz, float dutyCycle);

    /**
     * Stops PWM on the specified pin and drives it LOW.
     *
     * @param pin BCM GPIO pin number where PWM is currently active
     */
    void stopPwm(int pin);

    /**
     * Releases all PWM resources and drives all managed pins LOW.
     *
     * <p>Must be called from {@link ua.crowpi.core.CrowPiProject#shutdown()}.</p>
     */
    void close();
}
