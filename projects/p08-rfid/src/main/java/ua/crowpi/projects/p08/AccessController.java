package ua.crowpi.projects.p08;

import ua.crowpi.core.hardware.GpioFacade;
import ua.crowpi.core.hardware.I2cFacade;

import java.util.Optional;

/**
 * Orchestrates the RFID door-lock access control flow.
 *
 * <p>When a card UID is presented via {@link #processCard(String)} this class:</p>
 * <ol>
 *   <li>Looks up the UID in {@link CardDatabase}.</li>
 *   <li>Logs the attempt via {@link AccessAttemptLogger}.</li>
 *   <li>Drives the RGB LED to the appropriate colour.</li>
 *   <li>Updates both rows of the 16×2 LCD.</li>
 *   <li>Activates the relay for 500 ms (GRANTED) or sounds the buzzer (UNKNOWN_CARD).</li>
 * </ol>
 *
 * <p>Hardware pin assignments follow the CrowPi board layout documented in the
 * project specification.</p>
 */
public class AccessController {

    // -------------------------------------------------------------------------
    // Hardware constants
    // -------------------------------------------------------------------------

    /** I²C address of the PCF8574 LCD backpack. */
    public static final int LCD_ADDR = 0x21;

    /** BCM GPIO pin for the red RGB LED channel. */
    public static final int RGB_R = 22;

    /** BCM GPIO pin for the green RGB LED channel. */
    public static final int RGB_G = 27;

    /** BCM GPIO pin for the blue RGB LED channel. */
    public static final int RGB_B = 17;

    /** BCM GPIO pin for the passive buzzer (hardware PWM capable). */
    public static final int BUZZER_PIN = 18;

    /** Duration the relay stays open after a GRANTED result, in milliseconds. */
    private static final int UNLOCK_DURATION_MS = 500;

    /** Frequency for the warning buzz on UNKNOWN_CARD, in Hz. */
    private static final int BUZZ_FREQ_HZ = 800;

