package ua.crowpi.projects.p01;

/**
 * Static utility class that formats {@link TemperatureReading} and {@link ThermalZone}
 * values into exactly 16-character strings suitable for the CrowPi 16×2 LCD display.
 *
 * <p>All methods are pure functions (no side effects) and work independently of any
 * hardware facade, making them trivially unit-testable without mocks.</p>
 *
 * <p>Example output:</p>
 * <pre>
 *   Line 1: "TEMP: 23.4C H:61"   (16 chars)
 *   Line 2: "STATUS: COMFORT "   (16 chars)
 * </pre>
 */
public final class LcdDisplayHelper {

    /** The CrowPi 16×2 LCD column count — all output strings must match this length. */
    public static final int LCD_WIDTH = 16;

    // Утилітний клас — не допускаємо інстанціювання
    private LcdDisplayHelper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Formats the first LCD line with temperature and humidity data.
     *
     * <p>Template: {@code "TEMP: ##.#C H:##%"} where the numbers are
     * derived from the reading. The result is always padded or truncated
     * to exactly {@value #LCD_WIDTH} characters.</p>
     *
     * @param reading the sensor reading to display; must not be {@code null}
     * @return exactly 16-character string for LCD row 0
     */
    public static String formatLine1(TemperatureReading reading) {
        // Форматуємо рядок у вигляді "TEMP: XX.XC H:XX%"
        // String.format гарантує один знак після коми для температури
        String raw = String.format("TEMP: %.1fC H:%.0f%%",
                reading.getTemperatureC(),
                reading.getHumidity());
        // Вирівнюємо до рівно 16 символів щоб не залишати сміття від попереднього запису
        return pad16(raw);
    }

    /**
     * Formats the second LCD line with the current thermal zone status.
     *
     * <p>Template: {@code "STATUS: XXXXXXXX"} where {@code XXXXXXXX} is the
     * zone label (already right-padded in the enum). The result is always
     * exactly {@value #LCD_WIDTH} characters long.</p>
     *
     * @param zone the current thermal zone; must not be {@code null}
     * @return exactly 16-character string for LCD row 1
     */
    public static String formatLine2(ThermalZone zone) {
        // Префікс "STATUS: " = 8 символів + мітка зони = 8 символів = 16 разом
        String raw = "STATUS: " + zone.getLabel();
        return pad16(raw);
    }

    /**
     * Pads or truncates the given string to exactly {@value #LCD_WIDTH} characters.
     *
     * <p>If {@code s} is shorter than 16 characters, trailing spaces are added.
     * If it is longer, the string is truncated to the first 16 characters.
     * This prevents residual characters from previous writes remaining visible
     * on the physical LCD.</p>
     *
     * @param s the input string; must not be {@code null}
     * @return string of exactly 16 characters
     */
    public static String pad16(String s) {
        if (s.length() == LCD_WIDTH) {
            // Вже потрібна довжина — повертаємо без змін
            return s;
        } else if (s.length() > LCD_WIDTH) {
            // Обрізаємо надлишок — LCD фізично не може показати більше 16 знаків
            return s.substring(0, LCD_WIDTH);
        } else {
            // Доповнюємо пробілами справа — перекриваємо залишки попереднього рядка
            StringBuilder sb = new StringBuilder(s);
            while (sb.length() < LCD_WIDTH) {
                sb.append(' ');
            }
            return sb.toString();
        }
    }
}
