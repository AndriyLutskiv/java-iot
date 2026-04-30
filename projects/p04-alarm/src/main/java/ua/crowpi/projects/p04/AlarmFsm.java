package ua.crowpi.projects.p04;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Finite State Machine (FSM) for the CrowPi alarm system.
 *
 * <p>The transition table is built once in the constructor as an immutable
 * {@code Map<AlarmState, Map<AlarmEvent, AlarmState>>}. Calling
 * {@link #transition(AlarmEvent)} looks up the current state's event map and
 * returns the next state, or returns the current state unchanged if the
 * combination is not in the table (i.e. illegal transitions are silently ignored
 * rather than throwing, which is the safe behaviour for embedded systems).</p>
 *
 * <p>Thread-safety note: this class is <em>not</em> thread-safe. The caller
 * ({@link AlarmProject}) must ensure that {@link #transition(AlarmEvent)} and
 * state inspection are called from a single thread.</p>
 *
 * <p>Defined transitions:</p>
 * <pre>
 *   DISARMED + CORRECT_PIN   → ARMED
 *   ARMED    + CORRECT_PIN   → DISARMED
 *   ARMED    + PIR_TRIGGERED → TRIGGERED
 *   TRIGGERED+ CORRECT_PIN   → DISARMED
 *   LOCKED   + LOCK_EXPIRED  → DISARMED
 * </pre>
 */
public class AlarmFsm {

    // Таблиця переходів: поточний стан → (подія → наступний стан)
    private final Map<AlarmState, Map<AlarmEvent, AlarmState>> transitions;

    // Поточний стан автомату; volatile не потрібен — клас не є thread-safe за контрактом
    private AlarmState currentState;

    /**
     * Creates a new AlarmFsm with the full transition table and sets the initial state to DISARMED.
     */
    public AlarmFsm() {
        // Будуємо таблицю переходів один раз при ініціалізації.
        // EnumMap обирається навмисно — O(1) lookup і компактна пам'ять для enum-ключів
        Map<AlarmState, Map<AlarmEvent, AlarmState>> table =
                new EnumMap<>(AlarmState.class);

        // --- Переходи зі стану DISARMED ---
        Map<AlarmEvent, AlarmState> fromDisarmed = new EnumMap<>(AlarmEvent.class);
        // Правильний PIN зі знятою охороною — ставимо на охорону
        fromDisarmed.put(AlarmEvent.CORRECT_PIN, AlarmState.ARMED);
        table.put(AlarmState.DISARMED, Collections.unmodifiableMap(fromDisarmed));

        // --- Переходи зі стану ARMED ---
        Map<AlarmEvent, AlarmState> fromArmed = new EnumMap<>(AlarmEvent.class);
        // Правильний PIN на охороні — знімаємо охорону
        fromArmed.put(AlarmEvent.CORRECT_PIN, AlarmState.DISARMED);
        // PIR спрацював — переходимо до стану тривоги
        fromArmed.put(AlarmEvent.PIR_TRIGGERED, AlarmState.TRIGGERED);
        table.put(AlarmState.ARMED, Collections.unmodifiableMap(fromArmed));

        // --- Переходи зі стану TRIGGERED ---
        Map<AlarmEvent, AlarmState> fromTriggered = new EnumMap<>(AlarmEvent.class);
        // Правильний PIN при тривозі — скидаємо тривогу і знімаємо охорону
        fromTriggered.put(AlarmEvent.CORRECT_PIN, AlarmState.DISARMED);
        table.put(AlarmState.TRIGGERED, Collections.unmodifiableMap(fromTriggered));

        // --- Переходи зі стану LOCKED ---
        Map<AlarmEvent, AlarmState> fromLocked = new EnumMap<>(AlarmEvent.class);
        // Таймер блокування вичерпався — повертаємося до знятого стану
        fromLocked.put(AlarmEvent.LOCK_EXPIRED, AlarmState.DISARMED);
        table.put(AlarmState.LOCKED, Collections.unmodifiableMap(fromLocked));

        // Робимо зовнішній Map незмінним для захисту від випадкової модифікації
        this.transitions = Collections.unmodifiableMap(table);

        // Початковий стан — охорона знята
        this.currentState = AlarmState.DISARMED;
    }

    /**
     * Applies the given event to the current state and advances the FSM.
     *
     * <p>If no transition is defined for the (currentState, event) pair, the
     * current state is returned unchanged. This "ignore undefined" policy ensures
     * that spurious sensor signals never crash or deadlock the system.</p>
     *
     * @param event the event to process
     * @return the new state after applying the event (may equal the old state)
     */
    public AlarmState transition(AlarmEvent event) {
        // Шукаємо рядок таблиці для поточного стану
        Map<AlarmEvent, AlarmState> row = transitions.get(currentState);

        if (row != null) {
            // Якщо для цієї події є перехід — застосовуємо його
            AlarmState next = row.get(event);
            if (next != null) {
                currentState = next;
            }
            // Якщо переходу немає — ігноруємо подію (залишаємось у поточному стані)
        }

        return currentState;
    }

    /**
     * Returns the current state of the FSM.
     *
     * @return the current {@link AlarmState}; never {@code null}
     */
    public AlarmState getState() {
        return currentState;
    }

    /**
     * Resets the FSM to the initial DISARMED state.
     *
     * <p>Used during system restart or after a fatal error to bring the machine
     * back to a known-good state without creating a new instance.</p>
     */
    public void reset() {
        // Скидаємо до початкового стану — охорона знята
        currentState = AlarmState.DISARMED;
    }

    /**
     * Directly sets the FSM to the specified state, bypassing the transition table.
     *
     * <p>Package-private: intended only for testing and for exceptional use by
     * {@link AlarmProject} when the LOCKED state must be set externally after
     * three consecutive wrong PINs (which is a condition tracked outside the FSM).</p>
     *
     * @param state the state to forcibly set
     */
    void setState(AlarmState state) {
        // Пряме встановлення стану — використовується лише коли логіка поза FSM
        // (наприклад, лічильник невірних PIN) вимагає примусового переходу
        this.currentState = state;
    }
}
