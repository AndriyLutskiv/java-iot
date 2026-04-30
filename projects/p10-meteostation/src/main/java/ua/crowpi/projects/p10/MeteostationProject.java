package ua.crowpi.projects.p10;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.CrowPiProject;
import ua.crowpi.core.exception.HardwareException;
import ua.crowpi.core.hardware.GpioFacade;
import ua.crowpi.core.hardware.I2cFacade;
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
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Weather Station with Trend Forecast — CrowPi educational project p10.
 *
 * <p>Demonstrates:</p>
 * <ul>
 *   <li>Generic {@link RingBuffer} for a 3-hour sliding window of readings</li>
 *   <li>Ordinary least-squares linear regression ({@link TrendAnalyzer}) over historical data</li>
 *   <li>Rothfusz Heat Index formula ({@link HeatIndex})</li>
 *   <li>HTML report generation ({@link HtmlReportGenerator})</li>
 *   <li>Apache Commons CSV data logging ({@link CsvDataLogger})</li>
 * </ul>
 */
public class MeteostationProject implements CrowPiProject {

    private static final Logger LOG = LogManager.getLogger(MeteostationProject.class);

    private static final int LCD_ADDR = 0x21;

    /** GPIO pin of the sound/clap sensor (digital output HIGH on noise). */
    private static final int SOUND_PIN  = 17;
    /** GPIO pin of the tilt sensor (HIGH when tilted = wind proxy). */
    private static final int TILT_PIN   = 27;
    /** GPIO pin of the report-generation button. */
    private static final int BUTTON_PIN = 11;

    /** RGB LED pins. */
    private static final int RGB_R = 22, RGB_G = 6, RGB_B = 13;

    /** Poll interval for mock mode (shorter so demo is visible). */
    private static final long MOCK_POLL_SEC = 5L;
    /** Poll interval for production (every 60 seconds). */
    private static final long PROD_POLL_SEC = 60L;

    // -------------------------------------------------------------------------
    // Runtime state
    // -------------------------------------------------------------------------

    private final RingBuffer<WeatherReading> buffer = new RingBuffer<>(180);
    private volatile boolean running = false;
    private volatile boolean noisy   = false;
    private volatile boolean windy   = false;

    private GpioFacade gpio;
    private I2cFacade  lcd;

    private final TrendAnalyzer       analyzer  = new TrendAnalyzer();
    private final HtmlReportGenerator reporter  = new HtmlReportGenerator();
    private final CsvDataLogger       csvLogger = new CsvDataLogger("logs/weather.csv");

    private ScheduledExecutorService scheduler;

    private MatrixScrollerComponent matrixScroller;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** Creates a MeteostationProject for production use. */
    public MeteostationProject() { }

    /**
     * Creates a MeteostationProject with injected facades — for unit testing.
     *
     * @param gpio GPIO facade
     * @param lcd  I2C LCD facade
     */
    public MeteostationProject(GpioFacade gpio, I2cFacade lcd) {
        this.gpio = gpio;
        this.lcd  = lcd;
    }

    // -------------------------------------------------------------------------
    // CrowPiProject contract
    // -------------------------------------------------------------------------

    @Override public String getName()        { return "Weather Station"; }
    @Override public String getProjectId()   { return "p10"; }
    @Override public String getDescription() {
        return "DHT11 weather monitor with 3-hour RingBuffer history, OLS trend forecast, and HTML report generation.";
    }

    @Override
    public void run(boolean mockMode) throws HardwareException {
        running = true;
        LOG.info("MeteostationProject starting (mockMode={})", mockMode);

        // Ініціалізуємо hardware-фасади
        boolean matrixMock = (gpio != null) || mockMode;
        if (gpio == null) {
            if (mockMode) {
                gpio = new MockGpioFacade();
                lcd  = new MockI2cFacade();
            } else {
                gpio = new Pi4jGpioFacade();
                lcd  = new Pi4jLcdFacade(I2CBus.BUS_1, 0x21);
            }
        }

        // ── LED matrix scroller ─────────────────────────────────────────────────
        matrixScroller = new MatrixScrollerComponent(matrixMock);
        matrixScroller.start();

        // Симульовані показники із синусоїдальною динамікою для реалістичної демонстрації
        MockSensorReader<WeatherReading> mockSensor = new MockSensorReader<>(
            new WeatherReading(22.1, 58.0, false, false, ts()),
            new WeatherReading(23.4, 60.0, false, false, ts()),
            new WeatherReading(25.0, 63.0, true,  false, ts()),
            new WeatherReading(26.8, 65.0, false, true,  ts()),
            new WeatherReading(24.5, 61.0, false, false, ts())
        );

        // Реєструємо GPIO-слухачів для сенсорів та кнопки
        gpio.addListener(SOUND_PIN,  (pin, high) -> { if (high) noisy = true; });
        gpio.addListener(TILT_PIN,   (pin, high) -> { if (high) windy = true; });
        gpio.addListener(BUTTON_PIN, (pin, high) -> {
            if (high) generateReport();
        });

        long pollSec = mockMode ? MOCK_POLL_SEC : PROD_POLL_SEC;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "meteostation-poll");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Зчитуємо показники сенсора
                WeatherReading reading = mockSensor.read();

