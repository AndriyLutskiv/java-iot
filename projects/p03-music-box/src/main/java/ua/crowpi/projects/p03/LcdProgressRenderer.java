package ua.crowpi.projects.p03;

/**
 * Static utility that renders a text-based progress bar for the LCD 16×2 display.
 *
 * <p>The bar uses Unicode block characters:</p>
 * <ul>
 *   <li>{@code '█'} (U+2588 FULL BLOCK) for elapsed progress</li>
 *   <li>{@code '░'} (U+2591 LIGHT SHADE) for remaining time</li>
 * </ul>
 *
 * <p>Example output for 50 %:</p>
 * <pre>
 *   [█████░░░░░]     (line2, 16 chars total)
 * </pre>
 *
 * <p>All methods are static; this class cannot be instantiated.</p>
 */
public final class LcdProgressRenderer {

    /** Total number of block segments in the progress bar. */
    private static final int BAR_WIDTH = 10;

    /** Unicode full-block character used for elapsed progress. */
    private static final char FILLED = '\u2588'; // █

    /** Unicode light-shade character used for remaining time. */
    private static final char EMPTY  = '\u2591'; // ░

    /** Target width of the complete LCD line including brackets. */
    private static final int LCD_WIDTH = 16;

    // -------------------------------------------------------------------------
    // Private constructor — утиліта, не підлягає ініціалізації
    // -------------------------------------------------------------------------

    /** Utility class — instantiation is prohibited. */
    private LcdProgressRenderer() {
        throw new UnsupportedOperationException(
                "LcdProgressRenderer is a static utility class");
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Renders a 10-character progress bar string based on the ratio
     * {@code current / total}.
     *
     * <p>Edge cases:</p>
     * <ul>
     *   <li>If {@code total <= 0} the bar is all empty ({@code "░░░░░░░░░░"}).</li>
     *   <li>If {@code current >= total} the bar is all filled ({@code "██████████"}).</li>
     *   <li>If {@code current <= 0} the bar is all empty.</li>
     * </ul>
     *
     * @param current elapsed amount (e.g. elapsed milliseconds)
     * @param total   total amount (e.g. total melody duration in milliseconds)
     * @return exactly 10-character bar string
     */
    public static String renderBar(int current, int total) {
        // Захист від ділення на нуль та від'ємних значень
        if (total <= 0 || current <= 0) {
            return buildBar(0);
        }
        if (current >= total) {
            // Мелодія завершена — повністю заповнений бар
            return buildBar(BAR_WIDTH);
        }

        // Обчислюємо кількість заповнених блоків: floor(current/total * BAR_WIDTH)
        // Використовуємо long-арифметику щоб уникнути переповнення при великих значеннях
        int filled = (int) ((long) current * BAR_WIDTH / total);
        return buildBar(filled);
    }

    /**
     * Renders the complete LCD second row: bracket-enclosed progress bar padded
     * to exactly 16 characters.
     *
     * <p>Format: {@code "[" + renderBar(currentMs, totalMs) + "]" + spaces}</p>
     *
     * @param currentMs elapsed playback position in milliseconds
     * @param totalMs   total melody duration in milliseconds
     * @param padding   extra string appended after the closing bracket before padding
     *                  (pass empty string if not needed)
     * @return exactly 16-character string for the LCD second row
     */
    public static String renderLine2(int currentMs, int totalMs, String padding) {
        String bar = "[" + renderBar(currentMs, totalMs) + "]";
        // Якщо padding задано — додаємо після дужки, потім вирівнюємо до 16 символів
        String raw = bar + (padding != null ? padding : "");
        return String.format("%-" + LCD_WIDTH + "." + LCD_WIDTH + "s", raw);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a 10-character bar with the specified number of filled blocks.
     *
     * @param filledCount number of filled (█) characters; must be 0–{@value #BAR_WIDTH}
     * @return 10-character bar string
     */
    private static String buildBar(int filledCount) {
        // Обмежуємо значення діапазоном [0, BAR_WIDTH] для безпеки
        int clamped = Math.max(0, Math.min(filledCount, BAR_WIDTH));
        StringBuilder sb = new StringBuilder(BAR_WIDTH);
        for (int i = 0; i < clamped; i++) {
            sb.append(FILLED);
        }
        for (int i = clamped; i < BAR_WIDTH; i++) {
            sb.append(EMPTY);
        }
        return sb.toString();
    }
}
