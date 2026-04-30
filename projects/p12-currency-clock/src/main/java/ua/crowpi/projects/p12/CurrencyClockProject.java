package ua.crowpi.projects.p12;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.pi4j.io.i2c.I2CBus;
import ua.crowpi.core.CrowPiProject;
import ua.crowpi.core.exception.HardwareException;
import ua.crowpi.core.hardware.I2cFacade;
import ua.crowpi.core.mock.MockI2cFacade;
import ua.crowpi.core.pi4j.Pi4jLcdFacade;

import ua.crowpi.core.matrix.MatrixScrollerComponent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CrowPi educational project p12: <em>Currency &amp; Clock Display</em>.
 *
 * <p>Alternates the 16×2 I²C LCD between two pages:</p>
 * <ol>
 *   <li><strong>Clock page</strong> (5 seconds) — current local time on row 0,
 *       current date on row 1; updated every second while active.</li>
 *   <li><strong>Currency page</strong> (5 seconds) — USD/UAH rate on row 0,
 *       EUR/UAH rate on row 1; fetched fresh from the National Bank of Ukraine
 *       REST API on each entry to this page.</li>
 * </ol>
 *
 * <p>The two pages repeat indefinitely in a loop until the project is stopped.</p>
 *
 * <p>API endpoint used:</p>
 * <pre>
 * GET https://bank.gov.ua/NBU_Exchange/exchange?json
 * </pre>
 *
 * <p>In {@code --mock} mode, a {@link MockI2cFacade} logs LCD output to the console
 * and a {@link MockNbuApiClient} returns static pre-defined rates without any
 * network access.</p>
 *
 * <p>Hardware used:</p>
 * <ul>
 *   <li>LCD 16×2 with PCF8574 I²C backpack at address 0x27 (CrowPi on-board)</li>
 * </ul>
 */
public class CurrencyClockProject implements CrowPiProject {

    private static final Logger LOG = LogManager.getLogger(CurrencyClockProject.class);

    // -------------------------------------------------------------------------
    // Стан проекту
    // -------------------------------------------------------------------------

    /**
     * LCD I²C facade — mock or real Pi4j.
     * Kept as {@link Pi4jI2cFacade} when in hardware mode so we can call {@link Pi4jI2cFacade#close()}.
     */
    private I2cFacade lcd;

    /** Клієнт NBU API — реальний або mock залежно від режиму запуску. */
    private NbuApiClient apiClient;

    /** Контролер відображення — виконує логіку тіку. */
    private DisplayController controller;

    /** Планувальник, що викликає tick() щосекунди. */
    private ScheduledExecutorService scheduler;

    /** Прапор керування головним циклом очікування. */
    private final AtomicBoolean running = new AtomicBoolean(false);

    private MatrixScrollerComponent matrixScroller;

    // -------------------------------------------------------------------------
    // Конструктори
    // -------------------------------------------------------------------------

    /**
     * Default no-argument constructor used by {@link java.util.ServiceLoader}
     * and the CrowPi launcher.
     */
    public CurrencyClockProject() {
        // Порожній конструктор — фасади ін'єктуються під час run()
    }

    /**
     * Dependency-injection constructor for unit testing.
     *
     * <p>Allows injecting a mock I²C facade and a mock NBU API client without
     * starting any scheduler threads.</p>
     *
     * @param lcd       I²C facade for the LCD
     * @param apiClient NBU exchange-rate API client
     */
    CurrencyClockProject(I2cFacade lcd, NbuApiClient apiClient) {
        // Зберігаємо залежності для тестів — run() використає їх замість ініціалізації
        this.lcd       = lcd;
        this.apiClient = apiClient;
    }

    // -------------------------------------------------------------------------
    // CrowPiProject contract
    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getName() { return "Currency & Clock Display"; }

    /** {@inheritDoc} */
    @Override
    public String getProjectId() { return "p12"; }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "Alternates a 16x2 LCD between live time/date and USD+EUR/UAH rates "
               + "fetched from the National Bank of Ukraine REST API (5 seconds each).";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Initialises the I²C LCD facade and NBU API client (real or mock),
     * creates the {@link DisplayController}, starts the 1-second tick scheduler,
     * then blocks until {@link #shutdown()} is called.</p>
     *
     * @param mockMode {@code true} → use {@link MockI2cFacade} and {@link MockNbuApiClient};
     *                 {@code false} → use real I²C hardware and live NBU API
     * @throws HardwareException if the real I²C facade has not been injected in hardware mode
     */
    @Override
    public void run(boolean mockMode) throws HardwareException {
        // Ініціалізуємо залежності якщо їх не було передано через DI-конструктор
        if (lcd == null) {
            if (mockMode) {
                lcd       = new MockI2cFacade();
                apiClient = new MockNbuApiClient();
                LOG.info("Running in mock mode — no LCD hardware or network required.");
            } else {
                // Реальний режим: відкриваємо I²C шину 1 (/dev/i2c-1) через Pi4J
                // CrowPi LCD PCF8574 backpack завжди на шині 1
                lcd       = new Pi4jLcdFacade(I2CBus.BUS_1, 0x21);
                apiClient = new RealNbuApiClient();
                LOG.info("Running in hardware mode — I2C bus 1, NBU REST API.");
            }
        }

        // ── LED matrix scroller ─────────────────────────────────────────────────
        matrixScroller = new MatrixScrollerComponent(mockMode);
        matrixScroller.start();

        controller = new DisplayController(lcd, apiClient);

        // Daemon-планувальник — не блокує завершення JVM
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "p12-display-tick");
            t.setDaemon(true);
            return t;
        });
        // Перший тік через 0 секунд — дисплей оновлюється відразу при запуску
        scheduler.scheduleAtFixedRate(controller::tick, 0, 1, TimeUnit.SECONDS);

        running.set(true);
        LOG.info("CurrencyClockProject started. Clock and currency alternate every {} seconds.",
                DisplayController.DISPLAY_SECONDS);
        System.out.println("=== CURRENCY & CLOCK DISPLAY — p12 ===");
        System.out.println("Alternating every " + DisplayController.DISPLAY_SECONDS
                + "s: [CLOCK] ↔ [CURRENCY (USD/EUR)]");
        System.out.println("Press Ctrl+C to stop.");

        // Головний потік чекає поки running не стане false
        while (running.get()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stops the tick scheduler and releases the I²C facade.
     * Safe to call multiple times.</p>
     */
    @Override
    public void shutdown() {
        // Зупиняємо прапор основного циклу
        running.set(false);

        // Зупиняємо планувальник — не чекаємо завершення поточного тіку
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }

        // Закриваємо I²C шину якщо використовувався Pi4jI2cFacade (реальне залізо)
        if (lcd instanceof Pi4jLcdFacade) {
            try {
                ((Pi4jLcdFacade) lcd).close();
            } catch (Exception e) {
                LOG.warn("Error closing I2C bus: {}", e.getMessage());
            }
        }
        lcd        = null;
        apiClient  = null;
        controller = null;
        LOG.info("CurrencyClockProject shutdown complete.");
    }
}
