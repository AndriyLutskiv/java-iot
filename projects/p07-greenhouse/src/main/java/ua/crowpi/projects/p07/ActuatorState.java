package ua.crowpi.projects.p07;

/**
 * Represents the combined on/off state of the two actuators:
 * the water pump (Relay 1, GPIO 21) and the cooling fan (Relay 2, GPIO 20).
 *
 * <p>Provides factory methods and combinators that keep the rules engine
 * ({@link GreenhouseController}) free of bitmask arithmetic.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 *   ActuatorState s = ActuatorState.of(true, false);  // PUMP_ON
 *   s = s.withFan(true);                              // BOTH_ON
 *   s.isPumpOn();                                     // true
 * }</pre>
 */
public enum ActuatorState {

    /** Both pump and fan are switched off. */
    ALL_OFF,

    /** Only the water pump (Relay 1) is running. */
    PUMP_ON,

    /** Only the cooling fan (Relay 2) is running. */
    FAN_ON,

    /** Both the pump and the fan are running simultaneously. */
    BOTH_ON;

    // -------------------------------------------------------------------------
    // State query methods
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the water pump relay is currently energised.
     *
     * @return {@code true} for {@link #PUMP_ON} and {@link #BOTH_ON}
     */
    public boolean isPumpOn() {
        // Насос увімкнено лише коли стан явно передбачає роботу насосу
        return this == PUMP_ON || this == BOTH_ON;
    }

    /**
     * Returns {@code true} if the cooling fan relay is currently energised.
     *
     * @return {@code true} for {@link #FAN_ON} and {@link #BOTH_ON}
     */
    public boolean isFanOn() {
        // Вентилятор увімкнено лише коли стан явно передбачає роботу вентилятора
        return this == FAN_ON || this == BOTH_ON;
    }

    // -------------------------------------------------------------------------
    // Combinators
    // -------------------------------------------------------------------------

    /**
     * Returns a new {@code ActuatorState} that keeps the current fan state and
     * sets the pump state to the given value.
     *
     * <p>This allows the rules engine to update one actuator independently
     * without knowing or resetting the other.</p>
     *
     * @param on {@code true} to turn the pump on; {@code false} to turn it off
     * @return the resulting combined state
     */
    public ActuatorState withPump(boolean on) {
        // Поточний стан вентилятора зберігаємо, а насос виставляємо за параметром
        return of(on, this.isFanOn());
    }

    /**
     * Returns a new {@code ActuatorState} that keeps the current pump state and
     * sets the fan state to the given value.
     *
     * <p>This allows the rules engine to update one actuator independently
     * without knowing or resetting the other.</p>
     *
     * @param on {@code true} to turn the fan on; {@code false} to turn it off
     * @return the resulting combined state
     */
    public ActuatorState withFan(boolean on) {
        // Поточний стан насосу зберігаємо, а вентилятор виставляємо за параметром
        return of(this.isPumpOn(), on);
    }

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    /**
     * Creates an {@code ActuatorState} from two independent boolean flags.
     *
     * <p>This is the canonical way to construct a state when both actuator
     * values are known simultaneously (e.g. after the rules engine evaluates
     * a {@link GreenhouseReading}).</p>
     *
     * @param pump {@code true} if the pump should be on
     * @param fan  {@code true} if the fan should be on
     * @return the matching enum constant
     */
    public static ActuatorState of(boolean pump, boolean fan) {
        // Комбінація двох булевих прапорів -> один з чотирьох станів
        if (pump && fan) {
            return BOTH_ON;
        } else if (pump) {
            return PUMP_ON;
        } else if (fan) {
            return FAN_ON;
        } else {
            return ALL_OFF;
        }
    }
}
