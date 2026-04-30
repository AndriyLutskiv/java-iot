package ua.crowpi.projects.p10;

/**
 * Static utility for computing the Heat Index (apparent temperature / "feels like" value).
 *
 * <p>The Heat Index combines dry-bulb temperature and relative humidity into a single
 * value that approximates what the ambient temperature "feels like" to the human body
 * under conditions of light wind and shade.</p>
 *
 * <p>Two computation paths are used:</p>
 * <ol>
 *   <li>If temperature &lt; 27 °C or humidity &lt; 40 %, use the simplified linear
 *       approximation ({@code tempC + 0.5}) because the full Rothfusz polynomial
 *       is only valid above those thresholds.</li>
 *   <li>Otherwise use the full <em>Rothfusz regression equation</em> developed for
 *       the US National Weather Service, converted from Fahrenheit to Celsius.</li>
 * </ol>
 *
 * <p>This class is a pure static utility; it cannot be instantiated.</p>
 */
public final class HeatIndex {

    // Утилітний клас — забороняємо інстанціацію
    private HeatIndex() {
        throw new UnsupportedOperationException("Utility class — do not instantiate");
    }

    /**
     * Calculates the Heat Index (apparent temperature) for the given conditions.
     *
     * <p>For temperatures below 27 °C or humidity below 40 % the simplified formula
     * {@code tempC + 0.5} is returned because the Rothfusz polynomial is calibrated
     * for hot, humid conditions and gives incorrect results outside that range.</p>
     *
     * <p>For higher temperatures and humidity the full Rothfusz formula is applied.
     * All internal calculations use Fahrenheit (as required by the original equation)
     * and the final result is converted back to Celsius.</p>
     *
     * @param tempC           dry-bulb temperature in degrees Celsius
     * @param humidityPercent relative humidity as a percentage (0–100)
     * @return apparent temperature (Heat Index) in degrees Celsius
     */
    public static double calculate(double tempC, double humidityPercent) {
        // При низьких температурах або низькій вологості ефект відчутної температури
        // мінімальний — повертаємо спрощену оцінку замість формули Ротфуса,
        // яка дає некоректні результати за межами свого діапазону
        if (tempC < 27.0 || humidityPercent < 40.0) {
            return tempC + 0.5;
        }

        // Формула Ротфуса для відчутної температури (Apparent Temperature / Heat Index)
        // Використовуємо Фаренгейт для розрахунку, потім конвертуємо назад у Цельсій
        double T = tempC * 9.0 / 5.0 + 32.0; // → Fahrenheit
        double R = humidityPercent;

        // Повний поліном Ротфуса (Rothfusz regression, 9 коефіцієнтів)
        double HI = -42.379
                + 2.04901523  * T
                + 10.14333127 * R
                - 0.22475541  * T * R
                - 0.00683783  * T * T
                - 0.05481717  * R * R
                + 0.00122874  * T * T * R
                + 0.00085282  * T * R * R
                - 0.00000199  * T * T * R * R;

        // Конвертуємо результат назад у Цельсій
        return (HI - 32.0) * 5.0 / 9.0;
    }
}
