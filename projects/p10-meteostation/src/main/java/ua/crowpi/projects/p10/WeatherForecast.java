package ua.crowpi.projects.p10;

/**
 * Classifies the temperature trend direction computed by {@link TrendAnalyzer}.
 *
 * <p>The classification thresholds (±0.05 °C per reading) were chosen so that
 * noise in DHT11 readings (±0.5 °C absolute accuracy) does not cause spurious
 * trend changes when the weather is actually stable.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 *   double slope = new TrendAnalyzer().calculateSlope(temperatureHistory);
 *   WeatherForecast forecast = WeatherForecast.fromSlope(slope);
 *   lcd.writeLine("TREND:" + forecast.getArrow() + " " + forecast.getLabel());
 * }</pre>
 */
public enum WeatherForecast {

    /**
     * Temperature is rising — slope &gt; +0.05 °C/reading.
     * Indicates warming conditions ahead.
     */
    IMPROVING,

    /**
     * Temperature is approximately flat — slope in the range [-0.05, +0.05].
     * Indicates stable, unchanged conditions.
     */
    STABLE,

    /**
     * Temperature is falling — slope &lt; -0.05 °C/reading.
     * Indicates cooling conditions ahead.
     */
    WORSENING;

    // Пороговий нахил, що відокремлює STABLE від IMPROVING/WORSENING.
    // Значення 0.05 відповідає зміні ~0.05°C на кожне читання (1 раз/хв),
    // тобто ~3°C/год — відчутний тренд, але не шум DHT11.
    private static final double TREND_THRESHOLD = 0.05;

    /**
     * Determines the forecast category from a regression slope value.
     *
     * @param slope OLS slope in °C per reading interval, computed by
     *              {@link TrendAnalyzer#calculateSlope(java.util.List)}
     * @return {@link #IMPROVING} if slope &gt; +0.05,
     *         {@link #WORSENING} if slope &lt; -0.05,
     *         {@link #STABLE} otherwise
     */
    public static WeatherForecast fromSlope(double slope) {
        // Позитивний нахил більше порогу — температура зростає (покращення)
        if (slope > TREND_THRESHOLD) {
            return IMPROVING;
        }
        // Негативний нахил менше порогу — температура падає (погіршення)
        if (slope < -TREND_THRESHOLD) {
            return WORSENING;
        }
        // Нахил у межах порогу — стабільна погода
        return STABLE;
    }

    /**
     * Returns the human-readable label for this forecast shown on LCD and in reports.
     *
     * @return "IMPROVING", "STABLE", or "WORSENING"
     */
    public String getLabel() {
        // Повертаємо назву enum як є — вона вже правильно відформатована великими літерами
        return name();
    }

    /**
     * Returns a single Unicode arrow character representing the temperature direction.
     *
     * <p>Used on the LCD second line and in HTML reports for a quick visual indicator.</p>
     *
     * @return "↑" for IMPROVING, "→" for STABLE, "↓" for WORSENING
     */
    public String getArrow() {
        switch (this) {
            case IMPROVING: return "\u2191"; // ↑
            case WORSENING: return "\u2193"; // ↓
            default:        return "\u2192"; // →
        }
    }
}
