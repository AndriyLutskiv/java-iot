package ua.crowpi.projects.p03;

import java.util.Collections;
import java.util.List;

/**
 * Immutable value object representing a named sequence of {@link Note}s.
 *
 * <p>A melody holds a display name (at most 16 characters so it fits on the LCD
 * first row without truncation) and an ordered list of notes that {@link MelodyPlayer}
 * plays in sequence.</p>
 *
 * <p>The note list is wrapped in an unmodifiable view so callers cannot
 * accidentally mutate the shared {@link MelodyLibrary} data.</p>
 */
public final class Melody {

    /** Human-readable name shown on the LCD (up to 16 chars). */
    private final String name;

    /** Ordered sequence of notes that make up this melody. */
    private final List<Note> notes;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a new Melody.
     *
     * @param name  display name, e.g. {@code "C MAJOR SCALE"}
     * @param notes ordered list of notes; must not be {@code null} or empty
     */
    public Melody(String name, List<Note> notes) {
        this.name  = name;
        // Загортаємо список у незмінний вигляд — безпечна передача між потоками
        this.notes = Collections.unmodifiableList(notes);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the display name of this melody.
     *
     * @return melody name, never {@code null}
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the ordered list of notes in this melody.
     *
     * <p>The returned list is unmodifiable — attempts to add or remove elements
     * will throw {@link UnsupportedOperationException}.</p>
     *
     * @return unmodifiable list of notes
     */
    public List<Note> getNotes() {
        return notes;
    }

    // -------------------------------------------------------------------------
    // Derived metrics
    // -------------------------------------------------------------------------

    /**
     * Returns the total playback duration of this melody in milliseconds.
     *
     * <p>Computed as the sum of {@link Note#getDurationMs()} across all notes
     * including rests. The result is always positive for a non-empty melody.</p>
     *
     * @return sum of all note durations in milliseconds
     */
    public int getTotalDurationMs() {
        int total = 0;
        // Підсумовуємо тривалості всіх нот — паузи (rests) теж мають тривалість
        for (Note note : notes) {
            total += note.getDurationMs();
        }
        return total;
    }
}
