package ua.crowpi.projects.p11;

/**
 * Represents a stationary platform on the LCD game grid.
 *
 * <p>A platform occupies {@code width} consecutive columns starting at
 * column {@code x}, all on the same row {@code y}.  The player can stand
 * on a platform when both x and y match.</p>
 */
public class Platform {

    // Лівий край платформи (включно), рядок і ширина у клітинках
    private final int x;
    private final int y;
    private final int width;

    /**
     * Constructs a Platform at the given position with the given width.
     *
     * @param x     leftmost column of the platform (inclusive)
     * @param y     row of the platform (0 = top row, 1 = bottom row)
     * @param width number of columns the platform spans
     */
    public Platform(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    /**
     * Returns {@code true} if the given grid position is on this platform.
     *
     * <p>A position is considered "on" the platform when the column is within
     * {@code [x, x+width)} and the row matches exactly.</p>
     *
     * @param px column to test
     * @param py row to test
     * @return {@code true} if (px, py) is covered by this platform
     */
    public boolean contains(int px, int py) {
        // Перевіряємо горизонтальне перекриття і збіг рядка
        return px >= x && px < x + width && py == y;
    }

    /**
     * Returns the leftmost column of this platform.
     *
     * @return starting column index
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the row on which this platform is located.
     *
     * @return row index (0 or 1)
     */
    public int getY() {
        return y;
    }

    /**
     * Returns the width (number of columns) of this platform.
     *
     * @return platform width in columns
     */
    public int getWidth() {
        return width;
    }
}
