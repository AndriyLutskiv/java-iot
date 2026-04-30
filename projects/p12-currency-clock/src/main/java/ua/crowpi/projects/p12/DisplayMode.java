package ua.crowpi.projects.p12;

/**
 * Represents the two mutually exclusive display states of the currency-clock project.
 *
 * <p>The display alternates between {@link #CLOCK} (current time and date) and
 * {@link #CURRENCY} (USD and EUR exchange rates against UAH), spending
 * {@link DisplayController#DISPLAY_SECONDS} seconds in each state before switching.</p>
 */
public enum DisplayMode {

    /**
     * Shows the current local time on the top row and the current date on the bottom row.
     *
     * <p>The display is refreshed every second while this mode is active.</p>
     */
    CLOCK,

    /**
     * Shows the USD/UAH exchange rate on the top row and the EUR/UAH rate on the bottom row.
     *
     * <p>Rates are fetched from the NBU API once on entry to this mode and remain static
     * for the full display duration.</p>
     */
    CURRENCY
}
