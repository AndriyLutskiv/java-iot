package ua.crowpi.projects.p03;

import ua.crowpi.core.CrowPiProject;
import ua.crowpi.core.exception.HardwareException;
import ua.crowpi.core.hardware.GpioFacade;
import ua.crowpi.core.hardware.I2cFacade;
import ua.crowpi.core.matrix.MatrixScrollerComponent;
import ua.crowpi.core.mock.MockGpioFacade;
import ua.crowpi.core.mock.MockI2cFacade;
import com.pi4j.io.i2c.I2CBus;
import ua.crowpi.core.pi4j.Pi4jGpioFacade;
import ua.crowpi.core.pi4j.Pi4jLcdFacade;

import java.util.List;

/**
 * CrowPi educational project p03: <em>IR Remote Music Box</em>.
 *
 * <p>Demonstrates:</p>
 * <ul>
 *   <li>Thread management and {@code volatile} state — {@link MelodyPlayer}, {@link MatrixScroller}</li>
 *   <li>Hardware PWM audio — passive buzzer on BCM 18</li>
 *   <li>NEC IR decoding — {@link IrCodeDecoder}</li>
 *   <li>Enum-driven state machine — {@link PlaybackState}</li>
 *   <li>LCD progress bar rendering — {@link LcdProgressRenderer}</li>
 * </ul>
 *
 * <p><strong>Hardware used:</strong></p>
 * <ul>
 *   <li>IR receiver (VS1838B) on BCM 20 (physical pin 38)</li>
 *   <li>8×8 LED matrix (MAX7219) on SPI0 CE1 — BCM 7, physical pin 26</li>
 *   <li>Passive buzzer (PWM) on BCM 18</li>
 *   <li>RGB LED: R=BCM 22, G=BCM 27, B=BCM 17</li>
 *   <li>I²C LCD 16×2 at address 0x21</li>
 * </ul>
 *
 * <p><strong>User interface:</strong></p>
 * <ul>
 *   <li>IR button 1–9 → play the corresponding melody</li>
 *   <li>IR button 0   → stop playback</li>
 *   <li>LCD row 1     → melody name (up to 16 chars)</li>
 *   <li>LCD row 2     → {@code [████████░░]} progress bar</li>
 * </ul>
 *
 * <p>In mock mode the IR receiver is simulated: every
 * {@link #MOCK_INTERVAL_MS} milliseconds the next melody (1–9) is
 * auto-selected so students can observe the full UI behaviour without
 * physical hardware.</p>
 */
public final class MusicBoxProject implements CrowPiProject {

    // -------------------------------------------------------------------------
    // Hardware constants
    // -------------------------------------------------------------------------

    /** I²C address of the PCF8574 LCD backpack. */
    private static final int LCD_ADDR = 0x21;

    /** BCM GPIO pin of the IR receiver module. */
    private static final int IR_PIN = 20;

    // -------------------------------------------------------------------------
    // Mock-mode timing
    // -------------------------------------------------------------------------

    /** Milliseconds between auto-advancing to the next mock melody. */
    private static final long MOCK_INTERVAL_MS = 5_000L;

    /** Milliseconds to wait before auto-playing the first melody in mock mode. */
    private static final long MOCK_INITIAL_DELAY_MS = 2_000L;

    /** Poll interval for the main loop. */
    private static final long POLL_INTERVAL_MS = 100L;

    // -------------------------------------------------------------------------
    // Runtime components
    // -------------------------------------------------------------------------

    /** GPIO facade — real or mock depending on launch mode. */
    private GpioFacade gpio;

    /** I²C facade for the LCD — real or mock. */
    private I2cFacade lcd;

    /** Melody playback engine running in its own daemon thread. */
    private MelodyPlayer player;

    /** NEC IR code decoder. */
    private IrCodeDecoder decoder;

    /** NEC IR receiver — registers as a PinChangeListener and queues decoded frames. */
    private NecIrReceiver irReceiver;

    /** LED matrix scroller — scrolls "F7    Computer Engineering" in a daemon thread. */
    private MatrixScrollerComponent matrixScroller;

    /** Current playback state shown on the LCD. */
    private PlaybackState playbackState = PlaybackState.IDLE;

    /** The melody currently selected (or {@code null} when idle). */
    private Melody currentMelody = null;

