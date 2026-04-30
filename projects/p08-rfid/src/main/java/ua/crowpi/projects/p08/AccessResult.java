package ua.crowpi.projects.p08;

/**
 * Represents the outcome of an RFID card access attempt.
 *
 * <p>Each constant carries a fixed 16-character message string suitable for display
 * on the CrowPi 16×2 LCD. The message is padded with trailing spaces so that the
 * entire second LCD row is always overwritten without artefacts from a previous message.</p>
 *
 * <p>Usage in {@link AccessController}:</p>
 * <ul>
 *   <li>{@link #GRANTED}      — card found and active; relay opens the door, green LED</li>
 *   <li>{@link #DENIED}       — card found but deactivated; yellow LED, no relay action</li>
 *   <li>{@link #UNKNOWN_CARD} — UID not in database; red LED, buzzer beep</li>
 * </ul>
 */
public enum AccessResult {

    /**
     * The presented card is registered and active.
     * The relay unlocks the door for a short duration.
     */
    GRANTED("ACCESS GRANTED  "),

    /**
     * The presented card is registered but has been deactivated by an administrator.
     * The door remains locked.
     */
    DENIED("CARD DEACTIVATED"),

    /**
     * The presented card UID is not in the {@link CardDatabase}.
     * The buzzer sounds a short warning beep.
     */
    UNKNOWN_CARD("UNKNOWN CARD    ");

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** 16-character LCD message, padded to exactly fill one display row. */
    private final String message;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Initialises the enum constant with its associated LCD message.
     *
     * @param message exactly 16 characters to display on the LCD second row
     */
    AccessResult(String message) {
        // Зберігаємо незмінне повідомлення — рядок фіксованої довжини для LCD
        this.message = message;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the fixed-width (16 characters) LCD message associated with this result.
     *
     * <p>The returned string is always exactly 16 characters long, padded with spaces
     * on the right, ensuring it completely overwrites the previous LCD content.</p>
     *
     * @return 16-character message string for the CrowPi LCD second row
     */
    public String getMessage() {
        return message;
    }
}
