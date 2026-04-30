package ua.crowpi.projects.p03;

/**
 * Immutable value object that represents a single musical note (or a rest).
 *
 * <p>A note with {@code frequencyHz == 0} is treated as a rest (silence) —
 * see {@link #isRest()}. All other frequencies must be in the audible range
 * 20–20 000 Hz; no runtime validation is enforced here so that
 * {@link MelodyLibrary} can construct notes freely without try/catch overhead.</p>
 *
 * <p>Instances are created once in {@link MelodyLibrary} and reused read-only
 * across threads — no synchronisation is required.</p>
 */
public final class Note {

    /** Frequency of the note in Hertz. {@code 0} denotes a rest. */
    private final int frequencyHz;

    /** Duration the note (or rest) should be held, in milliseconds. */
    private final int durationMs;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a new Note.
     *
     * @param frequencyHz PWM carrier frequency in Hertz; use {@code 0} for a rest
     * @param durationMs  how long to play the note, in milliseconds
     */
    public Note(int frequencyHz, int durationMs) {
        this.frequencyHz = frequencyHz;
        this.durationMs  = durationMs;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the note's frequency in Hertz.
     *
     * @return frequency in Hz, or {@code 0} if this is a rest
     */
    public int getFrequencyHz() {
        return frequencyHz;
    }

    /**
     * Returns how long the note should be played.
     *
     * @return duration in milliseconds
     */
    public int getDurationMs() {
        return durationMs;
    }

    /**
     * Returns {@code true} when this note is a rest (silence).
     *
     * <p>A frequency of exactly {@code 0} means no sound should be emitted — the
     * buzzer PWM is disabled and the RGB LED is turned off for the duration.</p>
     *
     * @return {@code true} if {@code frequencyHz == 0}
     */
    public boolean isRest() {
        // Нульова частота — умовна позначка тиші, а не реальна звукова нота
        return frequencyHz == 0;
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    /**
     * Returns a human-readable representation, e.g. {@code "Note{freq=440Hz, dur=300ms}"}.
     *
     * @return string representation of this note
     */
    @Override
    public String toString() {
        if (isRest()) {
            return "Note{REST, dur=" + durationMs + "ms}";
        }
        return "Note{freq=" + frequencyHz + "Hz, dur=" + durationMs + "ms}";
    }
}
