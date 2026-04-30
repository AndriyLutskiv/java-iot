package ua.crowpi.projects.p12;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.hardware.I2cFacade;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

/**
 * Drives the 16×2 LCD alternating between a clock page and a currency page.
 *
 * <p>The controller is called once per second by the scheduler in
 * {@link CurrencyClockProject}.  Each call is referred to as a <em>tick</em>.</p>
 *
 * <p>Behaviour per tick:</p>
 * <ol>
 *   <li>If the current mode is {@link DisplayMode#CLOCK}, format and write the
 *       current local time and date to the LCD (updated every second).</li>
 *   <li>If the current mode is {@link DisplayMode#CURRENCY} and this is the first
 *       tick in the current window, fetch rates from the NBU API and write them.
 *       Subsequent ticks in the same window are skipped (static display).</li>
 *   <li>After {@value #DISPLAY_SECONDS} ticks, switch to the other mode.</li>
 * </ol>
 *
 * <p>The method {@link #tick()} is package-private to allow unit tests to drive
 * the controller without starting the scheduler thread.</p>
 */
public class DisplayController {

    private static final Logger LOG = LogManager.getLogger(DisplayController.class);

    /** Number of seconds each mode is shown before switching. */
    public static final int DISPLAY_SECONDS = 5;

    /** I²C address of the PCF8574-based LCD backpack on the CrowPi. */
    public static final int LCD_ADDR = 0x21;

    /** HD44780 command: move cursor to row 0, column 0 (DDRAM address 0x80). */
    private static final byte LCD_ROW0 = (byte) 0x80;

    /** HD44780 command: move cursor to row 1, column 0 (DDRAM address 0xC0). */
    private static final byte LCD_ROW1 = (byte) 0xC0;

    // -------------------------------------------------------------------------
    // Залежності
    // -------------------------------------------------------------------------

    private final I2cFacade    lcd;
    private final NbuApiClient apiClient;

    // -------------------------------------------------------------------------
    // Стан контролера
    // -------------------------------------------------------------------------

    /** Поточний режим відображення — з якого починаємо. */
    private DisplayMode currentMode = DisplayMode.CLOCK;

    /** Лічильник тіків у поточному режимі (0 … DISPLAY_SECONDS-1). */
    private int ticksInMode = 0;

    /**
     * Cached currency rates from the last successful NBU API call.
     * Falls back to an empty list on first-tick failure so the display
     * shows "N/A" rather than crashing.
     */
    private List<CurrencyRate> cachedRates = Collections.emptyList();

    // -------------------------------------------------------------------------
    // Конструктор
    // -------------------------------------------------------------------------

    /**
     * Constructs a {@code DisplayController}.
     *
     * @param lcd       I²C facade used to write characters to the LCD
     * @param apiClient client used to fetch exchange rates from the NBU API
     */
    public DisplayController(I2cFacade lcd, NbuApiClient apiClient) {
        this.lcd       = lcd;
        this.apiClient = apiClient;
    }

    // -------------------------------------------------------------------------
    // Публічний API
    // -------------------------------------------------------------------------

    /**
     * Executes one display tick — called every second by the scheduler.
     *
     * <p>Package-private to allow unit tests to invoke ticks directly without
     * starting a {@link java.util.concurrent.ScheduledExecutorService}.</p>
     */
    void tick() {
        try {
            if (currentMode == DisplayMode.CLOCK) {
                // Оновлюємо годинник кожного тіку — дисплей живий
                renderClock();
            } else if (ticksInMode == 0) {
                // Перший тік режиму CURRENCY — завантажуємо свіжі курси
                fetchAndRenderCurrency();
            }
            // В режимі CURRENCY тіки 1-4 пропускаємо — дисплей залишається статичним

            ticksInMode++;

            // Після DISPLAY_SECONDS тіків перемикаємо режим
            if (ticksInMode >= DISPLAY_SECONDS) {
                currentMode = (currentMode == DisplayMode.CLOCK)
                        ? DisplayMode.CURRENCY
                        : DisplayMode.CLOCK;
                ticksInMode = 0;
                LOG.debug("Display switched to {}", currentMode);
            }
        } catch (Exception e) {
            // Не падаємо — просто логуємо; наступний тік спробує знову
            LOG.error("DisplayController tick error: {}", e.getMessage());
        }
    }

    /**
     * Returns the current display mode (for testing and diagnostics).
     *
     * @return current {@link DisplayMode}
     */
    public DisplayMode getCurrentMode() { return currentMode; }

    /**
     * Returns the number of ticks elapsed in the current mode window.
     *
     * @return tick counter in range [0, {@value #DISPLAY_SECONDS})
     */
    public int getTicksInMode() { return ticksInMode; }

    // -------------------------------------------------------------------------
    // Приватна логіка
    // -------------------------------------------------------------------------

    /**
     * Writes the current local time (row 0) and date (row 1) to the LCD.
     */
    private void renderClock() {
        String row0 = ClockPage.formatRow0(LocalTime.now());
        String row1 = ClockPage.formatRow1(LocalDate.now());
        lcdWriteLine(0, row0);
        lcdWriteLine(1, row1);
        LOG.trace("Clock: {} | {}", row0.trim(), row1.trim());
    }

    /**
     * Fetches fresh rates from the NBU API and writes them to the LCD.
     * On failure, uses the last cached rates (or "N/A" if never loaded).
     */
    private void fetchAndRenderCurrency() {
        try {
            // Запит до NBU API — може зайняти до 10 секунд при повільній мережі
            List<CurrencyRate> freshRates = apiClient.fetchRates();
            cachedRates = freshRates;
            LOG.info("NBU rates fetched: {} entries", freshRates.size());
        } catch (IOException e) {
            // Мережева помилка — показуємо кешовані курси або N/A
            LOG.warn("Failed to fetch NBU rates: {}. Using cached data.", e.getMessage());
        }

        String row0 = CurrencyPage.formatRow0(cachedRates);
        String row1 = CurrencyPage.formatRow1(cachedRates);
        lcdWriteLine(0, row0);
        lcdWriteLine(1, row1);
        LOG.trace("Currency: {} | {}", row0.trim(), row1.trim());
    }

    /**
     * Writes a 16-character string to the specified row of the LCD using the
     * HD44780 command protocol over I²C.
     *
     * @param row  0-based row index (0 = top, 1 = bottom)
     * @param text exactly 16-character string; longer strings are silently truncated
     */
    private void lcdWriteLine(int row, String text) {
        // Вибираємо DDRAM-адресу рядка: 0x80 для рядка 0, 0xC0 для рядка 1
        byte cursorCmd = (row == 0) ? LCD_ROW0 : LCD_ROW1;
        // Регістр 0x00 — команда HD44780 (встановлення позиції курсора)
        lcd.writeByte(LCD_ADDR, 0x00, cursorCmd);

        // Виводимо кожен символ у регістр 0x01 (символьні дані LCD)
        int len = Math.min(text.length(), ClockPage.LCD_WIDTH);
        for (int i = 0; i < len; i++) {
            lcd.writeByte(LCD_ADDR, 0x01, (byte) text.charAt(i));
        }
    }
}
