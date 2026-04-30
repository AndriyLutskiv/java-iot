package ua.crowpi.projects.p12;

import java.util.List;

/**
 * Formats USD and EUR exchange rates against UAH into two 16-character strings
 * suitable for display on a 16×2 character LCD.
 *
 * <p>Output layout (rate formatted to 2 decimal places):</p>
 * <pre>
 * Row 0: "USD: 41.23 UAH  "   (5 + rate + " UAH", padded to 16)
 * Row 1: "EUR: 44.56 UAH  "   (5 + rate + " UAH", padded to 16)
 * </pre>
 *
 * <p>If a currency is not found in the provided rate list, the row shows
 * {@code "XXX: N/A UAH    "} so the user knows the data is unavailable.</p>
 *
 * <p>All methods are {@code static} — this class contains no mutable state.</p>
 */
public final class CurrencyPage {

    /** Width of the 16×2 LCD in characters. */
    public static final int LCD_WIDTH = 16;

    private CurrencyPage() {
        // Утилітний клас — конструктор закритий
    }

    /**
     * Formats the top LCD row showing the USD rate.
     *
     * <p>Example: {@code "USD: 41.23 UAH  "} (always exactly 16 characters).</p>
     *
     * @param rates list of currency rates fetched from the NBU API
     * @return exactly {@value #LCD_WIDTH}-character string for LCD row 0
     */
    public static String formatRow0(List<CurrencyRate> rates) {
        return formatLine("USD", rates);
    }

    /**
     * Formats the bottom LCD row showing the EUR rate.
     *
     * <p>Example: {@code "EUR: 44.56 UAH  "} (always exactly 16 characters).</p>
     *
     * @param rates list of currency rates fetched from the NBU API
     * @return exactly {@value #LCD_WIDTH}-character string for LCD row 1
     */
    public static String formatRow1(List<CurrencyRate> rates) {
        return formatLine("EUR", rates);
    }

    /**
     * Formats a single LCD row for the given currency code.
     *
     * <p>The rate is formatted with 2 decimal places.  If the currency is not
     * present in the list, the text {@code "N/A"} is shown instead of a number.</p>
     *
     * @param cc    ISO 4217 currency code, e.g. {@code "USD"}
     * @param rates list of currency rates to search in
     * @return exactly {@value #LCD_WIDTH}-character string
     */
    public static String formatLine(String cc, List<CurrencyRate> rates) {
        // Шукаємо курс для потрібної валюти без урахування регістру
        String rateStr = rates.stream()
                .filter(r -> cc.equalsIgnoreCase(r.getCurrencyCode()))
                .findFirst()
                .map(r -> String.format("%.2f", r.getRate()))
                .orElse("N/A");

        // "USD: 41.23 UAH" — відстань 5 + довжина курсу + 4 = 14 для типових значень
        return ClockPage.pad(cc + ": " + rateStr + " UAH");
    }
}
