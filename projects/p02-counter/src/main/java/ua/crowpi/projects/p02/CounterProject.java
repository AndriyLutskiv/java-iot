package ua.crowpi.projects.p02;

import ua.crowpi.core.CrowPiProject;
import ua.crowpi.core.exception.HardwareException;
import ua.crowpi.core.hardware.GpioFacade;
import ua.crowpi.core.hardware.I2cFacade;
import ua.crowpi.core.mock.MockGpioFacade;
import ua.crowpi.core.mock.MockI2cFacade;
import ua.crowpi.core.pi4j.Pi4jGpioFacade;
import ua.crowpi.core.pi4j.Pi4jI2cFacade;

import com.pi4j.io.i2c.I2CBus;
import ua.crowpi.core.matrix.MatrixScrollerComponent;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * p02 — Motion Event Counter with 7-segment display.
 *
 * <p>Uses a PIR motion sensor to increment or decrement a counter displayed on a
 * 7-segment LED. Two push-buttons let the user reset the counter (or set a
 * countdown start value) and toggle the counting direction. A buzzer provides
 * audible feedback on every PIR detection event.</p>
 *
 * <p>GPIO pin assignments (BCM numbering):</p>
 * <ul>
 *   <li>PIR sensor  → GPIO 23 (physical pin 16)</li>
 *   <li>Button 1    → GPIO 11 (RESET / set start value in COUNTDOWN mode)</li>
 *   <li>Button 2    → GPIO  9 (toggle COUNT_UP ↔ COUNTDOWN)</li>
 *   <li>Buzzer PWM  → GPIO 18</li>
 *   <li>Segments    → GPIO 6, 13, 19, 21, 20, 16, 12 (A–G)</li>
 * </ul>
 *
 * <p>Event log lines are written to {@code logs/events.log} relative to the
 * current working directory.</p>
 */
public class CounterProject implements CrowPiProject {

    // -------------------------------------------------------------------------
    // Номери пінів GPIO (BCM)
    // -------------------------------------------------------------------------

    /** BCM pin number for the PIR motion sensor output. */
    private static final int PIR_PIN     = 23;

    /** BCM pin number for Button 1 (RESET / set start value). */
    private static final int BUTTON1_PIN = 11;

    /** BCM pin number for Button 2 (mode toggle). */
    private static final int BUTTON2_PIN =  9;

    /** BCM pin number for the active buzzer driven via PWM. */
    private static final int BUZZER_PIN  = 18;

    /** Buzzer click frequency in Hertz — short audible beep. */
    private static final int BUZZ_FREQ_HZ = 1000;

    /** Buzzer duty cycle during a click (50% = square wave, loudest). */
    private static final float BUZZ_DUTY = 0.5f;

    /** Duration of a single buzzer click in milliseconds. */
    private static final long BUZZ_DURATION_MS = 80L;

    /** Path to the event log file, relative to the working directory. */
    private static final String LOG_PATH = "logs/events.log";

    // -------------------------------------------------------------------------
    // Mutable project state — всі поля volatile, бо їх може змінити будь-який
    // потік-слухач GPIO або основний цикл
    // -------------------------------------------------------------------------

    /** Current counter value; always kept in [0, 9] via modulo arithmetic. */
    volatile int counter = 0;

    /** Active counting direction. */
    volatile CounterMode mode = CounterMode.COUNT_UP;

    /**
     * Start value used in COUNTDOWN mode; Button1 increments it (0–9).
     * When the user resets in COUNTDOWN mode the counter jumps to this value.
     */
    volatile int countdownStart = 0;

    /** Controls the main event loop; set to {@code false} by {@link #shutdown()}. */
    volatile boolean running = false;

    // -------------------------------------------------------------------------
    // Hardware and helper objects
    // -------------------------------------------------------------------------

    /** GPIO abstraction; assigned in the constructor or via {@link #run(boolean)}. */
    private GpioFacade gpio;

    /** I²C abstraction for the HT16K33 7-segment display at address 0x70. */
    private I2cFacade i2c;

