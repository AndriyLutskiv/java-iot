package ua.crowpi.projects.p09;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.CrowPiProject;
import ua.crowpi.core.exception.DatabaseException;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Smart Home Assistant — CrowPi educational project p09.
 *
 * <p>Demonstrates:</p>
 * <ul>
 *   <li>SQLite persistence via JDBC (repository pattern)</li>
 *   <li>Multi-threaded temperature auto-control ({@link AutoTempControl})</li>
 *   <li>Menu-driven UI on a 16×2 LCD with keypad / IR navigation</li>
 *   <li>{@link java.util.concurrent.ScheduledExecutorService} for timed device shutdown</li>
 * </ul>
 *
 * <p><strong>Startup sequence:</strong></p>
 * <ol>
 *   <li>Open SQLite database ({@code smartHome.db}) and create tables if absent.</li>
 *   <li>Seed default profiles (DAY 26 °C, NIGHT 22 °C, VACATION 30 °C) if not present.</li>
 *   <li>Load the DAY profile as the active one.</li>
 *   <li>Initialise hardware facades (real or mock).</li>
 *   <li>Build the 7-item {@link MenuNavigator} and start {@link AutoTempControl}.</li>
 *   <li>Run the menu loop until the user presses BACK.</li>
 * </ol>
 */
public class SmartHomeProject implements CrowPiProject {

    private static final Logger LOG = LogManager.getLogger(SmartHomeProject.class);

    private static final String DB_FILE  = "smartHome.db";
    private static final String PROJECT_ID = "smart-home";

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // -------------------------------------------------------------------------
    // Runtime resources (initialised in run(), released in shutdown())
    // -------------------------------------------------------------------------

    private DatabaseManager dbManager;
    private GpioFacade gpio;
    private I2cFacade lcd;
    private LightController lightController;
    private FanController fanController;
    private AutoTempControl autoTempControl;
    private TimerMenuItem timerMenuItem;
    private volatile boolean running = false;

    private MatrixScrollerComponent matrixScroller;

