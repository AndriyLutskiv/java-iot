package ua.crowpi.projects.p07;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

/**
 * Automatic Greenhouse Controller — CrowPi educational project p07.
 *
 * <p>Demonstrates:</p>
 * <ul>
 *   <li>SPI soil moisture sensor simulation (MCP3008) — {@link SoilMoistureSensor}</li>
 *   <li>DHT11 temperature/humidity sensor — {@link MockSensorReader} in mock mode</li>
 *   <li>Rules engine for actuator decisions — {@link GreenhouseController}</li>
 *   <li>CSV data logging with Apache Commons CSV — {@link CsvDataLogger}</li>
 *   <li>Scheduled monitoring every 30 seconds — {@link ScheduledExecutorService}</li>
 *   <li>Manual override via button press — {@link ManualOverride}</li>
 *   <li>LCD 16×2 display update — via {@link I2cFacade}</li>
 *   <li>RGB LED status indicator — via {@link GpioFacade}</li>
 * </ul>
 *
 * <p><strong>LCD display format:</strong></p>
 * <pre>
 *   Line 1: "SOIL:45% T:24.1C"
 *   Line 2: "PUMP:OFF  FAN:ON "
 * </pre>
 *
 * <p><strong>RGB colour coding:</strong></p>
 * <ul>
 *   <li>Green  — normal, no action needed</li>
 *   <li>Blue   — watering (pump on, fan off)</li>
 *   <li>Red    — overheated (fan on, pump off)</li>
 *   <li>Yellow — both pump and fan active</li>
 * </ul>
 */
public class GreenhouseProject implements CrowPiProject {

    private static final Logger LOG = LogManager.getLogger(GreenhouseProject.class);

    // -------------------------------------------------------------------------
    // Hardware pin constants
    // -------------------------------------------------------------------------

    /** DHT11 temperature/humidity sensor is connected to BCM GPIO 4. */
    private static final int DHT11_PIN = 4;

    /** Button for manual pump override is connected to BCM GPIO 11. */
    private static final int BUTTON_PIN = 11;

    /** RGB LED pin for red channel (BCM GPIO 22). */
    private static final int RGB_RED_PIN   = 22;

    /** RGB LED pin for green channel (BCM GPIO 27). */
    private static final int RGB_GREEN_PIN = 27;

    /** RGB LED pin for blue channel (BCM GPIO 17). */
    private static final int RGB_BLUE_PIN  = 17;

    /** I2C address of the PCF8574 LCD backpack. */
    private static final int LCD_ADDR = 0x21;

    /** Formatter for ISO-8601-like timestamp strings written to CSV. */
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // -------------------------------------------------------------------------
    // Runtime state — ініціалізуються в run(), звільняються в shutdown()
    // -------------------------------------------------------------------------

    // volatile — прапорець читається головним потоком і може скидатися з shutdown()
    private volatile boolean running = false;

    private GpioFacade gpio;
    private I2cFacade  lcd;

    private ThresholdConfig      config;
    private ManualOverride        manualOverride;
    private RelayBoard            relayBoard;
    private SoilMoistureSensor    soilSensor;
    private SensorReader<double[]> dhtReader;
    private GreenhouseController  controller;
    private CsvDataLogger         csvLogger;

    private ScheduledExecutorService scheduler;

    private MatrixScrollerComponent matrixScroller;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Default constructor for production use and {@link java.util.ServiceLoader} discovery.
     * Hardware facades are created lazily inside {@link #run(boolean)}.
     */
    public GreenhouseProject() {
        // Фасади і компоненти будуть створені у run() — стандартний шаблон проекту
    }

    /**
     * Dependency-injection constructor for unit testing.
     *
     * <p>Allows injecting mock or real hardware facades directly, bypassing
     * the mock/real branching in {@link #run(boolean)}.</p>
     *
     * @param gpio GPIO facade (may be a mock)
     * @param lcd  I2C facade for the LCD (may be a mock)
     */
    public GreenhouseProject(GpioFacade gpio, I2cFacade lcd) {
        this.gpio = gpio;
        this.lcd  = lcd;
    }

    // -------------------------------------------------------------------------
    // CrowPiProject contract
    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "Automatic Greenhouse";
    }

