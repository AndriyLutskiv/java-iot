package ua.crowpi.projects.p07;

import ua.crowpi.core.hardware.GpioFacade;

/**
 * Controls the two-channel relay board connected to the Raspberry Pi GPIO.
 *
 * <p>Relay 1 (pump, GPIO 21) and Relay 2 (fan, GPIO 20) are driven as
 * active-HIGH outputs — setting the pin HIGH energises the relay coil and
 * closes the normally-open contact.</p>
 *
 * <p>The class tracks the last applied {@link ActuatorState} so that
 * {@link #getCurrent()} can be queried without re-reading GPIO pins
 * (reading an output pin is not reliable on all platforms).</p>
 */
public class RelayBoard {

    /** GPIO facade used to drive the relay output pins. */
    private final GpioFacade gpio;

    /** BCM GPIO pin number for the pump relay (Relay 1). */
    private final int pumpPin;

    /** BCM GPIO pin number for the fan relay (Relay 2). */
    private final int fanPin;

    // Кешований стан — оновлюється кожного разу при виклику apply() або allOff()
    private ActuatorState currentState = ActuatorState.ALL_OFF;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a RelayBoard controlling two GPIO output pins.
     *
     * <p>The constructor does not change pin levels — call {@link #allOff()} or
     * {@link #apply(ActuatorState)} explicitly after construction.</p>
     *
     * @param gpio    GPIO facade (real Pi4J or mock)
     * @param pumpPin BCM GPIO pin number for Relay 1 (pump)
     * @param fanPin  BCM GPIO pin number for Relay 2 (fan)
     */
    public RelayBoard(GpioFacade gpio, int pumpPin, int fanPin) {
        this.gpio    = gpio;
        this.pumpPin = pumpPin;
        this.fanPin  = fanPin;
    }

    // -------------------------------------------------------------------------
    // Control methods
    // -------------------------------------------------------------------------

    /**
     * Drives both relay pins according to the given {@link ActuatorState}.
     *
     * <p>HIGH = relay energised (device ON), LOW = relay de-energised (device OFF).</p>
     *
     * @param state the desired combined actuator state; must not be {@code null}
     */
    public void apply(ActuatorState state) {
        // Виставляємо рівні обох пінів відповідно до нового стану
        gpio.setOutput(pumpPin, state.isPumpOn());
        gpio.setOutput(fanPin,  state.isFanOn());
        // Зберігаємо стан для подальшого getCurrent() без зайвого читання GPIO
        this.currentState = state;
        System.out.printf("[RELAY] Applied state=%s (pump=%s, fan=%s)%n",
                state, state.isPumpOn() ? "ON" : "OFF", state.isFanOn() ? "ON" : "OFF");
    }

    /**
     * Deactivates both relays by driving both pins LOW.
     *
     * <p>Called during {@link GreenhouseProject#shutdown()} to ensure no
     * actuator remains energised after the JVM exits.</p>
     */
    public void allOff() {
        // Примусово вимикаємо обидва реле незалежно від поточного стану
        gpio.setOutput(pumpPin, false);
        gpio.setOutput(fanPin,  false);
        this.currentState = ActuatorState.ALL_OFF;
        System.out.println("[RELAY] All relays OFF");
    }

    // -------------------------------------------------------------------------
    // State query
    // -------------------------------------------------------------------------

    /**
     * Returns the last {@link ActuatorState} that was applied to the relay board.
     *
     * <p>Reflects the most recent call to {@link #apply(ActuatorState)} or
     * {@link #allOff()}. The initial state before any call is {@link ActuatorState#ALL_OFF}.</p>
     *
     * @return current actuator state
     */
    public ActuatorState getCurrent() {
        return currentState;
    }
}
