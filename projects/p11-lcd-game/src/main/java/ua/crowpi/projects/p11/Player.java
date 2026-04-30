package ua.crowpi.projects.p11;

/**
 * Represents the player character in the LCD Platform Game.
 *
 * <p>The player occupies a single cell on the 16×2 LCD grid.
 * Row 0 is the top row (air / high platforms) and row 1 is the
 * bottom row (floor / ground level).  The jump mechanic uses a
 * countdown timer ({@code jumpTicksRemaining}) rather than
 * continuous velocity so it maps cleanly to the 2-row display.</p>
 */
public class Player {

    // Поточна позиція на LCD-сітці (0-15 по X, 0-1 по Y)
    private int x;
    private int y;

    // Кількість тіків, що залишилась у повітрі після стрибка
    private int jumpTicksRemaining;

    // Вертикальна швидкість (використовується для анімації падіння)
    private int velocityY;

    // Кількість очок і здоров'я гравця
    private int health;
    private int score;

    // Прапор живого/мертвого — false після втрати останнього HP
    private boolean alive;

    /**
     * Constructs a new Player at the specified starting position.
     *
     * @param startX initial column (0–15)
     * @param startY initial row (0–1)
     */
    public Player(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        // Ініціалізація решти полів через reset() для уникнення дублювання логіки
        reset();
    }

    /**
     * Resets the player to the default starting state: position (1, 1),
     * full health (3), zero score, not jumping, alive.
     */
    public void reset() {
        // Стандартна стартова позиція — другий стовпчик, нижній рядок (підлога)
        this.x = 1;
        this.y = 1;
        this.velocityY = 0;
        this.jumpTicksRemaining = 0;
        this.health = 3;
        this.score = 0;
        this.alive = true;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    /**
     * Returns the current column position of the player.
     *
     * @return column index in range 0–15
     */
    public int getX() {
        return x;
    }

    /**
     * Sets the column position of the player.
     *
     * @param x column index in range 0–15
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Returns the current row position of the player.
     *
     * @return row index: 0 = top row, 1 = bottom row
     */
    public int getY() {
        return y;
    }

    /**
     * Sets the row position of the player.
     *
     * @param y row index: 0 = top row, 1 = bottom row
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Returns the remaining ticks the player stays airborne after a jump.
     *
     * @return jump ticks remaining (0 means not jumping)
     */
    public int getJumpTicksRemaining() {
        return jumpTicksRemaining;
    }

    /**
     * Sets the jump countdown timer.
     *
     * @param jumpTicksRemaining number of ticks to stay at y=0 before falling
     */
    public void setJumpTicksRemaining(int jumpTicksRemaining) {
        this.jumpTicksRemaining = jumpTicksRemaining;
    }

    /**
     * Returns the vertical velocity of the player.
     *
     * @return velocityY value
     */
    public int getVelocityY() {
        return velocityY;
    }

    /**
     * Sets the vertical velocity of the player.
     *
     * @param velocityY new vertical velocity
     */
    public void setVelocityY(int velocityY) {
        this.velocityY = velocityY;
    }

    /**
     * Returns the current health points of the player.
     *
     * @return health value (typically 0–3)
     */
    public int getHealth() {
        return health;
    }

    /**
     * Sets the health points of the player.
     *
     * @param health new health value
     */
    public void setHealth(int health) {
        this.health = health;
    }

    /**
     * Returns the current score of the player (number of coins collected).
     *
     * @return score value
     */
    public int getScore() {
        return score;
    }

    /**
     * Sets the player score.
     *
     * @param score new score value
     */
    public void setScore(int score) {
        this.score = score;
    }

    /**
     * Returns whether the player is currently alive.
     *
     * @return {@code true} if alive; {@code false} if health reached zero
     */
    public boolean isAlive() {
        return alive;
    }

    /**
     * Sets the alive flag for the player.
     *
     * @param alive {@code true} to mark player as alive
     */
    public void setAlive(boolean alive) {
        this.alive = alive;
    }
}