    /** {@inheritDoc} */
    @Override
    public String getProjectId() {
        return "p07";
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "Automated plant care system that monitors soil moisture and temperature,"
                + " controlling a pump and fan via relay based on configurable thresholds.";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Initialises all hardware facades and components, registers the button listener,
     * schedules the sensor polling task, then enters the idle main loop
     * until {@link #shutdown()} is called.</p>
     *
     * @param mockMode {@code true} to use {@link MockGpioFacade} and {@link MockI2cFacade}
     * @throws HardwareException if real hardware initialisation fails
     */
    @Override
    public void run(boolean mockMode) throws HardwareException {
        running = true;
        LOG.info("GreenhouseProject starting (mockMode={})", mockMode);

        // ----------------------------------------------------------------
        // 1. Завантажуємо конфігурацію з classpath-ресурсу
        // ----------------------------------------------------------------
        config = new ThresholdConfig();

        // ----------------------------------------------------------------
        // 2. Ініціалізуємо hardware-фасади якщо не були ін'єктовані ззовні
        // ----------------------------------------------------------------
        boolean matrixMock = (gpio != null) || mockMode;
        if (gpio == null) {
            if (mockMode) {
                gpio = new MockGpioFacade();
                lcd  = new MockI2cFacade();
            } else {
                // Реальне Pi4J GPIO — студент підключає пізніше
                gpio = new Pi4jGpioFacade();
                lcd  = new Pi4jLcdFacade(I2CBus.BUS_1, 0x21);
            }
        }

        // ── LED matrix scroller ─────────────────────────────────────────────────
        matrixScroller = new MatrixScrollerComponent(matrixMock);
        matrixScroller.start();

        // ----------------------------------------------------------------
        // 3. Ініціалізуємо компоненти проекту
        // ----------------------------------------------------------------
        manualOverride = new ManualOverride();
        relayBoard     = new RelayBoard(gpio, config.getPumpRelayPin(), config.getFanRelayPin());
        soilSensor     = new SoilMoistureSensor(gpio);
        controller     = new GreenhouseController(config, manualOverride);
        csvLogger      = new CsvDataLogger(config.getLogFile());

        // DHT11 mock: cycling sequence of [tempC, humidityPct] pairs as double[2]
        // Значення спеціально включають температуру вище 30°C для демонстрації вентилятора
        dhtReader = new MockSensorReader<>(
                new double[]{24.1, 55.0},
                new double[]{26.3, 60.0},
                new double[]{31.5, 65.0},  // > 30°C → fan ON
                new double[]{28.7, 58.0},
                new double[]{22.0, 50.0}
        );

        // ----------------------------------------------------------------
        // 4. Реєструємо listener кнопки (GPIO 11) для ручного перевизначення насосу
        // ----------------------------------------------------------------
        gpio.addListener(BUTTON_PIN, (pin, high) -> {
            // Реагуємо тільки на спадаючий фронт (відпускання кнопки) — HIGH=false
            // У mock режимі listener вогонь HIGH=true, тому реагуємо на будь-який край
            if (high) {
                manualOverride.togglePump();
                LOG.debug("Button pressed on pin {} — pump override toggled to {}",
                        pin, manualOverride.isPumpForced());
            }
        });

        // ----------------------------------------------------------------
        // 5. Ініціалізуємо LCD і відображаємо стартове повідомлення
        // ----------------------------------------------------------------
        lcdInit();
        lcdWriteLine(0, "Greenhouse Ready");
        lcdWriteLine(1, "Starting...");

        // ----------------------------------------------------------------
        // 6. Запускаємо планувальник опитування сенсорів
        // ----------------------------------------------------------------
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "greenhouse-poll");
            // Daemon — не блокує завершення JVM якщо shutdown() не викликати вчасно
            t.setDaemon(true);
            return t;
        });

        // scheduleAtFixedRate: перше спрацювання негайно (delay=0), потім кожні N секунд
        scheduler.scheduleAtFixedRate(
                this::pollAndControl,
                0,
                config.getPollIntervalSeconds(),
                TimeUnit.SECONDS
        );

