package ua.crowpi.projects.p09;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.exception.DatabaseException;
import ua.crowpi.core.exception.HardwareException;
import ua.crowpi.core.hardware.SensorReader;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Background thread that automatically turns the fan on when the temperature exceeds
 * the active profile's threshold.
 *
 * <p>Runs as a daemon thread that checks the DHT11 sensor every 60 seconds.
 * When temperature exceeds {@link Profile#getTempThreshold()}, the fan is activated
 * and a {@code TEMP_ALERT} event is logged to the database.</p>
 *
 * <p>The active profile can be changed at runtime (from the menu) via
 * {@link #setActiveProfile(Profile)} — the next check cycle picks up the new threshold.</p>
 *
 * <h3>Threading</h3>
 * <p>{@link #checkTemperature()} is package-private and called directly in unit tests
 * without starting the thread, making the auto-control logic fully testable:</p>
 * <pre>{@code
 *   autoTempControl.checkTemperature();
 *   verify(fanController).turnOn();
 * }</pre>
 */
public class AutoTempControl extends Thread {

    private static final Logger LOG = LogManager.getLogger(AutoTempControl.class);

    /** Interval between temperature checks. 60 000 ms = 60 seconds. */
    private static final long CHECK_INTERVAL_MS = 60_000L;

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final SensorReader<DhtReading> sensor;
    private final FanController fan;
    private final DeviceEventRepository eventRepo;
    private final DeviceState deviceState;

    // volatile — змінюється з головного потоку меню, читається з цього потоку
    private volatile Profile activeProfile;
    private volatile boolean running = false;

    /**
     * Creates an AutoTempControl thread (does not start it yet).
     *
     * @param sensor        DHT11 sensor reader (real or mock)
     * @param fan           fan relay controller
     * @param eventRepo     repository for logging TEMP_ALERT events
     * @param deviceState   shared device state, updated when fan state changes
     * @param activeProfile initial active profile with the temperature threshold
     */
    public AutoTempControl(SensorReader<DhtReading> sensor,
                           FanController fan,
                           DeviceEventRepository eventRepo,
                           DeviceState deviceState,
                           Profile activeProfile) {
        super("auto-temp-control");
        // Daemon-потік — не заважає JVM завершитись після виходу з main()
        setDaemon(true);
        this.sensor = sensor;
        this.fan = fan;
        this.eventRepo = eventRepo;
        this.deviceState = deviceState;
        this.activeProfile = activeProfile;
    }

    /**
     * Starts the background temperature monitoring loop.
     * Overridden to set {@code running = true} before the super call.
     */
    @Override
    public synchronized void start() {
        running = true;
        super.start();
    }

    /**
     * Signals the monitoring loop to stop after the current sleep interval.
     *
     * <p>Interrupts the thread to wake it from {@link Thread#sleep} immediately.</p>
     */
    public void stopMonitoring() {
        running = false;
        interrupt();
        LOG.info("AutoTempControl stop requested");
    }

    /**
     * Updates the active profile used for the temperature threshold comparison.
     *
     * <p>Thread-safe: writes to a {@code volatile} field.</p>
     *
     * @param profile the new active profile
     */
    public void setActiveProfile(Profile profile) {
        this.activeProfile = profile;
        LOG.debug("AutoTempControl profile updated to '{}' (threshold={}°C)",
                profile.getName(), profile.getTempThreshold());
    }

    /**
     * Main loop: checks temperature every {@value #CHECK_INTERVAL_MS} ms and turns
     * the fan on if the temperature exceeds the active profile's threshold.
     */
    @Override
    public void run() {
        LOG.info("AutoTempControl started (interval={}s)", CHECK_INTERVAL_MS / 1000);
        while (running) {
            checkTemperature();
            try {
                Thread.sleep(CHECK_INTERVAL_MS);
            } catch (InterruptedException e) {
                // Перервано через stopMonitoring() — виходимо з циклу
                Thread.currentThread().interrupt();
                break;
            }
        }
        LOG.info("AutoTempControl stopped");
    }

    /**
     * Performs a single temperature check cycle.
     *
     * <p>Package-private for direct use in unit tests (called without starting
     * the background thread).</p>
     */
    void checkTemperature() {
        DhtReading reading;
        try {
            reading = sensor.read();
        } catch (HardwareException e) {
            LOG.error("DHT11 read failed in AutoTempControl: {}", e.getMessage());
            return;
        }

        double temp = reading.getTemperatureC();
        double threshold = activeProfile.getTempThreshold();

        LOG.debug("AutoTempControl: temp={}°C threshold={}°C", temp, threshold);

        if (temp > threshold) {
            // Температура перевищила поріг — вмикаємо вентилятор і пишемо подію
            if (!fan.isOn()) {
                fan.turnOn();
                deviceState.setFanOn(true);
                logFanOnEvent();
                LOG.info("Fan auto-ON: temp={}°C > threshold={}°C", temp, threshold);
            }
        }
        // Автоматично НЕ вимикаємо вентилятор — це робиться вручну через меню.
        // Причина: щоб не виникало швидких вмикань/вимикань навколо порогу (bang-bang)
    }

    // -------------------------------------------------------------------------
    // Допоміжні методи
    // -------------------------------------------------------------------------

    /**
     * Inserts a TEMP_ALERT event into the database.
     * Errors are logged but do not propagate — a failed log must not stop the fan.
     */
    private void logFanOnEvent() {
        DeviceEvent event = DeviceEvent.of(
                "TEMP_ALERT",
                "FAN",
                "OFF",
                "ON",
                activeProfile.getId() > 0 ? activeProfile.getId() : null,
                LocalDateTime.now().format(TS_FMT)
        );
        try {
            eventRepo.insert(event);
        } catch (DatabaseException e) {
            // Помилка запису в БД не повинна зупинити управління вентилятором
            LOG.error("Failed to log TEMP_ALERT event: {}", e.getMessage());
        }
    }
}
