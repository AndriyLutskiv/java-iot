package ua.crowpi.projects.p06;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single participant in the Reaction Speed Trainer game.
 *
 * <p>Holds the player's display name, accumulated score across all rounds,
 * and a history of individual reaction times in milliseconds collected
 * during the game session.</p>
 */
public class Player {

    /** Human-readable display name shown on the LCD and in results. */
    private final String name;

    /** Total score: +1 per win, -1 per false start, 0 for draw/no-press. */
    private int score;

    /** Ordered list of reaction times (ms) recorded for this player. */
    private final List<Long> reactionTimesMs;

    // -------------------------------------------------------------------------
    // Конструктор
    // -------------------------------------------------------------------------

    /**
     * Creates a new player with the given name, zero score, and an empty
     * reaction-time history.
     *
     * @param name the player's display name (e.g. "PLAYER 1")
     */
    public Player(String name) {
        this.name = name;
        // Ініціалізуємо порожнім списком — він наповнюється після кожного раунду
        this.score = 0;
        this.reactionTimesMs = new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // Методи для зміни стану
    // -------------------------------------------------------------------------

    /**
     * Adds {@code delta} to the player's score.
     *
     * <p>Pass +1 for a successful win, -1 for a false start penalty.</p>
     *
     * @param delta score adjustment (may be negative)
     */
    public void addScore(int delta) {
        // Накопичуємо рахунок: може йти як вгору (виграш), так і вниз (фальстарт)
        this.score += delta;
    }

    /**
     * Records a single reaction time for this player.
     *
     * @param ms reaction time in milliseconds measured from stimulus onset
     *           to button press
     */
    public void addReactionTime(long ms) {
        reactionTimesMs.add(ms);
    }

    // -------------------------------------------------------------------------
    // Геттери та сеттери
    // -------------------------------------------------------------------------

    /**
     * Returns the player's display name.
     *
     * @return name string, never {@code null}
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the player's current total score.
     *
     * @return accumulated score (can be negative if multiple false starts occurred)
     */
    public int getScore() {
        return score;
    }

    /**
     * Sets the player's score to an explicit value.
     *
     * <p>Normally only used for deserialization or test setup; prefer
     * {@link #addScore(int)} during gameplay.</p>
     *
     * @param score new score value
     */
    public void setScore(int score) {
        this.score = score;
    }

    /**
     * Returns a live reference to the list of reaction times recorded for this player.
     *
     * <p>The returned list is mutable; callers may iterate but should not
     * modify it directly — use {@link #addReactionTime(long)} instead.</p>
     *
     * @return mutable list of reaction times in milliseconds
     */
    public List<Long> getReactionTimesMs() {
        return reactionTimesMs;
    }
}