        LOG.info("Greenhouse polling scheduler started (interval={}s)",
                config.getPollIntervalSeconds());

        // ----------------------------------------------------------------
        // 7. Головний цикл — чекаємо поки running=true
        // ----------------------------------------------------------------
        while (running) {
            sleepMs(500);
        }

        LOG.info("GreenhouseProject main loop exited");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stops the scheduler, turns off all relays, resets manual overrides,
     * and releases GPIO resources. Idempotent — safe to call multiple times.</p>
     */
    @Override
    public void shutdown() {
        if (!running && scheduler == null) {
            // Ідемпотентність — повторний виклик безпечний
            return;
        }
        running = false;

        // Зупиняємо планувальник — чекаємо до 2 секунд завершення поточного завдання
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                scheduler.shutdownNow();
            }
        }

        // Вимикаємо всі реле для безпеки — насос і вентилятор мають зупинитись
        if (relayBoard != null) {
            relayBoard.allOff();
        }

        // Скидаємо ручні перевизначення
        if (manualOverride != null) {
            manualOverride.reset();
        }

        // Вимикаємо RGB LED — вся підсвітка має зникнути при завершенні
        if (gpio != null) {
            gpio.setOutput(RGB_RED_PIN,   false);
            gpio.setOutput(RGB_GREEN_PIN, false);
            gpio.setOutput(RGB_BLUE_PIN,  false);
            gpio.close();
        }

        LOG.info("GreenhouseProject shutdown complete");
    }

    // -------------------------------------------------------------------------
    // Core polling task — виконується планувальником кожні pollIntervalSeconds
    // -------------------------------------------------------------------------

    /**
     * Reads all sensors, evaluates the rules engine, applies actuator state,
     * updates the LCD and RGB LED, and logs data to CSV.
     *
     * <p>This method is executed by the {@link ScheduledExecutorService} and must
     * not block for longer than the poll interval. Any exception is caught and
     * logged to prevent the scheduler from silently stopping.</p>
     */
    private void pollAndControl() {
        try {
            // --- a) Зчитуємо датчики ---
            double[] dht = dhtReader.read();
            double tempC    = dht[0];
            double humidity = dht[1];
            int soilPercent = soilSensor.readPercent();

            // --- b) Формуємо рядок часової мітки для CSV і POJO ---
            String timestamp = LocalDateTime.now().format(TS_FMT);

            // --- c) Створюємо знімок показань ---
            GreenhouseReading reading = new GreenhouseReading(tempC, humidity, soilPercent, timestamp);

            // --- d) Виконуємо правила і отримуємо бажаний стан актуаторів ---
            ActuatorState desired = controller.evaluate(reading);

            // --- e) Застосовуємо стан до реле-плати ---
            relayBoard.apply(desired);

            // --- f) Оновлюємо LCD ---
            updateLcd(reading, desired);

            // --- g) Оновлюємо RGB LED за станом ---
            updateRgb(desired);

            // --- h) Записуємо в CSV ---
            csvLogger.log(reading, desired);

            LOG.info("Poll: {} | soil={}% | temp={} | state={}",
                    timestamp, soilPercent, tempC, desired);

        } catch (HardwareException e) {
            LOG.error("Hardware error during greenhouse poll: {}", e.getMessage(), e);
        } catch (IOException e) {
            LOG.error("CSV logging error: {}", e.getMessage(), e);
        } catch (Exception e) {
            // Ловимо всі винятки — планувальник не повинен зупинитись через несподівану помилку
            LOG.error("Unexpected error in poll task: {}", e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Display helpers
    // -------------------------------------------------------------------------

    /**
     * Updates both LCD lines with the latest sensor reading and actuator state.
     *
     * <p>Line 1 format: {@code "SOIL:45% T:24.1C"}  (16 chars)</p>
     * <p>Line 2 format: {@code "PUMP:OFF  FAN:ON "} (16 chars)</p>
     *
     * @param r the latest sensor reading
     * @param s the current actuator state
     */
    private void updateLcd(GreenhouseReading r, ActuatorState s) {
        // Рядок 1: вологість ґрунту і температура — найважливіші показники
        String line1 = String.format("SOIL:%d%% T:%.1fC", r.getSoilPercent(), r.getTempC());

        // Рядок 2: поточний стан насосу і вентилятора — для оператора
        String pumpStr = s.isPumpOn() ? "ON " : "OFF";
        String fanStr  = s.isFanOn()  ? "ON " : "OFF";
        String line2   = String.format("PUMP:%-3s  FAN:%-3s", pumpStr, fanStr);

        lcdWriteLine(0, line1);
        lcdWriteLine(1, line2);
    }

    /**
     * Sets the RGB LED colour to indicate the current operational status.
     *
     * <ul>
     *   <li>Green  — all off, normal conditions</li>
     *   <li>Blue   — watering only (pump on, fan off)</li>
     *   <li>Red    — overheated (fan on, pump off)</li>
     *   <li>Yellow — both pump and fan active (red + green = yellow)</li>
     * </ul>
     *
     * @param s the current actuator state driving colour selection
     */
    private void updateRgb(ActuatorState s) {
        // Кольори відповідають смисловій кодовій схемі:
        // зелений = норма, синій = полив, червоний = перегрів, жовтий = обидва
        switch (s) {
            case ALL_OFF:
                // Зелений — система в нормі, жодна дія не потрібна
                setRgbColor(false, true, false);
                break;
            case PUMP_ON:
                // Синій — активний полив (грунт сухий)
                setRgbColor(false, false, true);
                break;
            case FAN_ON:
                // Червоний — перегрів, охолодження вмикається
                setRgbColor(true, false, false);
                break;
            case BOTH_ON:
                // Жовтий = червоний + зелений — і полив, і охолодження одночасно
                setRgbColor(true, true, false);
                break;
            default:
                // Захисний default — вимикаємо всі канали
                setRgbColor(false, false, false);
                break;
        }
    }

    /**
     * Writes a text line to a specific row of the 16×2 LCD via the I2C facade.
     *
     * <p>The text is padded or truncated to exactly 16 characters to fill
     * the LCD row cleanly.</p>
     *
     * @param row  0 for the top row, 1 for the bottom row
     * @param text content to display (padded/truncated to 16 chars)
     */
    private void lcdWriteLine(int row, String text) {
        // 0x80 = команда встановлення курсора на рядок 0 (адреса DDRAM 0x00)
        // 0xC0 = команда встановлення курсора на рядок 1 (адреса DDRAM 0x40)
        byte rowCmd = (row == 0) ? (byte) 0x80 : (byte) 0xC0;
        lcd.writeByte(LCD_ADDR, 0x00, rowCmd);

        // Нормалізуємо до рівно 16 символів: обрізаємо якщо довше, доповнюємо пробілами якщо коротше
        String padded = String.format("%-16.16s", text != null ? text : "");
        for (char c : padded.toCharArray()) {
            lcd.writeByte(LCD_ADDR, 0x01, (byte) c);
        }
    }

    /**
     * Sends the LCD initialisation command sequence to prepare the display.
     */
    private void lcdInit() {
        // Команди ініціалізації HD44780 через PCF8574 I2C backpack
        lcd.writeByte(LCD_ADDR, 0x00, (byte) 0x01); // Clear display
        sleepMs(5);
        lcd.writeByte(LCD_ADDR, 0x00, (byte) 0x0C); // Display ON, cursor OFF
    }

    /**
     * Drives the three RGB LED pins to produce the requested colour.
     *
     * @param red   {@code true} to activate the red component
     * @param green {@code true} to activate the green component
     * @param blue  {@code true} to activate the blue component
     */
    private void setRgbColor(boolean red, boolean green, boolean blue) {
        gpio.setOutput(RGB_RED_PIN,   red);
        gpio.setOutput(RGB_GREEN_PIN, green);
        gpio.setOutput(RGB_BLUE_PIN,  blue);
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    /**
     * Sleeps the current thread for the given milliseconds, preserving the
     * interrupt flag if the sleep is interrupted.
     *
     * @param ms milliseconds to sleep; negative values are treated as 0
     */
    private static void sleepMs(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // Відновлюємо прапорець переривання — не гасимо його тихо
            Thread.currentThread().interrupt();
        }
    }
}
