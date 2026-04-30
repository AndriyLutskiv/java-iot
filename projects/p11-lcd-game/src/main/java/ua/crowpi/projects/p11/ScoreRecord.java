package ua.crowpi.projects.p11;

/**
 * A single leaderboard entry storing a player name, score, and date.
 *
 * <p>This class is serialised/deserialised by Jackson's {@code ObjectMapper}
 * via the {@link LeaderBoard} class, so it requires a no-argument constructor
 * and public getters/setters that follow the JavaBeans convention.</p>
 */
public class ScoreRecord {

    private String playerName;
    private int    score;
    private String date;   // ISO-8601 date string, e.g. "2024-01-15T14:23:11"

    /**
     * No-arg constructor required by Jackson for JSON deserialisation.
     */
    public ScoreRecord() { }

    /**
     * Creates a ScoreRecord with all fields set.
     *
     * @param playerName the player's name (1–16 characters)
     * @param score      the final score (number of coins collected)
     * @param date       ISO-8601 timestamp of when the score was achieved
     */
    public ScoreRecord(String playerName, int score, String date) {
        this.playerName = playerName;
        this.score      = score;
        this.date       = date;
    }

    /** Returns the player name. @return player name */
    public String getPlayerName() { return playerName; }

    /** Sets the player name. @param playerName new name */
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    /** Returns the score. @return score */
    public int getScore() { return score; }

    /** Sets the score. @param score new score */
    public void setScore(int score) { this.score = score; }

    /** Returns the ISO date string. @return date */
    public String getDate() { return date; }

    /** Sets the date string. @param date ISO date */
    public void setDate(String date) { this.date = date; }

    @Override
    public String toString() {
        return playerName + " | " + score + " pts | " + date;
    }
}
