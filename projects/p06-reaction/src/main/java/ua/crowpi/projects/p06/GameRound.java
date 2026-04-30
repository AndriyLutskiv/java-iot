package ua.crowpi.projects.p06;

/**
 * Immutable record of a single round in the Reaction Speed Trainer game.
 *
 * <p>Stores the exact millisecond timestamps of the stimulus flash and each
 * player's button press (0 = did not press). After construction, call
 * {@link #applyScores(Player, Player)} to evaluate the round, assign
 * point deltas to the players and record the winner field.</p>
 */
public class GameRound {

    /**
     * Wall-clock time (ms) at which the RGB LED stimulus was activated.
     * Established by {@link StimulusGenerator#generate()}.
     */
    private final long stimulusTimeMs;

    /**
     * Wall-clock time (ms) at which Player 1 pressed their button.
     * {@code 0} indicates the player did not press during the response window.
     */
    private final long p1PressTimeMs;

    /**
     * Wall-clock time (ms) at which Player 2 pressed their button.
     * {@code 0} indicates the player did not press during the response window.
     */
    private final long p2PressTimeMs;

    /**
     * The player who earned +1 this round; {@code null} for a draw, a
     * double-false-start, or when neither player pressed.
     */
    private Player winner;

    // -------------------------------------------------------------------------
    // Конструктор
    // -------------------------------------------------------------------------

    /**
     * Creates a new GameRound capturing the raw timing data for one round.
     *
     * <p>Call {@link #applyScores(Player, Player)} after construction to evaluate
     * false starts, determine the winner and update the Player objects.</p>
     *
     * @param stimulusTimeMs wall-clock ms when the stimulus fired
     * @param p1PressTimeMs  wall-clock ms when P1 pressed; {@code 0} = no press
     * @param p2PressTimeMs  wall-clock ms when P2 pressed; {@code 0} = no press
     */
    public GameRound(long stimulusTimeMs, long p1PressTimeMs, long p2PressTimeMs) {
        this.stimulusTimeMs = stimulusTimeMs;
        this.p1PressTimeMs  = p1PressTimeMs;
        this.p2PressTimeMs  = p2PressTimeMs;
        // winner залишається null до явного виклику applyScores()
        this.winner = null;
    }

    // -------------------------------------------------------------------------
    // Бізнес-логіка
    // -------------------------------------------------------------------------

    /**
     * Evaluates the round and applies score deltas to both players.
     *
     * <p>Rules applied in order:</p>
     * <ol>
     *   <li>If P1 pressed <em>before</em> the stimulus ({@code p1PressTimeMs > 0 &&
     *       p1PressTimeMs < stimulusTimeMs}) → P1 gets -1 (false start).</li>
     *   <li>If P2 pressed <em>before</em> the stimulus → P2 gets -1 (false start).</li>
     *   <li>Among players who did <em>not</em> false-start and did press after the
     *       stimulus, the one with the smaller press time wins and gets +1. If
     *       both qualify, the earlier press wins. If only one qualifies, that
     *       player wins.</li>
     *   <li>If neither player pressed (both 0) or both false-started, no winner
     *       is assigned and no +1 is given.</li>
     * </ol>
     *
     * <p>Reaction times for valid (non-false-start) presses are recorded via
     * {@link Player#addReactionTime(long)}.</p>
     *
     * @param p1 Player 1 object whose score and reaction times will be updated
     * @param p2 Player 2 object whose score and reaction times will be updated
     */
    public void applyScores(Player p1, Player p2) {
        // Перевіряємо фальстарт для кожного гравця окремо
        boolean p1FalseStart = (p1PressTimeMs > 0 && p1PressTimeMs < stimulusTimeMs);
        boolean p2FalseStart = (p2PressTimeMs > 0 && p2PressTimeMs < stimulusTimeMs);

        if (p1FalseStart) {
            // Фальстарт: гравець натиснув ДО спалаху — штрафуємо
            p1.addScore(-1);
        }
        if (p2FalseStart) {
            // Фальстарт: гравець натиснув ДО спалаху — штрафуємо
            p2.addScore(-1);
        }

        // Визначаємо, чи натиснув кожен гравець ПІСЛЯ стимулу (і без фальстарту)
        boolean p1Valid = !p1FalseStart && (p1PressTimeMs > 0) && (p1PressTimeMs >= stimulusTimeMs);
        boolean p2Valid = !p2FalseStart && (p2PressTimeMs > 0) && (p2PressTimeMs >= stimulusTimeMs);

        if (p1Valid && p2Valid) {
            // Обоє натиснули правильно — перемагає той, хто натиснув швидше
            if (p1PressTimeMs <= p2PressTimeMs) {
                p1.addScore(1);
                p1.addReactionTime(p1PressTimeMs - stimulusTimeMs);
                winner = p1;
            } else {
                p2.addScore(1);
                p2.addReactionTime(p2PressTimeMs - stimulusTimeMs);
                winner = p2;
            }
        } else if (p1Valid) {
            // Тільки P1 натиснув правильно — P1 виграє раунд
            p1.addScore(1);
            p1.addReactionTime(p1PressTimeMs - stimulusTimeMs);
            winner = p1;
        } else if (p2Valid) {
            // Тільки P2 натиснув правильно — P2 виграє раунд
            p2.addScore(1);
            p2.addReactionTime(p2PressTimeMs - stimulusTimeMs);
            winner = p2;
        }
        // Якщо ніхто не натиснув або обидва отримали фальстарт — winner залишається null
    }

    // -------------------------------------------------------------------------
    // Геттери
    // -------------------------------------------------------------------------

    /**
     * Returns the wall-clock time (ms) at which the stimulus LED was activated.
     *
     * @return stimulus timestamp in milliseconds
     */
    public long getStimulusTimeMs() {
        return stimulusTimeMs;
    }

    /**
     * Returns the wall-clock time (ms) at which Player 1 pressed their button.
     *
     * @return P1 press timestamp; {@code 0} if P1 did not press
     */
    public long getP1PressTimeMs() {
        return p1PressTimeMs;
    }

    /**
     * Returns the wall-clock time (ms) at which Player 2 pressed their button.
     *
     * @return P2 press timestamp; {@code 0} if P2 did not press
     */
    public long getP2PressTimeMs() {
        return p2PressTimeMs;
    }

    /**
     * Returns the player who earned +1 in this round.
     *
     * @return the winning {@link Player}, or {@code null} if the round had no winner
     *         (double false start, draw, or no presses)
     */
    public Player getWinner() {
        return winner;
    }
}
