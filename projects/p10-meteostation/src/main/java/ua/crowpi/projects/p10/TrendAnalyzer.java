package ua.crowpi.projects.p10;

import java.util.List;

/**
 * Calculates the temperature trend slope using Ordinary Least Squares (OLS) linear regression.
 *
 * <p>The slope is computed over a time series of temperature values where the index
 * position acts as the independent variable x (0, 1, 2, …) and the temperature value
 * is the dependent variable y. A positive slope means the temperature is rising
 * (trending towards warmer weather), while a negative slope means it is falling.</p>
 *
 * <p>The result is used by {@link WeatherForecast#fromSlope(double)} to classify the
 * trend as IMPROVING, STABLE, or WORSENING.</p>
 */
public class TrendAnalyzer {

    /**
     * Computes the OLS linear regression slope over the given value series.
     *
     * <p>The Ordinary Least Squares formula used is:</p>
     * <pre>
     *   slope = (n * Σ(x*y) - Σx * Σy) / (n * Σ(x²) - (Σx)²)
     * </pre>
     * <p>where x = index (0, 1, 2, …) and y = temperature value at that index.</p>
     *
     * <p>Returns {@code 0.0} if there are fewer than 2 values (no trend can be computed)
     * or if the denominator is effectively zero (all x values identical, which cannot
     * happen with consecutive integer indices but is guarded against for safety).</p>
     *
     * @param values list of temperature values in chronological order (oldest first);
     *               must not be {@code null}
     * @return OLS slope in units of °C per reading interval; {@code 0.0} if insufficient data
     */
    public double calculateSlope(List<Double> values) {
        // МНК: slope = (n*Σxy - Σx*Σy) / (n*Σx² - (Σx)²)
        // де x = індекс (0,1,2,...), y = значення температури
        int n = values.size();
        if (n < 2) return 0.0;

        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumXX = 0;

        // Обчислюємо суми для формули МНК за один прохід
        for (int i = 0; i < n; i++) {
            sumX  += i;
            sumY  += values.get(i);
            sumXY += (double) i * values.get(i);
            sumXX += (double) i * i;
        }

        double denom = (double) n * sumXX - sumX * sumX;

        // Захист від ділення на нуль (не може статись з послідовними цілими x,
        // але перевіряємо на випадок крайніх ситуацій, наприклад n=1 вже відфільтровано)
        if (Math.abs(denom) < 1e-9) return 0.0;

        return ((double) n * sumXY - sumX * sumY) / denom;
    }
}
