package ua.crowpi.projects.p01;

import ua.crowpi.core.CrowPiProject;
import ua.crowpi.core.exception.HardwareException;
import ua.crowpi.core.hardware.GpioFacade;
import ua.crowpi.core.hardware.I2cFacade;
import ua.crowpi.core.hardware.SensorReader;
import ua.crowpi.core.mock.MockGpioFacade;
import ua.crowpi.core.mock.MockI2cFacade;
import ua.crowpi.core.mock.MockSensorReader;
import com.pi4j.io.i2c.I2CBus;
import ua.crowpi.core.pi4j.Pi4jGpioFacade;
import ua.crowpi.core.pi4j.Pi4jLcdFacade;

import ua.crowpi.core.matrix.MatrixScrollerComponent;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CrowPi educational project p01: <em>Thermometer &amp; Humidity Monitor</em>.
 *
 * <p>Reads the DHT11 sensor every 2 seconds and displays temperature and humidity
 * on the 16×2 LCD. The RGB LED reflects the thermal zone (blue = cold, green = comfortable,
 * red = hot). Two buttons allow the user to raise or lower the alert threshold at runtime.
 * When the temperature crosses the threshold, the buzzer emits three short beeps.
 * Every reading is logged to a CSV file for later analysis.</p>
 *
 * <p>Hardware used:</p>
 * <ul>
 *   <li>DHT11 sensor on BCM GPIO 4</li>
 *   <li>LCD 16×2 with I²C PCF8574 backpack at address 0x21</li>
 *   <li>RGB LED: red=GPIO22, green=GPIO27, blue=GPIO17</li>
 *   <li>Passive buzzer on GPIO 18 (hardware PWM0)</li>
 *   <li>Button 1 (raise threshold) on GPIO 11</li>
 *   <li>Button 2 (lower threshold) on GPIO 9</li>
 * </ul>
 *
 * <p>In {@code --mock} mode all hardware interactions are replaced with console log
 * output via {@link MockGpioFacade} and {@link MockI2cFacade}.</p>
 */
public class ThermometerProject implements CrowPiProject {

    // -------------------------------------------------------------------------
    // Константи GPIO / I2C
    // -------------------------------------------------------------------------

    /** I²C address of the PCF8574-based LCD backpack on the CrowPi. */
    private static final int LCD_ADDR = 0x21;

    /** HD44780 command: move cursor to the start of line 1 (DDRAM address 0x00). */
    private static final byte LCD_CMD_LINE1 = (byte) 0x80;

    /** HD44780 command: move cursor to the start of line 2 (DDRAM address 0x40). */
    private static final byte LCD_CMD_LINE2 = (byte) 0xC0;

    /** BCM GPIO pin for the RED channel of the RGB LED. */
    private static final int PIN_RGB_R = 22;

    /** BCM GPIO pin for the GREEN channel of the RGB LED. */
    private static final int PIN_RGB_G = 27;

    /** BCM GPIO pin for the BLUE channel of the RGB LED. */
    private static final int PIN_RGB_B = 17;

    /** BCM GPIO pin for the passive buzzer (hardware PWM0). */
    private static final int PIN_BUZZER = 18;

    /** BCM GPIO pin for Button 1 (raises the alert threshold). */
    private static final int PIN_BTN1 = 11;

    /** BCM GPIO pin for Button 2 (lowers the alert threshold). */
    private static final int PIN_BTN2 = 9;

    /** Buzzer frequency in Hz used for the alert beep tone. */
    private static final int BUZZER_FREQ_HZ = 2000;

    /** Duty cycle for the buzzer PWM signal (50% = square wave = loudest). */
    private static final float BUZZER_DUTY = 0.5f;

    /** Duration of each buzzer beep in milliseconds. */
    private static final int BEEP_ON_MS = 100;

    /** Silence gap between consecutive beeps in milliseconds. */
    private static final int BEEP_OFF_MS = 100;

    /** ISO-8601 formatter used when constructing TemperatureReading timestamps. */
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // -------------------------------------------------------------------------
    // Стан проекту
    // -------------------------------------------------------------------------

