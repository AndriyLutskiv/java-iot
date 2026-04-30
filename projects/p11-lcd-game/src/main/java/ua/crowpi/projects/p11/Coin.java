package ua.crowpi.projects.p11;

/**
 * Represents a collectible coin on the LCD game grid.
 *
 * <p>A coin is a single-cell collectible that disappears once the player
 * occupies its cell.  Collecting all coins wins the game.</p>
 */
public class Coin {

    private final int x;
    private final int y;
    private boolean collected;

    /**
     * Constructs a Coin at the given grid position.
     *
     * @param x column (0–15)
     * @param y row (0–1)
     */
    public Coin(int x, int y) {
        this.x = x;
        this.y = y;
        this.collected = false;
    }

    /**
     * Marks this coin as collected.
     *
     * <p>Idempotent — calling collect() on an already-collected coin has no effect.</p>
     */
    public void collect() {
        // Монету не можна "повернути" — once collected, always collected
        this.collected = true;
    }

    /** Returns the column of this coin. @return column index 0–15 */
    public int getX() { return x; }

    /** Returns the row of this coin. @return row index 0–1 */
    public int getY() { return y; }

    /** Returns whether this coin has been collected. @return {@code true} if collected */
    public boolean isCollected() { return collected; }
}