                // Враховуємо тактильні події між опитуваннями
                WeatherReading enriched = new WeatherReading(
                    reading.getTempC(), reading.getHumidity(),
                    noisy, windy, ts()
                );
                noisy = false; windy = false; // скидаємо прапори після запису

                buffer.add(enriched);

                // Обчислюємо тренд по наявній історії температур
                List<Double> temps = buffer.toList().stream()
                    .map(WeatherReading::getTempC)
                    .collect(Collectors.toList());
                double slope = analyzer.calculateSlope(temps);
                WeatherForecast forecast = WeatherForecast.fromSlope(slope);

                double hi = HeatIndex.calculate(enriched.getTempC(), enriched.getHumidity());

                updateLcd(enriched, forecast, hi);
                updateRgb(hi);

                try {
                    csvLogger.log(enriched, hi, forecast);
                } catch (IOException e) {
                    LOG.warn("CSV log write failed: {}", e.getMessage());
                }

                LOG.debug("Reading: T={} H={} HI={} Trend={}", enriched.getTempC(),
                    enriched.getHumidity(), hi, forecast);

            } catch (Exception e) {
                LOG.error("Sensor poll error", e);
            }
        }, 0, pollSec, TimeUnit.SECONDS);

        // Головний цикл — чекаємо поки running=false
        while (running) {
            sleepMs(500);
        }
    }

    @Override
    public void shutdown() {
        if (!running) return;
        running = false;
        if (scheduler != null) scheduler.shutdownNow();
        if (gpio != null) gpio.close();
        LOG.info("MeteostationProject shutdown complete");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void generateReport() {
        List<WeatherReading> readings = buffer.toList();
        if (readings.isEmpty()) {
            LOG.warn("No readings yet — cannot generate report");
            return;
        }
        List<Double> temps = readings.stream().map(WeatherReading::getTempC)
            .collect(Collectors.toList());
        double slope = analyzer.calculateSlope(temps);
        WeatherForecast forecast = WeatherForecast.fromSlope(slope);
        WeatherReading last = readings.get(readings.size() - 1);
        double hi = HeatIndex.calculate(last.getTempC(), last.getHumidity());

        String html = reporter.generate(readings, forecast, hi);
        try {
            reporter.writeToFile(html, "weather_report.html");
            LOG.info("HTML report written to weather_report.html");
        } catch (IOException e) {
            LOG.error("Failed to write HTML report", e);
        }
    }

    private void updateLcd(WeatherReading r, WeatherForecast forecast, double hi) {
        String line1 = String.format("T:%.1f H:%.0f HI:%.0f",
            r.getTempC(), r.getHumidity(), hi);
        String line2 = String.format("TREND:%s %s",
            forecast.getArrow(), forecast.getLabel());
        lcdWriteLine(0, line1);
        lcdWriteLine(1, line2);
    }

    private void updateRgb(double heatIndex) {
        // RGB відповідно до зон Heat Index: синій=комфортно, зелений=тепло, жовтий=гаряче, червоний=небезпечно
        if (heatIndex < 27.0) {
            setRgb(false, false, true);   // синій — комфортно
        } else if (heatIndex < 32.0) {
            setRgb(false, true, false);   // зелений — тепло
        } else if (heatIndex < 40.0) {
            setRgb(true, true, false);    // жовтий — гаряче
        } else {
            setRgb(true, false, false);   // червоний — небезпечно
        }
    }

    private void setRgb(boolean r, boolean g, boolean b) {
        gpio.setOutput(RGB_R, r);
        gpio.setOutput(RGB_G, g);
        gpio.setOutput(RGB_B, b);
    }

    private void lcdWriteLine(int row, String text) {
        byte rowCmd = (row == 0) ? (byte) 0x80 : (byte) 0xC0;
        lcd.writeByte(LCD_ADDR, 0x00, rowCmd);
        String padded = String.format("%-16.16s", text != null ? text : "");
        for (char c : padded.toCharArray()) {
            lcd.writeByte(LCD_ADDR, 0x01, (byte) c);
        }
    }

    private static String ts() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }

    private static void sleepMs(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