    /** GPIO facade — may be real (Pi4J) or mock depending on run mode. */
    private GpioFacade gpio;

    /** I²C facade for LCD communication. */
    private I2cFacade lcd;

    /** Sensor reader — either real Dht11Reader or a MockSensorReader. */
    private SensorReader<TemperatureReading> sensor;

    /** Scheduler that triggers periodic DHT11 reads. */
    private ScheduledExecutorService scheduler;

    /** CSV logger instance. */
    private CsvDataLogger csvLogger;

    /** Current alert threshold in °C; adjusted at runtime by button presses. */
    private volatile double alertThreshold;

    /** Last seen thermal zone, used to detect zone transitions for buzzer trigger. */
    private volatile ThermalZone lastZone;

    /** Flag controlling the main-thread wait loop; set to false by shutdown(). */
    private final AtomicBoolean running = new AtomicBoolean(false);

    private MatrixScrollerComponent matrixScroller;

    // -------------------------------------------------------------------------
    // Конструктори
    // -------------------------------------------------------------------------

    /**
     * Default no-argument constructor used by {@link java.util.ServiceLoader}
     * and for production runs via the launcher.
     */
    public ThermometerProject() {
        // Порожній конструктор — фасади ін'єктуються під час run()
    }

    /**
     * Dependency-injection constructor for unit testing.
     *
     * <p>Allows passing mock or stub implementations of the hardware facades
     * without starting any real hardware or scheduler.</p>
     *
     * @param gpio GPIO facade (mock or real)
     * @param lcd  I²C facade for the LCD (mock or real)
     */
    public ThermometerProject(GpioFacade gpio, I2cFacade lcd) {
        // Зберігаємо фасади для подальшого використання — тест контролює їх поведінку
        this.gpio = gpio;
        this.lcd  = lcd;
    }

    // -------------------------------------------------------------------------
    // CrowPiProject contract
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @return {@code "Thermometer &amp; Humidity Monitor"}
     */
    @Override
    public String getName() {
        return "Thermometer & Humidity Monitor";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "p01"}
     */
    @Override
    public String getProjectId() {
        return "p01";
    }

