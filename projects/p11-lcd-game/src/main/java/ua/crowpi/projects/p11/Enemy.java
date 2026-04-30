package ua.crowpi.projects.p11;

/**
 * Represents a moving enemy on the LCD game grid.
 *
 * <p>The enemy bounces back and forth between {@code minX} and {@code maxX}.
 * The {@link #move(int, int)} method is called from the game engine tick and
 * must be {@code synchronized} so concurrent calls from the engine and renderer
 * threads do not produce a torn read.</p>
 */
public class Enemy {

    private int x;
    private int y;

    // volatile — direction змінюється з різних потоків без синхронізації
    private volatile int     direction = 1;   // 1=right, -1=left
    private volatile boolean alive     = true;

    /**
     * Constructs an Enemy at the given starting position, moving right.
     *
     * @param startX starting column (0–15)
     * @param startY starting row (0–1)
     */
    public Enemy(int startX, int startY) {
        this.x = startX;
        this.y = startY;
    }

    /**
     * Moves the enemy one cell in its current direction, bouncing at the boundaries.
     *
     * <p>Synchronized so the engine tick and the renderer read a consistent (x, direction)
     * pair even when called from different threads.</p>
     *
     * @param minX leftmost column the enemy may occupy (inclusive)
     * @param maxX rightmost column the enemy may occupy (inclusive)
     */
    public synchronized void move(int minX, int maxX) {
        if (!alive) return;

        x += direction;

        // При досягненні межі — змінюємо напрямок (відбиття)
        if (x <= minX) {
            x = minX;
            direction = 1;
        } else if (x >= maxX) {
            x = maxX;
            direction = -1;
        }
    }

    /** Returns the current column. @return column index */
    public synchronized int getX() { return x; }

    /** Returns the current row. @return row index */
    public int getY() { return y; }

    /** Sets the column. @param x new column */
    public synchronized void setX(int x) { this.x = x; }

    /** Sets the row. @param y new row */
    public void setY(int y) { this.y = y; }

    /** Returns the current movement direction (+1=right, -1=left). @return direction */
    public int getDirection() { return direction; }

    /** Returns whether this enemy is alive. @return {@code true} if alive */
    public boolean isAlive() { return alive; }

    /** Kills this enemy (removes from gameplay). @param alive false to kill */
    public void setAlive(boolean alive) { this.alive = alive; }
}
