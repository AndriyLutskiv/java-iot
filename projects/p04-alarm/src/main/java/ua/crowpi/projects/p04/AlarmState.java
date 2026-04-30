package ua.crowpi.projects.p04;

/**
 * Represents the possible operating states of the alarm system FSM.
 *
 * <p>State lifecycle overview:</p>
 * <pre>
 *   DISARMED ──(CORRECT_PIN)──► ARMED
 *   ARMED    ──(CORRECT_PIN)──► DISARMED
 *   ARMED    ──(PIR_TRIGGERED)─► TRIGGERED
 *   TRIGGERED──(CORRECT_PIN)──► DISARMED
 *   any      ──(lockout)───────► LOCKED
 *   LOCKED   ──(LOCK_EXPIRED)──► DISARMED
 * </pre>
 *
 * <p>The LOCKED state is entered externally by {@link AlarmProject} when
 * {@link PinValidator#isLockedOut()} returns {@code true}; it is not a pure
 * FSM transition because it depends on a counter maintained outside the FSM.</p>
 */
public enum AlarmState {

    /**
     * The alarm is off and no motion monitoring is active.
     * This is the initial state after power-on or after a correct PIN while TRIGGERED.
     */
    DISARMED,

    /**
     * The alarm is actively monitoring the PIR sensor.
     * A motion detection event will transition the system to TRIGGERED.
     */
    ARMED,

    /**
     * Motion was detected while ARMED.
     * The siren is playing and the relay is active.
     * Entering the correct PIN disarms the system and returns to DISARMED.
     */
    TRIGGERED,

    /**
     * The keypad is locked out due to three consecutive wrong PIN attempts.
     * No PIN entry is accepted until the lockout timer expires.
     * After expiry the system returns to DISARMED.
     */
    LOCKED
}
