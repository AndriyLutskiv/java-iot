package ua.crowpi.projects.p03;

/**
 * Represents the current playback state of the music box.
 *
 * <p>The state is used by {@link MusicBoxProject} to decide how to update
 * the LCD display and the RGB LED between iterations of the main loop:</p>
 * <ul>
 *   <li>{@link #IDLE}    — no melody is playing; LCD shows a welcome prompt.</li>
 *   <li>{@link #PLAYING} — a melody thread is running; LCD shows name + progress bar.</li>
 *   <li>{@link #PAUSED}  — playback was stopped mid-melody; LCD shows "STOPPED".</li>
 * </ul>
 */
public enum PlaybackState {

    /**
     * The music box is idle — no melody has been selected yet or the last
     * melody finished naturally.
     */
    IDLE("IDLE"),

    /**
     * A melody is currently being played by {@link MelodyPlayer}.
     */
    PLAYING("PLAYING"),

    /**
     * Playback was explicitly interrupted via IR button 0 ({@code stop} command).
     */
    PAUSED("STOPPED");

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /** Short display label shown on the LCD when in this state. */
    private final String label;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a PlaybackState with the given display label.
     *
     * @param label short string suitable for the 16-char LCD display
     */
    PlaybackState(String label) {
        this.label = label;
    }

    // -------------------------------------------------------------------------
    // Accessor
    // -------------------------------------------------------------------------

    /**
     * Returns the short display label for this state.
     *
     * @return label string, never {@code null}
     */
    public String getLabel() {
        return label;
    }
}