    /** Duration of the warning buzz, in milliseconds. */
    private static final int BUZZ_DURATION_MS = 200;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final CardDatabase db;
    private final RelayDoorLock lock;
    private final GpioFacade gpio;
    private final I2cFacade lcd;
    private final AccessAttemptLogger logger;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates an AccessController with all required collaborators injected.
     *
     * @param db     the card registry used to resolve UIDs to access results
     * @param lock   the relay controlling the physical door lock
     * @param gpio   the GPIO facade for RGB LED and buzzer
     * @param lcd    the I²C facade for the 16×2 LCD display
     * @param logger the structured logger for access attempts
     */
    public AccessController(CardDatabase db,
                            RelayDoorLock lock,
                            GpioFacade gpio,
                            I2cFacade lcd,
                            AccessAttemptLogger logger) {
        this.db     = db;
        this.lock   = lock;
        this.gpio   = gpio;
        this.lcd    = lcd;
        this.logger = logger;
        // Ініціалізуємо LCD та вимикаємо всі LED при старті
        lcdInit();
        setRgb(false, false, false);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Processes a single card UID and executes the appropriate hardware response.
     *
     * <p>Steps performed:</p>
     * <ol>
     *   <li>Query {@link CardDatabase} for the UID.</li>
     *   <li>Determine {@link AccessResult} (GRANTED / DENIED / UNKNOWN_CARD).</li>
     *   <li>Log the attempt with owner name (or "UNKNOWN" if not found).</li>
     *   <li>Show UID on LCD row 1 and result message on LCD row 2.</li>
     *   <li>Set RGB LED colour: green=GRANTED, yellow=DENIED, red=UNKNOWN_CARD.</li>
     *   <li>For GRANTED: unlock the door for {@value #UNLOCK_DURATION_MS} ms.</li>
     *   <li>For UNKNOWN_CARD: sound a {@value #BUZZ_FREQ_HZ} Hz buzzer beep.</li>
     * </ol>
     *
     * @param uid the colon-separated hex UID string read from the RFID reader
     * @return the {@link AccessResult} for this attempt
     */
    public AccessResult processCard(String uid) {
        // Крок 1: шукаємо картку в базі (може не знайти → Optional.empty())
        Optional<KnownCard> found = db.findByUid(uid);
        AccessResult result = db.check(uid);

        // Визначаємо ім'я власника для запису в лог
        String ownerName = found.isPresent() ? found.get().getOwnerName() : "UNKNOWN";

        // Крок 2: логуємо спробу доступу з усіма деталями
        logger.logAttempt(uid, result, ownerName);

        // Крок 3: відображаємо UID на першому рядку LCD (скорочуємо до 16 символів)
        String uidDisplay = uid != null ? uid : "NO UID";
        lcdShow("UID: " + uidDisplay, result.getMessage());

        // Крок 4: реагуємо на результат відповідними апаратними діями
        switch (result) {
            case GRANTED:
                // Зелений LED — доступ дозволено
                setRgb(false, true, false);
                // Відкриваємо реле на 500 мс — двері відчиняються
                lock.unlock(UNLOCK_DURATION_MS);
                break;

            case DENIED:
                // Жовтий LED (червоний + зелений) — картка деактивована
                setRgb(true, true, false);
                // Двері залишаються замкненими — реле не активуємо
                break;

            case UNKNOWN_CARD:
                // Червоний LED — невідома картка
                setRgb(true, false, false);
                // Короткий звуковий сигнал попередження
                buzz();
                break;

            default:
                // Захисна гілка — на випадок майбутнього розширення enum
                setRgb(false, false, false);
                break;
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Private hardware helpers
    // -------------------------------------------------------------------------

    /**
     * Sets the RGB LED colour by individually driving each colour channel.
     *
     * @param r {@code true} to turn the red channel on; {@code false} to turn it off
     * @param g {@code true} to turn the green channel on; {@code false} to turn it off
     * @param b {@code true} to turn the blue channel on; {@code false} to turn it off
     */
    private void setRgb(boolean r, boolean g, boolean b) {
        // Встановлюємо кожен канал незалежно — вони керуються окремими GPIO-піннами
        gpio.setOutput(RGB_R, r);
        gpio.setOutput(RGB_G, g);
        gpio.setOutput(RGB_B, b);
    }

    /**
     * Writes two lines of text to the 16×2 LCD display.
     *
     * <p>Each line is padded or truncated to exactly 16 characters before being sent
     * to ensure the previous content is completely overwritten.</p>
     *
     * @param line1 text for the top row of the LCD
     * @param line2 text for the bottom row of the LCD
     */
    private void lcdShow(String line1, String line2) {
        lcdWriteLine(0, line1);
        lcdWriteLine(1, line2);
    }

    /**
     * Writes a single line to the specified row of the HD44780 LCD via the I²C facade.
     *
     * @param row  0 for the top row (DDRAM address 0x00), 1 for the bottom row (0x40)
     * @param text text content; padded/truncated to exactly 16 characters
     */
    private void lcdWriteLine(int row, String text) {
        // Команда встановлення курсора: 0x80 = рядок 0, 0xC0 = рядок 1 (DDRAM offset 0x40)
        byte rowCmd = (row == 0) ? (byte) 0x80 : (byte) 0xC0;
        lcd.writeByte(LCD_ADDR, 0x00, rowCmd);

        // Нормалізуємо до рівно 16 символів — забезпечуємо повне перезаписування рядка
        String padded = String.format("%-16.16s", text != null ? text : "");
        for (char c : padded.toCharArray()) {
            lcd.writeByte(LCD_ADDR, 0x01, (byte) c);
        }
    }

    /**
     * Sends the LCD initialisation command sequence (clear display + turn on).
     *
     * <p>Called once from the constructor to put the HD44780 into a known state
     * before any content is written.</p>
     */
    private void lcdInit() {
        // Команда очистки дисплея — скидає всі символи у DDRAM
        lcd.writeByte(LCD_ADDR, 0x00, (byte) 0x01);
        sleepMs(5); // HD44780 потребує ~1.52 мс після clear — даємо 5 мс для надійності
        // Вмикаємо дисплей, курсор OFF, миготіння OFF (0x0C)
        lcd.writeByte(LCD_ADDR, 0x00, (byte) 0x0C);
    }

    /**
     * Sounds a short warning buzzer beep at {@value #BUZZ_FREQ_HZ} Hz for
     * {@value #BUZZ_DURATION_MS} milliseconds.
     *
     * <p>Uses hardware PWM via the GPIO facade. The buzzer is silenced (duty cycle 0)
     * after the beep duration to prevent continuous noise.</p>
     */
    private void buzz() {
        // Вмикаємо PWM зі скважністю 50% — стандарт для пасивного п'єзозумера
        gpio.pwm(BUZZER_PIN, BUZZ_FREQ_HZ, 0.5f);
        sleepMs(BUZZ_DURATION_MS);
        // Вимикаємо сигнал — duty cycle 0 = тиша
        gpio.pwm(BUZZER_PIN, BUZZ_FREQ_HZ, 0.0f);
    }

    /**
     * Sleeps the current thread for the specified number of milliseconds,
     * restoring the interrupt flag if the thread is interrupted.
     *
     * @param ms milliseconds to sleep
     */
    private static void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // Відновлюємо прапорець переривання — не гасимо його мовчки
            Thread.currentThread().interrupt();
        }
    }
}
