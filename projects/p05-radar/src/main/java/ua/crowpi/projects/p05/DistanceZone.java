package ua.crowpi.projects.p05;

/**
 * Classification of a distance reading into one of three operational zones.
 *
 * <p>Zone boundaries follow common HC-SR04 practice used in robotics and
 * obstacle-avoidance systems:</p>
 * <ul>
 *   <li>{@link #CLEAR}   — object is far away (more than 50 cm), safe to proceed</li>
 *   <li>{@link #WARNING} — object is approaching (20–50 cm inclusive), caution needed</li>
 *   <li>{@link #DANGER}  — object is very close (less than 20 cm), stop or alert</li>
 * </ul>
 *
 * <p>The RGB LED colour associated with each zone:</p>
 * <ul>
 *   <li>CLEAR   → green (R=LOW, G=HIGH, B=LOW)</li>
 *   <li>WARNING → yellow (R=HIGH, G=HIGH, B=LOW)</li>
 *   <li>DANGER  → red blinking (R=HIGH, G=LOW, B=LOW)</li>
 * </ul>
 */
public enum DistanceZone {

    /**
     * The measured distance exceeds 50 cm — the path is clear.
     * RGB LED colour: green.
     */
    CLEAR("CLEAR"),

    /**
     * The measured distance is between 20 cm and 50 cm (inclusive) — caution zone.
     * RGB LED colour: yellow.
     */
    WARNING("WARNING"),

    /**
     * The measured distance is less than 20 cm — danger, object is very close.
     * RGB LED colour: red (blinks to attract attention).
     */
    DANGER("DANGER");

    // Текстова мітка зони для відображення на LCD та у JSON-звіті
    private final String label;

    /**
     * Enum constructor — stores the display label for each zone.
     *
     * @param label short human-readable zone name
     */
    DistanceZone(String label) {
        this.label = label;
    }

    /**
     * Classifies a raw distance reading into the appropriate zone.
     *
     * <p>Boundary rule: {@code distanceCm < 20} is DANGER, {@code distanceCm <= 50}
     * is WARNING, and anything above 50 cm is CLEAR.</p>
     *
     * @param distanceCm distance in centimetres, expected in range [2.0, 400.0]
     * @return the matching {@link DistanceZone}; never {@code null}
     */
    public static DistanceZone forDistance(double distanceCm) {
        // Найближча небезпечна зона — менше 20 см, об'єкт дуже близько
        if (distanceCm < 20.0) {
            return DANGER;
        }
        // Зона попередження — від 20 до 50 см включно, потрібна обережність
        if (distanceCm <= 50.0) {
            return WARNING;
        }
        // Понад 50 см — шлях вільний
        return CLEAR;
    }

    /**
     * Returns the display label for this zone (e.g. {@code "CLEAR"}, {@code "WARNING"}).
     *
     * @return zone label string, never {@code null}
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns {@code true} only for the {@link #DANGER} zone, enabling callers
     * to branch on critical conditions without an explicit enum comparison.
     *
     * @return {@code true} if this zone is {@code DANGER}, {@code false} otherwise
     */
    public boolean isDanger() {
        // Лише DANGER вимагає миготіння LED та максимально гучної тривоги
        return this == DANGER;
    }
}
