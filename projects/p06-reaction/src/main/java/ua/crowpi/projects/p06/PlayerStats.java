package ua.crowpi.projects.p06;

import java.util.List;

/**
 * Computes descriptive statistics over a player's reaction times collected
 * during a game session.
 *
 * <p>All values are derived from the immutable snapshot of reaction times
 * passed at construction; the instance does not hold a reference to the
 * source {@link Player}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 *   PlayerStats stats = new PlayerStats(player.getReactionTimesMs());
 *   System.out.printf("Avg: %.1f ms, Min: %d ms%n",
 *                     stats.getAverage(), stats.getMin());
 * }</pre>
 */
public class PlayerStats {

    /** The snapshot of reaction times used for all calculations. */
    private final List<Long> times;

    // -------------------------------------------------------------------------
    // Конструктор
    // -------------------------------------------------------------------------

    /**
     * Creates a {@code PlayerStats} over the provided list of reaction times.
     *
     * @param times list of reaction times in milliseconds; may be empty but
     *              must not be {@code null}
     */
    public PlayerStats(List<Long> times) {
        // Зберігаємо посилання на список — він не змінюється ззовні під час підрахунку
        this.times = times;
    }

    // -------------------------------------------------------------------------
    // Статистичні методи
    // -------------------------------------------------------------------------

    /**
     * Returns the arithmetic mean of all recorded reaction times.
     *
     * @return average reaction time in milliseconds; {@code 0.0} if no times recorded
     */
    public double getAverage() {
        if (times.isEmpty()) {
            // Немає даних — повертаємо нуль замість NaN, щоб уникнути помилок при виводі
            return 0.0;
        }
        long sum = 0L;
        for (Long t : times) {
            sum += t;
        }
        // Ділимо суму на кількість записів для отримання середнього
        return (double) sum / times.size();
    }

    /**
     * Returns the smallest reaction time recorded.
     *
     * @return minimum reaction time in milliseconds; {@link Long#MAX_VALUE} if empty
     */
    public long getMin() {
        if (times.isEmpty()) {
            // Немає даних — повертаємо MAX_VALUE як sentinel-значення
            return Long.MAX_VALUE;
        }
        long min = Long.MAX_VALUE;
        for (Long t : times) {
            if (t < min) {
                min = t;
            }
        }
        return min;
    }

    /**
     * Returns the largest reaction time recorded.
     *
     * @return maximum reaction time in milliseconds; {@code 0} if empty
     */
    public long getMax() {
        if (times.isEmpty()) {
            // Немає даних — повертаємо нуль як sentinel-значення
            return 0L;
        }
        long max = 0L;
        for (Long t : times) {
            if (t > max) {
                max = t;
            }
        }
        return max;
    }

    /**
     * Returns the total number of reaction times recorded.
     *
     * @return count of recorded times; {@code 0} if none
     */
    public int getCount() {
        return times.size();
    }
}
