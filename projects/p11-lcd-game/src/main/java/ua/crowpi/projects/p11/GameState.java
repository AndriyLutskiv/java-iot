package ua.crowpi.projects.p11;

/**
 * Represents the possible states of the LCD Platform Game.
 *
 * <p>The game follows a linear state machine:
 * MENU → PLAYING ↔ PAUSED → GAME_OVER or WIN.</p>
 */
public enum GameState {

    /** Main menu is displayed, waiting for the player to start the game. */
    MENU("MENU"),

    /** The game is actively running; the game loop processes physics and input. */
    PLAYING("PLAYING"),

    /** Game is temporarily halted; all updates are frozen. */
    PAUSED("PAUSED"),

    /** The player has lost all health; the game-over screen is shown. */
    GAME_OVER("GAME OVER"),

    /** The player has collected all coins; the win screen is shown. */
    WIN("YOU WIN!");

    // Рядок для відображення на LCD — враховує обмеження 16 символів
    private final String label;

    /**
     * Constructs a GameState with the given display label.
     *
     * @param label human-readable label shown on the LCD screen
     */
    GameState(String label) {
        this.label = label;
    }

    /**
     * Returns the human-readable label for this state, suitable for LCD display.
     *
     * @return display label string
     */
    public String getLabel() {
        return label;
    }
}
