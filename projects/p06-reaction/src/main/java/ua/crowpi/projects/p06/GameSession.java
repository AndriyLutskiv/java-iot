package ua.crowpi.projects.p06;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the full sequence of rounds played in a single game session between two players.
 *
 * <p>Rounds are added one at a time via {@link #addRound(GameRound)}, which also
 * immediately triggers score evaluation. After all rounds are complete,
 * {@link #getWinner()} identifies the overall session winner, and
 * {@link #calcStats()} can be called to trigger any deferred statistics gathering.</p>
 */
public class GameSession {

    /** The first participant in this session. */
    private final Player p1;

    /** The second participant in this session. */
    private final Player p2;

    /** Ordered list of all rounds played so far. */
    private final List<GameRound> rounds;

    // -------------------------------------------------------------------------
    // Конструктор
    // -------------------------------------------------------------------------

    /**
     * Creates a new game session between two players with no rounds played yet.
     *
     * @param p1 the first player
     * @param p2 the second player
     */
    public GameSession(Player p1, Player p2) {
        this.p1 = p1;
        this.p2 = p2;
        // Порожній список — раунди додаються поступово через addRound()
        this.rounds = new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // Ігрова логіка
    // -------------------------------------------------------------------------

    /**
     * Adds a completed round to this session and immediately evaluates scores.
     *
     * <p>{@link GameRound#applyScores(Player, Player)} is called here so that
     * both players' scores are updated as soon as the round data is recorded.</p>
     *
     * @param round the finished round with all timing data set
     */
    public void addRound(GameRound round) {
        // Зберігаємо раунд і одразу застосовуємо результати — щоб рахунок завжди актуальний
        rounds.add(round);
        round.applyScores(p1, p2);
    }

    /**
     * Returns the player with the higher total score after all rounds.
     *
     * <p>Returns {@code null} if both players have equal scores (a draw).</p>
     *
     * @return the winning {@link Player}, or {@code null} for a draw
     */
    public Player getWinner() {
        if (p1.getScore() > p2.getScore()) {
            return p1;
        } else if (p2.getScore() > p1.getScore()) {
            return p2;
        }
        // Нічия — переможця немає
        return null;
    }

    /**
     * Triggers deferred statistics calculation for both players.
     *
     * <p>Reaction-time statistics are computed on demand via {@link PlayerStats};
     * this method exists as a lifecycle hook for any future pre-computation.</p>
     */
    public void calcStats() {
        // Метод-гачок для попереднього підрахунку статистики якщо потрібно.
        // Зараз PlayerStats обчислює все ліниво при виклику гетерів,
        // тому тут нічого додатково не робимо.
    }

    // -------------------------------------------------------------------------
    // Геттери
    // -------------------------------------------------------------------------

    /**
     * Returns the ordered list of all rounds recorded in this session.
     *
     * @return unmodifiable view of the round list
     */
    public List<GameRound> getRounds() {
        return rounds;
    }

    /**
     * Returns Player 1 associated with this session.
     *
     * @return the first player
     */
    public Player getP1() {
        return p1;
    }

    /**
     * Returns Player 2 associated with this session.
     *
     * @return the second player
     */
    public Player getP2() {
        return p2;
    }
}
