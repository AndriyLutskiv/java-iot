package ua.crowpi.projects.p04;

/**
 * Represents the events that can drive the alarm system FSM.
 *
 * <p>Events are produced by hardware components and fed into {@link AlarmFsm#transition(AlarmEvent)}
 * to advance the state machine to the next state. Not all events are meaningful in
 * every state; undefined transitions are silently ignored by the FSM.</p>
 *
 * <p>Event sources:</p>
 * <ul>
 *   <li>{@link #PIR_TRIGGERED} — PIR sensor GPIO pin goes HIGH</li>
 *   <li>{@link #CORRECT_PIN}   — {@link PinValidator#validate(String)} returns {@code true}</li>
 *   <li>{@link #WRONG_PIN}     — {@link PinValidator#validate(String)} returns {@code false}</li>
 *   <li>{@link #LOCK_EXPIRED}  — lockout timer scheduled by {@link AlarmProject} fires</li>
 * </ul>
 */
public enum AlarmEvent {

    /**
     * The PIR motion sensor detected movement.
     * Only meaningful in the ARMED state; in other states it is ignored by the FSM.
     */
    PIR_TRIGGERED,

    /**
     * The user entered a PIN that matches the stored SHA-256 hash.
     * Causes disarm from ARMED or TRIGGERED, and arm from DISARMED.
     */
    CORRECT_PIN,

    /**
     * The user entered a PIN that does NOT match the stored hash.
     * Tracked by {@link PinValidator}; after three consecutive failures
     * {@link AlarmProject} transitions the system to LOCKED.
     */
    WRONG_PIN,

    /**
     * The lockout timer expired after the configured lockout duration.
     * Transitions the system from LOCKED back to DISARMED.
     */
    LOCK_EXPIRED
}
