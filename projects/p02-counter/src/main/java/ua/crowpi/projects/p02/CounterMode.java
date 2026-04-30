package ua.crowpi.projects.p02;

/**
 * Operating mode of the motion-event counter.
 *
 * <p>The mode determines whether a PIR trigger increments or decrements the
 * counter value. Button2 on the CrowPi board toggles between the two modes
 * at runtime without interrupting the session.</p>
 */
public enum CounterMode {

    /** Counter increases by 1 on every PIR detection event. */
    COUNT_UP("Count Up"),

    /** Counter decreases by 1 on every PIR detection event. */
    COUNTDOWN("Countdown");

    // Зберігаємо відображувану назву окремо, щоб легко виводити у лог і на дисплей
    private final String label;

    /**
     * Associates a human-readable label with each mode constant.
     *
     * @param label the display label shown in logs and UI output
     */
    CounterMode(String label) {
        this.label = label;
    }

    /**
     * Returns the human-readable display label for this mode.
     *
     * @return display label string, e.g. {@code "Count Up"} or {@code "Countdown"}
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the opposite operating mode.
     *
     * <p>This is the core of the toggle-button logic: calling {@code toggle()} on
     * the current mode always produces the other one without any conditional code
     * in the caller.</p>
     *
     * @return {@link #COUNTDOWN} when called on {@link #COUNT_UP}, and vice-versa
     */
    public CounterMode toggle() {
        // Перемикаємо режим: якщо рахуємо вгору — переходимо на зворотний відлік, і навпаки
        return this == COUNT_UP ? COUNTDOWN : COUNT_UP;
    }
}