    /**
     * Signals the main loop to exit.
     *
     * <p>{@code volatile} — written by {@link #shutdown()} from a JVM shutdown hook
     * thread, read by the main loop thread.</p>
     */
    private volatile boolean running = false;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Creates a MusicBoxProject for production use.
     *
     * <p>Hardware facades are created in {@link #run(boolean)} according to the
     * {@code mockMode} parameter.</p>
     */
    public MusicBoxProject() {
        // Фасади ініціалізуються ліниво в run() — залежать від режиму запуску
    }

    /**
     * Creates a MusicBoxProject with pre-injected hardware facades.
     *
     * <p>Intended for unit tests that supply mock or Mockito-stubbed facades directly,
     * bypassing the {@code mockMode} branch in {@link #run(boolean)}.</p>
     *
     * @param gpio GPIO facade to use
     * @param lcd  I²C facade for the LCD
     */
    public MusicBoxProject(GpioFacade gpio, I2cFacade lcd) {
        this.gpio = gpio;
        this.lcd  = lcd;
    }

    // -------------------------------------------------------------------------
    // CrowPiProject contract
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @return {@code "IR Remote Music Box"}
     */
    @Override
    public String getName() {
        return "IR Remote Music Box";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "p03"}
     */
    @Override
    public String getProjectId() {
        return "p03";
    }

    /**
     * {@inheritDoc}
     *
     * @return a one-sentence description of this project
     */
    @Override
    public String getDescription() {
        return "Play nine built-in melodies using an IR remote: buttons 1-9 select a melody, "
                + "button 0 stops playback; an LCD shows the melody name and progress bar.";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Initialises hardware, then enters the main polling loop.
     * In mock mode, an auto-play sequence cycles through all nine melodies.</p>
     *
     * @param mockMode {@code true} to use {@link MockGpioFacade} and {@link MockI2cFacade}
     * @throws HardwareException if real hardware was requested but is not available
     */
    @Override
    public void run(boolean mockMode) throws HardwareException {
        running = true;

        // ----------------------------------------------------------------
        // 1. Ініціалізуємо hardware-фасади якщо вони не були ін'єктовані
        // ----------------------------------------------------------------
        if (gpio == null) {
            if (mockMode) {
                gpio = new MockGpioFacade();
                lcd  = new MockI2cFacade();
            } else {
                gpio = new Pi4jGpioFacade();
                lcd  = new Pi4jLcdFacade(I2CBus.BUS_1, 0x21);
            }
        }

        // ----------------------------------------------------------------
        // 2. Створюємо компоненти
        // ----------------------------------------------------------------
        player     = new MelodyPlayer(gpio);
        decoder    = new IrCodeDecoder(mockMode);
        irReceiver = new NecIrReceiver();
        if (!mockMode) {
            // У реальному режимі реєструємо NecIrReceiver як слухач GPIO-піну
            gpio.addListener(IR_PIN, irReceiver);
        }

        // ── LED матриця MAX7219 на SPI0 CE1 ──────────────────────────────────
        matrixScroller = new MatrixScrollerComponent(mockMode);
        matrixScroller.start();

        List<Melody> library = MelodyLibrary.getMelodies();

        // ----------------------------------------------------------------
        // 3. Ініціалізуємо LCD
        // ----------------------------------------------------------------
        lcdInit();
        lcdWriteLine(0, "IR MUSIC BOX");
        lcdWriteLine(1, "Press 1-9 to play");

        System.out.println("=== IR REMOTE MUSIC BOX — p03 ===");
        System.out.println("Melodies: " + library.size());
        System.out.println("IR pin: BCM " + IR_PIN + "  |  Buzzer: BCM " + MelodyPlayer.BUZZER_PIN);
        System.out.println("---------------------------------");

        // ----------------------------------------------------------------
        // 4. Mock-режим: чекаємо MOCK_INITIAL_DELAY_MS потім починаємо
        // ----------------------------------------------------------------
        long mockLastAdvance = System.currentTimeMillis() - MOCK_INTERVAL_MS
                + MOCK_INITIAL_DELAY_MS;

        // ----------------------------------------------------------------
        // 5. Головний цикл
        // ----------------------------------------------------------------
        while (running) {

            int digit = -1;

            if (mockMode) {
                // У mock-режимі симулюємо IR: кожні MOCK_INTERVAL_MS секунд обираємо наступну мелодію
                long now = System.currentTimeMillis();
                if (now - mockLastAdvance >= MOCK_INTERVAL_MS) {
                    mockLastAdvance = now;
                    // decoder.decode() у mock-режимі ігнорує параметр та повертає 1–9 циклічно
                    digit = decoder.decode(0);
                }
            } else {
                // На реальному RPi: NecIrReceiver декодує NEC-кадри у фоні через
                // PinChangeListener і кладе їх у чергу.  Тут просто перевіряємо
                // чи є готовий кадр (poll(0) — без блокування).
                int rawCode = irReceiver.poll(0);
                if (rawCode != -1) {
                    digit = decoder.decode(rawCode);
                }
            }

            // ---- Обробка отриманого IR-сигналу ----
            if (digit == 0) {
                // Кнопка 0 — зупинити відтворення
                player.stop();
                playbackState = PlaybackState.PAUSED;
                currentMelody = null;
                lcdWriteLine(0, "STOPPED         ");
                lcdWriteLine(1, "Press 1-9...    ");

            } else if (digit >= 1 && digit <= 9) {
                // Кнопка 1–9 — відтворити відповідну мелодію
                // Зупиняємо попереднє відтворення якщо воно тривало
                player.stop();

                Melody melody = library.get(digit - 1);
                currentMelody = melody;
                playbackState = PlaybackState.PLAYING;

                System.out.println("[p03] Playing melody " + digit + ": " + melody.getName());
                player.play(melody);

                // Відображаємо назву мелодії на першому рядку LCD
                lcdWriteLine(0, melody.getName());
            }

            // ---- Оновлення прогрес-бару якщо грає мелодія ----
            if (playbackState == PlaybackState.PLAYING && currentMelody != null) {
                if (player.isPlaying()) {
                    // Мелодія ще грає — оновлюємо прогрес-бар
                    int elapsed = player.getElapsedMs();
                    int total   = currentMelody.getTotalDurationMs();
                    String line2 = LcdProgressRenderer.renderLine2(elapsed, total, "");
                    lcdWriteLine(1, line2);
                } else {
                    // Мелодія природно завершилась
                    playbackState = PlaybackState.IDLE;
                    currentMelody = null;
                    lcdWriteLine(0, "DONE            ");
                    lcdWriteLine(1, LcdProgressRenderer.renderLine2(1, 1, ""));
                }
            } else if (playbackState == PlaybackState.IDLE) {
                // Стан очікування — показуємо підказку
                lcdWriteLine(0, "IR MUSIC BOX    ");
                lcdWriteLine(1, "Press 1-9 ...   ");
            }

            // Невелика пауза щоб не перевантажувати процесор і I2C-шину
            sleepMs(POLL_INTERVAL_MS);
        }

        System.out.println("[p03] Main loop exited");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stops any active playback, closes the GPIO facade and flags the main
     * loop to exit. Safe to call multiple times.</p>
     */
    @Override
    public void shutdown() {
        if (!running && player == null) {
            // Ідемпотентність: якщо run() ще не викликався — нічого не робимо
            return;
        }
        running = false;

        if (player != null) {
            player.stop();
        }
        if (matrixScroller != null) {
            matrixScroller.stop();
            matrixScroller = null;
        }
        if (gpio != null) {
            gpio.close();
        }

        System.out.println("[p03] Shutdown complete");
    }

    // -------------------------------------------------------------------------
    // LCD helpers
    // -------------------------------------------------------------------------

    /**
     * Sends the LCD initialisation command sequence (clear + display on).
     */
    private void lcdInit() {
        // Команди ініціалізації HD44780: спочатку очищаємо, потім вмикаємо дисплей
        lcd.writeByte(LCD_ADDR, 0x00, (byte) 0x01); // Clear display
        sleepMs(5);
        lcd.writeByte(LCD_ADDR, 0x00, (byte) 0x0C); // Display ON, cursor OFF
    }

    /**
     * Writes a line of text to the specified LCD row.
     *
     * <p>The text is padded or truncated to exactly 16 characters to fill the
     * LCD column without leaving stale characters from previous writes.</p>
     *
     * @param row  0 for the top row, 1 for the bottom row
     * @param text text content; padded/truncated to 16 chars
     */
    private void lcdWriteLine(int row, String text) {
        // Команда встановлення курсора: 0x80 = рядок 0, 0xC0 = рядок 1
        byte rowCmd = (row == 0) ? (byte) 0x80 : (byte) 0xC0;
        lcd.writeByte(LCD_ADDR, 0x00, rowCmd);

        // Нормалізуємо до рівно 16 символів, щоб не залишати сміттєвих символів
        String padded = String.format("%-16.16s", text != null ? text : "");
        for (char c : padded.toCharArray()) {
            lcd.writeByte(LCD_ADDR, 0x01, (byte) c);
        }
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    /**
     * Sleeps the current thread for the given number of milliseconds,
     * restoring the interrupt flag if interrupted.
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