    /**
     * {@inheritDoc}
     *
     * @return a one-sentence description of this project
     */
    @Override
    public String getDescription() {
        return "Monitors ambient temperature and humidity with a DHT11 sensor, "
               + "shows readings on a 16x2 LCD, drives an RGB LED by thermal zone, "
               + "and logs data to CSV with configurable alert threshold.";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Initialises hardware facades (or mocks), registers button listeners,
     * starts the periodic sensor poll scheduler, and blocks until
     * {@link #shutdown()} is called or the thread is interrupted.</p>
     *
     * @param mockMode {@code true} to use mock facades; {@code false} for real RPi hardware
     * @throws HardwareException if real hardware cannot be initialised
     */
    @Override
    public void run(boolean mockMode) throws HardwareException {
        // Завантажуємо конфігурацію з thermometer.properties
        AlertConfig config = new AlertConfig();
        alertThreshold = config.getAlertThresholdCelsius();
        csvLogger      = new CsvDataLogger(config.getLogFile());

        // Вибираємо реальне або симульоване апаратне забезпечення
        boolean matrixMock = mockMode || (gpio != null);
        if (mockMode) {
            gpio   = new MockGpioFacade();
            lcd    = new MockI2cFacade();
            // Три симульовані показання циклічно: холодно → комфорт → жарко
            sensor = new MockSensorReader<>(
                    new TemperatureReading(15.0, 60.0, now()),
                    new TemperatureReading(23.0, 65.0, now()),
                    new TemperatureReading(32.0, 70.0, now())
            );
        } else {
            // На реальному RPi фасади повинні бути ін'єктовані через конструктор або Launcher;
            // якщо цього не відбулось — кидаємо виняток з підказкою
            if (gpio == null || lcd == null) {
                gpio = new Pi4jGpioFacade();
                lcd  = new Pi4jLcdFacade(I2CBus.BUS_1, 0x21);
            }
            sensor = new Dht11Reader(gpio);
        }

        // ── LED matrix scroller ─────────────────────────────────────────────────
        matrixScroller = new MatrixScrollerComponent(matrixMock);
        matrixScroller.start();

        // Реєструємо обробники кнопок — Button1 підвищує поріг, Button2 знижує
        gpio.addListener(PIN_BTN1, (pin, high) -> {
            if (high) {
                // Кожне натискання кнопки 1 підвищує поріг тривоги на 1°C
                alertThreshold += 1.0;
                System.out.printf("[p01] Button1: threshold raised to %.1f°C%n", alertThreshold);
            }
        });
        gpio.addListener(PIN_BTN2, (pin, high) -> {
            if (high) {
                // Кожне натискання кнопки 2 знижує поріг тривоги на 1°C
                alertThreshold -= 1.0;
                System.out.printf("[p01] Button2: threshold lowered to %.1f°C%n", alertThreshold);
            }
        });

        // Запускаємо планувальник — читаємо датчик кожні pollIntervalSeconds секунд
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "p01-sensor-poll");
            // Daemon-потік не блокує завершення JVM після виходу з run()
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(
                this::pollSensor,
                0,
                config.getPollIntervalSeconds(),
                TimeUnit.SECONDS
        );

        running.set(true);
        System.out.println("=== THERMOMETER & HUMIDITY MONITOR — p01 ===");
        System.out.println("Alert threshold: " + alertThreshold + "°C");
        System.out.println("Poll interval  : " + config.getPollIntervalSeconds() + "s");
        System.out.println("CSV log        : " + config.getLogFile());
        System.out.println("Press Ctrl+C to stop.");

        // Головний потік чекає доки running не стане false (при shutdown)
        while (running.get()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // Переривання означає зовнішній сигнал завершення
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stops the scheduler, turns off all GPIO outputs, and releases the GPIO facade.
     * Safe to call multiple times.</p>
     */
    @Override
    public void shutdown() {
        // Зупиняємо прапор основного циклу очікування
        running.set(false);

        // Зупиняємо планувальник — не чекаємо завершення поточного завдання
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }

        if (matrixScroller != null) {
            matrixScroller.stop();
            matrixScroller = null;
        }

        // Вимикаємо всі виходи перед закриттям GPIO
        if (gpio != null) {
            // Гасимо RGB LED та зупиняємо PWM
            gpio.pwm(PIN_BUZZER, 0, 0.0f);
            gpio.setOutput(PIN_RGB_R, false);
            gpio.setOutput(PIN_RGB_G, false);
            gpio.setOutput(PIN_RGB_B, false);
            gpio.close();
            gpio = null;
        }

        lcd    = null;
        sensor = null;
        System.out.println("[p01] Shutdown complete.");
    }

    // -------------------------------------------------------------------------
    // Внутрішня логіка опитування датчика
    // -------------------------------------------------------------------------

    /**
     * Reads one sample from the DHT11, updates the LCD, sets the RGB LED colour,
     * triggers the buzzer if the zone changed across the threshold, and appends
     * a row to the CSV log. Called by the scheduled executor every poll interval.
     */
    private void pollSensor() {
        TemperatureReading reading;
        try {
            reading = sensor.read();
        } catch (HardwareException e) {
            // Помилка датчика не зупиняє роботу — логуємо та пробуємо знову наступного разу
            System.err.println("[p01] Sensor read error: " + e.getMessage());
            return;
        }

        // Визначаємо теплову зону за поточною температурою
        ThermalZone zone = ThermalZone.forTemp(reading.getTemperatureC());

        // Оновлюємо дисплей та LED
        lcdWriteLine(0, LcdDisplayHelper.formatLine1(reading));
        lcdWriteLine(1, LcdDisplayHelper.formatLine2(zone));
        setRgbColor(zone == ThermalZone.HOT, zone == ThermalZone.COMFORT, zone == ThermalZone.COLD);

        // Перевіряємо перетин порогу — якщо зона змінилась і нова зона == HOT → сигналізуємо
        boolean thresholdCrossed = lastZone != null
                && lastZone != zone
                && reading.getTemperatureC() >= alertThreshold;
        if (thresholdCrossed) {
            // Перетин порогу — подаємо три коротких сигнали щоб привернути увагу
            System.out.printf("[p01] ALERT: temperature %.1f°C crossed threshold %.1f°C!%n",
                    reading.getTemperatureC(), alertThreshold);
            buzz(3);
        }
        lastZone = zone;

        // Записуємо показання у CSV; помилка запису не є критичною — лише попередження
        try {
            csvLogger.log(reading, zone, alertThreshold);
        } catch (IOException e) {
            System.err.println("[p01] CSV log warning: " + e.getMessage());
        }

        System.out.printf("[p01] %s  zone=%-7s  threshold=%.1f°C%n",
                reading, zone.name(), alertThreshold);
    }

    // -------------------------------------------------------------------------
    // Допоміжні методи
    // -------------------------------------------------------------------------

    /**
     * Emits a specified number of short buzzer beeps via hardware PWM on GPIO 18.
     *
     * <p>Each beep is {@value #BEEP_ON_MS} ms at {@value #BUZZER_FREQ_HZ} Hz,
     * followed by {@value #BEEP_OFF_MS} ms of silence.</p>
     *
     * @param times number of beeps to emit
     */
    private void buzz(int times) {
        for (int i = 0; i < times; i++) {
            // Вмикаємо PWM-сигнал на зумері — 2000 Гц, 50% duty cycle
            gpio.pwm(PIN_BUZZER, BUZZER_FREQ_HZ, BUZZER_DUTY);
            sleepMs(BEEP_ON_MS);
            // Вимикаємо сигнал — пауза між біпами
            gpio.pwm(PIN_BUZZER, 0, 0.0f);
            sleepMs(BEEP_OFF_MS);
        }
    }

    /**
     * Writes a text string to the specified LCD row by sending the cursor-position
     * command followed by each character byte via the I²C facade.
     *
     * <p>Register {@code 0x00} is used for HD44780 commands;
     * register {@code 0x01} is used for character data.</p>
     *
     * @param row  0-based row index (0 = top line, 1 = bottom line)
     * @param text exactly 16-character string to display; see {@link LcdDisplayHelper#pad16}
     */
    private void lcdWriteLine(int row, String text) {
        // Вибираємо відповідну адресу DDRAM для рядка 0 або 1
        byte cursorCmd = (row == 0) ? LCD_CMD_LINE1 : LCD_CMD_LINE2;
        // Регістр 0x00 — команда HD44780 (встановлення позиції курсора)
        lcd.writeByte(LCD_ADDR, 0x00, cursorCmd);

        // Записуємо кожен символ рядка у регістр 0x01 (символьні дані)
        for (int i = 0; i < text.length(); i++) {
            lcd.writeByte(LCD_ADDR, 0x01, (byte) text.charAt(i));
        }
    }

    /**
     * Sets the RGB LED colour by driving each channel HIGH or LOW.
     *
     * <p>The CrowPi RGB LED uses a common-cathode configuration so a HIGH signal
     * turns the channel ON and LOW turns it OFF.</p>
     *
     * @param r {@code true} to enable the red channel (HOT zone indicator)
     * @param g {@code true} to enable the green channel (COMFORT zone indicator)
     * @param b {@code true} to enable the blue channel (COLD zone indicator)
     */
    private void setRgbColor(boolean r, boolean g, boolean b) {
        // Встановлюємо кожен канал RGB окремо — логіка зони вже визначена в pollSensor
        gpio.setOutput(PIN_RGB_R, r);
        gpio.setOutput(PIN_RGB_G, g);
        gpio.setOutput(PIN_RGB_B, b);
    }

    /**
     * Returns the current timestamp formatted as ISO-8601.
     *
     * @return timestamp string in {@code "yyyy-MM-dd'T'HH:mm:ss"} format
     */
    private static String now() {
        return LocalDateTime.now().format(TIMESTAMP_FMT);
    }

    /**
     * Sleeps for the given number of milliseconds, absorbing any
     * {@link InterruptedException} and restoring the thread interrupt flag.
     *
     * @param ms sleep duration in milliseconds
     */
    private static void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // Відновлюємо прапор переривання — не «ковтаємо» переривання безслідно
            Thread.currentThread().interrupt();
        }
    }
}