    // -------------------------------------------------------------------------
    // CrowPiProject contract
    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "Smart Home Assistant";
    }

    /** {@inheritDoc} */
    @Override
    public String getProjectId() {
        return PROJECT_ID;
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "Keypad/IR menu to control relays and DHT11 with SQLite event logging.";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Initialises the database, hardware, and menu, then blocks in the menu loop.</p>
     *
     * @param mockMode {@code true} to use in-memory SQLite and mock hardware facades
     */
    @Override
    public void run(boolean mockMode) throws HardwareException {
        running = true;
        LOG.info("SmartHomeProject starting (mockMode={})", mockMode);

        // ----------------------------------------------------------------
        // 1. Database setup
        // ----------------------------------------------------------------
        try {
            if (mockMode) {
                // in-memory SQLite — не торкаємось файлової системи
                dbManager = DatabaseManager.forFile(":memory:");
            } else {
                dbManager = DatabaseManager.forFile(DB_FILE);
            }
            dbManager.createTables();
        } catch (DatabaseException e) {
            throw new HardwareException("Database initialisation failed: " + e.getMessage(), e);
        }

        ProfileRepository profileRepo = new ProfileRepository(dbManager.getConnection());
        DeviceEventRepository eventRepo = new DeviceEventRepository(dbManager.getConnection());

        try {
            dbManager.seedDefaultProfiles(profileRepo);
        } catch (DatabaseException e) {
            LOG.warn("Seed profiles failed (non-fatal): {}", e.getMessage());
        }

        // ----------------------------------------------------------------
        // 2. Load active profile (DAY)
        // ----------------------------------------------------------------
        Profile activeProfile = loadActiveProfile(profileRepo);
        DeviceState deviceState = new DeviceState(
                activeProfile.isLightOn(),
                activeProfile.isFanOn(),
                activeProfile.getName());

        // ----------------------------------------------------------------
        // 3. Hardware facades
        // ----------------------------------------------------------------
        if (mockMode) {
            gpio = new MockGpioFacade();
            lcd  = new MockI2cFacade();
        } else {
            // На реальному RPi 3 з Pi4J 1.4 тут буде реальна реалізація
            // Поки кидаємо пояснювальний виняток якщо реальне залізо не реалізовано
            gpio = new Pi4jGpioFacade();
            lcd  = new Pi4jLcdFacade(I2CBus.BUS_1, 0x21);
        }

        // ── LED matrix scroller ─────────────────────────────────────────────────
        matrixScroller = new MatrixScrollerComponent(mockMode);
        matrixScroller.start();

        lightController = new LightController(gpio);
        fanController   = new FanController(gpio);

        // Ініціалізуємо LCD (посилаємо reset + налаштування 4-bit режиму)
        LcdHelper.init(lcd);

        // ----------------------------------------------------------------
        // 4. DHT11 sensor (mock: sinusoidal 18–28 °C)
        // ----------------------------------------------------------------
        SensorReader<DhtReading> dhtSensor;
        if (mockMode) {
            // Синусоїдальний цикл температур для демонстрації автоконтролю
            dhtSensor = new MockSensorReader<>(
                    new DhtReading(20.0, 50.0),
                    new DhtReading(23.5, 55.0),
                    new DhtReading(26.5, 60.0), // > DAY threshold → fan ON
                    new DhtReading(28.0, 65.0), // > всіх threshold
                    new DhtReading(24.0, 58.0),
                    new DhtReading(21.0, 52.0)
            );
        } else {
            dhtSensor = new Dht11Reader(gpio);
        }

        // ----------------------------------------------------------------
        // 5. AutoTempControl background thread
        // ----------------------------------------------------------------
        autoTempControl = new AutoTempControl(dhtSensor, fanController,
                eventRepo, deviceState, activeProfile);
        autoTempControl.start();

        // ----------------------------------------------------------------
        // 6. Navigation input
        // ----------------------------------------------------------------
        NavigationInput navInput;
        if (mockMode) {
            navInput = new ConsoleNavigationInput();
        } else {
            navInput = new InputRouter(gpio);
        }

        // ----------------------------------------------------------------
        // 7. Build menu
        // ----------------------------------------------------------------
        MenuNavigator menu = new MenuNavigator(lcd);
        menu.addItem(new TemperatureMenuItem(dhtSensor, lcd, deviceState));
        menu.addItem(new LightMenuItem(lightController, deviceState, eventRepo, lcd));
        menu.addItem(new FanMenuItem(fanController, deviceState, eventRepo, lcd));

        timerMenuItem = new TimerMenuItem(lightController, fanController,
                deviceState, eventRepo, lcd, navInput);
        menu.addItem(timerMenuItem);

        menu.addItem(new SwitchProfileMenuItem(profileRepo, eventRepo,
                deviceState, autoTempControl, lcd));
        menu.addItem(new ViewEventLogMenuItem(eventRepo, lcd, navInput));
        menu.addItem(new SaveStateMenuItem(profileRepo, deviceState, lcd));

        // Стартове повідомлення на LCD
        LcdHelper.writeLine(lcd, 0, "SMART HOME      ");
        LcdHelper.writeLine(lcd, 1, "Loading...      ");

        // Логуємо запуск системи
        logSystemEvent(eventRepo, "SYSTEM_START", activeProfile.getId());

        // ----------------------------------------------------------------
        // 8. Run menu loop (blocks until BACK)
        // ----------------------------------------------------------------
        menu.run(navInput);

        LOG.info("SmartHomeProject menu exited normally");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stops background threads, cancels timers, and closes the database.</p>
     */
    @Override
    public void shutdown() {
        if (!running) return; // ідемпотентність — повторний виклик безпечний
        running = false;

        if (autoTempControl != null) {
            autoTempControl.stopMonitoring();
        }
        if (timerMenuItem != null) {
            timerMenuItem.cancel();
        }
        if (lightController != null) {
            lightController.turnOff();
        }
        if (fanController != null) {
            fanController.turnOff();
        }
        if (gpio != null) {
            gpio.close();
        }
        if (dbManager != null) {
            try {
                dbManager.close();
            } catch (DatabaseException e) {
                LOG.error("DB close error: {}", e.getMessage());
            }
        }
        LOG.info("SmartHomeProject shutdown complete");
    }

    // -------------------------------------------------------------------------
    // Допоміжні методи
    // -------------------------------------------------------------------------

    /**
     * Loads the DAY profile from the database; falls back to a hardcoded default
     * if the DB is empty (e.g. seed failed).
     */
    private Profile loadActiveProfile(ProfileRepository repo) {
        try {
            Optional<Profile> day = repo.findByName("DAY");
            if (day.isPresent()) {
                LOG.info("Active profile loaded: {}", day.get());
                return day.get();
            }
        } catch (DatabaseException e) {
            LOG.warn("Could not load DAY profile: {}", e.getMessage());
        }
        // Fallback — дефолтний профіль без запису в БД
        LOG.warn("Using hardcoded fallback profile (DAY 26°C)");
        return new Profile(0, "DAY", 26.0, false, false,
                LocalDateTime.now().format(TS_FMT));
    }

    /** Logs a SYSTEM event ignoring errors (startup/shutdown marker). */
    private void logSystemEvent(DeviceEventRepository repo,
                                String eventType, int profileId) {
        DeviceEvent event = DeviceEvent.of(
                eventType, "SYSTEM", null, "RUNNING",
                profileId > 0 ? profileId : null,
                LocalDateTime.now().format(TS_FMT));
        try {
            repo.insert(event);
        } catch (DatabaseException e) {
            LOG.debug("Could not log system event: {}", e.getMessage());
        }
    }
}
