package ua.crowpi.projects.p01;

/**
 * Classifies ambient temperature into three comfort zones used by the
 * Thermometer project to drive the RGB LED colour and LCD status line.
 *
 * <p>Zone boundaries:</p>
 * <ul>
 *   <li>{@link #COLD}    — below 18 °C  → blue LED</li>
 *   <li>{@link #COMFORT} — 18–28 °C     → green LED</li>
 *   <li>{@link #HOT}     — above 28 °C  → red LED</li>
 * </ul>
 *
 * <p>The static factory method {@link #forTemp(double)} encapsulates all
 * boundary logic so the rest of the application never hard-codes magic numbers.</p>
 */
public enum ThermalZone {

    /** Temperature below 18 °C — occupant may feel cold. */
    COLD("COLD    "),

    /** Temperature between 18 °C and 28 °C (inclusive) — comfortable range. */
    COMFORT("COMFORT "),

    /** Temperature above 28 °C — potentially overheated. */
    HOT("HOT     ");

    // Відображуваний рядок для LCD — вирівняний пробілами до 8 символів,
    // щоб разом з префіксом "STATUS: " точно заповнити 16 знаків
    private final String label;

    /**
     * Constructs a {@code ThermalZone} with the given display label.
     *
     * @param label display string shown on the LCD status line
     */
    ThermalZone(String label) {
        this.label = label;
    }

    /**
     * Returns the display label used on the LCD second line.
     *
     * <p>The label is right-padded with spaces so that
     * {@code "STATUS: " + label} fits exactly 16 characters.</p>
     *
     * @return LCD-ready label string
     */
    public String getLabel() {
        return label;
    }

    /**
     * Determines the thermal zone for a given temperature reading.
     *
     * <p>Boundary conditions:</p>
     * <ul>
     *   <li>tempC &lt; 18.0  → {@link #COLD}</li>
     *   <li>tempC &gt; 28.0  → {@link #HOT}</li>
     *   <li>otherwise        → {@link #COMFORT}</li>
     * </ul>
     *
     * @param tempC temperature in degrees Celsius
     * @return the corresponding {@code ThermalZone}; never {@code null}
     */
    public static ThermalZone forTemp(double tempC) {
        // Класифікуємо температуру за фіксованими порогами комфорту
        if (tempC < 18.0) {
            return COLD;
        } else if (tempC > 28.0) {
            return HOT;
        } else {
            // Діапазон [18, 28] включно вважається комфортним
            return COMFORT;
        }
    }
}