    private MatrixScrollerComponent matrixScroller;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Default no-argument constructor used by the {@link java.util.ServiceLoader}.
     *
     * <p>The GPIO facade is created lazily inside {@link #run(boolean)} depending
     * on whether mock mode is requested.</p>
     */
    public CounterProject() {
        // GPIO-фасад буде встановлено в run() — тут лише ініціалізація за замовчуванням
    }

    /**
     * Constructor used in unit tests to inject a pre-configured GPIO facade.
     *
     * <p>This avoids ServiceLoader discovery and lets tests supply a mock or spy
     * without starting the full runtime loop.</p>
     *
     * @param gpio the GPIO facade to use; must not be {@code null}
     */
    public CounterProject(GpioFacade gpio) {
        // Дозволяємо тестам підмінити GPIO без запуску повного циклу
        this.gpio = gpio;
    }

    // -------------------------------------------------------------------------
    // CrowPiProject identity
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @return {@code "Motion Event Counter"}
     */
    @Override
    public String getName() {
        return "Motion Event Counter";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "p02"}
     */
    @Override
    public String getProjectId() {
        return "p02";
    }

    /**
     * {@inheritDoc}
     *
     * @return one-sentence description of this project
     */
    @Override
    public String getDescription() {
        return "Counts PIR-triggered motion events on a 7-segment display "
                + "with buzzer feedback, two operating modes, and persistent file logging.";
    }

    // -------------------------------------------------------------------------
    // Main lifecycle
    // -------------------------------------------------------------------------

    /**
     * Initialises hardware, registers GPIO listeners, and enters the main loop.
     *
     * <p>In mock mode a {@link MockGpioFacade} is used so the project can run on
     * any laptop without real Raspberry Pi hardware.</p>
     *
     * <p>The method blocks until {@link #shutdown()} sets {@code running = false}
     * or the calling thread is interrupted.</p>
     *
     * @param mockMode {@code true} to use the mock GPIO facade; {@code false} for real Pi4J GPIO
     * @throws HardwareException if the GPIO subsystem cannot be initialised
     */
    @Override
    public void run(boolean mockMode) throws HardwareException {
        // У режимі симуляції використовуємо MockGpioFacade, яка не звертається до реального GPIO
        boolean matrixMock = mockMode || (gpio != null);
        if (mockMode) {
            gpio = new MockGpioFacade();
            i2c  = new MockI2cFacade();
        } else if (gpio == null) {
            gpio = new Pi4jGpioFacade();
            i2c  = new Pi4jI2cFacade(I2CBus.BUS_1);
        }

        // ── LED matrix scroller ─────────────────────────────────────────────────
        matrixScroller = new MatrixScrollerComponent(matrixMock);
        matrixScroller.start();

        // Ініціалізуємо допоміжні об'єкти: дисплей і файловий логер
        SevenSegmentDisplay display = new SevenSegmentDisplay(i2c);
        EventFileLogger logger = new EventFileLogger(LOG_PATH);

        // Встановлюємо початковий стан лічильника та дисплею
        counter = 0;
        mode = CounterMode.COUNT_UP;
        countdownStart = 0;
        running = true;

        // Показуємо нуль на дисплеї при старті
        display.showDigit(0);

        // -----------------------------------------------------------------
        // Реєструємо слухача PIR-датчика (GPIO 26)
        // -----------------------------------------------------------------
        gpio.addListener(PIR_PIN, (pin, high) -> {
            // Реагуємо лише на передній фронт (HIGH) — спрацювання PIR
            if (!high) {
                return;
            }

            // Змінюємо лічильник залежно від поточного режиму
            if (mode == CounterMode.COUNT_UP) {
                // Збільшуємо лічильник, огортаючи через 10 (0→9→0)
                counter = (counter + 1) % 10;
            } else {
                // Зменшуємо лічильник; якщо від'ємний — огортаємо до 9
                counter = (counter - 1 + 10) % 10;
            }

            // Відтворюємо короткий звуковий сигнал (клік) через бузер
            buzzOnce();

            // Оновлюємо відображення на 7-сегментному дисплеї
            display.showDigit(Math.abs(counter) % 10);

            // Логуємо подію у файл; якщо запис не вдається — виводимо помилку в stderr
            try {
                EventRecord record = new EventRecord(LocalDateTime.now(), mode, counter);
                logger.append(record);
            } catch (IOException e) {
                System.err.println("[p02] Failed to write event log: " + e.getMessage());
            }
        });

        // -----------------------------------------------------------------
        // Реєструємо слухача Кнопки 1 (GPIO 11) — RESET або встановлення старту
        // -----------------------------------------------------------------
        gpio.addListener(BUTTON1_PIN, (pin, high) -> {
            // Реагуємо лише на натиснення (HIGH)
            if (!high) {
                return;
            }

            if (mode == CounterMode.COUNTDOWN) {
                // У режимі зворотного відліку Button1 збільшує початкове значення (циклічно 0–9)
                // Кожне натиснення додає 1, максимум 9
                countdownStart = (countdownStart + 1) % 10;
                counter = countdownStart;
                display.showDigit(counter);
                System.out.printf("[p02] Countdown start set to %d%n", countdownStart);
            } else {
                // У режимі COUNT_UP Button1 скидає лічильник до нуля
                counter = 0;
                display.showDigit(0);
                System.out.println("[p02] Counter reset to 0");
            }
        });

        // -----------------------------------------------------------------
        // Реєструємо слухача Кнопки 2 (GPIO 9) — перемикання режиму
        // -----------------------------------------------------------------
        gpio.addListener(BUTTON2_PIN, (pin, high) -> {
            // Реагуємо лише на натиснення (HIGH)
            if (!high) {
                return;
            }

            // Перемикаємо режим і скидаємо лічильник до відповідного початкового значення
            mode = mode.toggle();

            if (mode == CounterMode.COUNTDOWN) {
                // Переходимо на зворотний відлік: починаємо з countdownStart
                counter = countdownStart;
            } else {
                // Переходимо на COUNT_UP: починаємо з нуля
                counter = 0;
            }

            display.showDigit(counter);
            System.out.printf("[p02] Mode switched to: %s%n", mode.getLabel());
        });

        // -----------------------------------------------------------------
        // Основний цикл — тримаємо потік живим поки running == true
        // -----------------------------------------------------------------
        System.out.println("[p02] Motion Event Counter started. "
                + "Waiting for PIR events (mock fires every 5 s)…");

        while (running) {
            try {
                // Спимо 100 мс між ітераціями, щоб не блокувати CPU даремно
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Прапор переривання відновлюємо, щоб батьківський потік міг його побачити
                Thread.currentThread().interrupt();
                running = false;
            }
        }

        // Гасимо дисплей після виходу з головного циклу
        display.clear();
        System.out.println("[p02] Motion Event Counter stopped.");
    }

    /**
     * Stops the main loop and releases all GPIO resources.
     *
     * <p>This method is idempotent: repeated calls have no effect after the first.</p>
     */
    @Override
    public void shutdown() {
        // Зупиняємо головний цикл
        running = false;

        if (matrixScroller != null) {
            matrixScroller.stop();
            matrixScroller = null;
        }

        // Звільняємо ресурси GPIO лише якщо вони були ініціалізовані
        if (gpio != null) {
            gpio.close();
        }

        // Закриваємо I²C шину
        if (i2c instanceof AutoCloseable) {
            try {
                ((AutoCloseable) i2c).close();
            } catch (Exception e) {
                System.err.println("[p02] Failed to close I2C: " + e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Внутрішні допоміжні методи
    // -------------------------------------------------------------------------

    /**
     * Produces a single short buzzer click using PWM on {@link #BUZZER_PIN}.
     *
     * <p>PWM is enabled at {@link #BUZZ_FREQ_HZ} Hz for {@link #BUZZ_DURATION_MS}
     * milliseconds, then silenced. The call blocks for the click duration.</p>
     */
    private void buzzOnce() {
        // Вмикаємо PWM-сигнал на бузер — це генерує звук заданої частоти
        gpio.pwm(BUZZER_PIN, BUZZ_FREQ_HZ, BUZZ_DUTY);
        try {
            // Тримаємо сигнал протягом BUZZ_DURATION_MS мілісекунд
            Thread.sleep(BUZZ_DURATION_MS);
        } catch (InterruptedException e) {
            // Якщо потік перерваний — відновлюємо прапор, але звук все одно зупиняємо
            Thread.currentThread().interrupt();
        }
        // Вимикаємо PWM, встановлюючи нульовий робочий цикл — buzzer мовчить
        gpio.pwm(BUZZER_PIN, BUZZ_FREQ_HZ, 0.0f);
    }
}
