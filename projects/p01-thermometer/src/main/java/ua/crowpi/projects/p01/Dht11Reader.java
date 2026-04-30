package ua.crowpi.projects.p01;

import ua.crowpi.core.exception.HardwareException;
import ua.crowpi.core.hardware.GpioFacade;
import ua.crowpi.core.hardware.SensorReader;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Reads temperature and humidity from a DHT11 sensor connected to GPIO pin 4
 * using a simplified bit-bang protocol over the {@link GpioFacade}.
 *
 * <p>The DHT11 single-wire protocol summary:</p>
 * <ol>
 *   <li>Host drives the DATA pin LOW for ≥ 18 ms (start signal)</li>
 *   <li>Host releases the pin HIGH; DHT11 responds with 80 µs LOW + 80 µs HIGH</li>
 *   <li>DHT11 transmits 40 bits MSB-first: 8b humidity int · 8b humidity dec ·
 *       8b temp int · 8b temp dec · 8b checksum</li>
 *   <li>Each bit: 50 µs LOW, then ~26 µs HIGH = logic 0 or ~70 µs HIGH = logic 1</li>
 * </ol>
 *
 * <p><strong>Note on timing:</strong> Java {@link Thread#sleep} has ~1 ms granularity
 * on Linux ARM, so the 18 ms start pulse is reliable. The microsecond-level bit timing
 * uses {@link System#nanoTime()} busy-waiting. On a lightly-loaded Raspberry Pi 3 this
 * is generally sufficient; the checksum byte catches any bit errors.</p>
 *
 * <p>In {@code --mock} mode this class is never instantiated; {@link ThermometerProject}
 * substitutes a {@link ua.crowpi.core.mock.MockSensorReader} instead.</p>
 */
public class Dht11Reader implements SensorReader<TemperatureReading> {

    /** BCM GPIO pin number connected to the DHT11 DATA line on the CrowPi board. */
    public static final int DHT11_PIN = 4;

    /** Start-signal LOW pulse duration in milliseconds (DHT11 spec requires ≥ 18 ms). */
    private static final long START_LOW_MS = 20;

    /** Total number of data bits the DHT11 transmits per reading (5 bytes × 8 bits). */
    private static final int TOTAL_BITS = 40;

    /** Maximum busy-wait iterations before declaring a sensor timeout. */
    private static final int TIMEOUT_LOOPS = 100_000;

    /** ISO-8601 timestamp formatter used to record when each reading occurred. */
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final GpioFacade gpio;

    /**
     * Creates a {@code Dht11Reader} bound to the given GPIO facade.
     *
     * @param gpio GPIO facade used to drive and read the DHT11 data pin
     */
    public Dht11Reader(GpioFacade gpio) {
        this.gpio = gpio;
    }

    /**
     * Triggers one DHT11 measurement and returns the decoded reading.
     *
     * <p>The method blocks for approximately 20 ms (start signal) plus ~5 ms
     * for the 40-bit data stream, for a total of ~25 ms per call.</p>
     *
     * <p>Because real DHT11 bit-bang timing requires native microsecond precision
     * that the JVM cannot guarantee on all platforms, this implementation uses
     * {@link System#nanoTime()}-based busy-waiting for the HIGH-pulse duration
     * measurement. On desktop/CI without real hardware the mock path is used
     * instead, so this code path only executes on a real Raspberry Pi.</p>
     *
     * @return a {@link TemperatureReading} with current temperature, humidity, and timestamp
     * @throws HardwareException if the sensor does not respond or the checksum fails
     */
    @Override
    public TemperatureReading read() throws HardwareException {
        // Крок 1: відправляємо стартовий сигнал — утримуємо лінію LOW на 20 мс
        gpio.setOutput(DHT11_PIN, false);
        sleepMs(START_LOW_MS);
        // Короткий HIGH щоб DHT11 побачив наростаючий фронт (~40 µs)
        gpio.setOutput(DHT11_PIN, true);
        // Перемикаємо пін у режим входу з підтяжкою вгору — лінію тепер може тягти вниз датчик
        gpio.setInput(DHT11_PIN);

        // Крок 2: чекаємо підтвердження від датчика — 80 µs LOW потім 80 µs HIGH
        if (!waitForLevel(false, TIMEOUT_LOOPS)) {
            throw new HardwareException("DHT11: немає LOW-відповіді після стартового сигналу");
        }
        if (!waitForLevel(true, TIMEOUT_LOOPS)) {
            throw new HardwareException("DHT11: немає HIGH-відповіді після стартового LOW");
        }
        if (!waitForLevel(false, TIMEOUT_LOOPS)) {
            throw new HardwareException("DHT11: немає LOW перед початком даних");
        }

        // Крок 3: зчитуємо 40 бітів — кожен байт у data[0..4]
        byte[] data = new byte[5];
        for (int i = 0; i < TOTAL_BITS; i++) {
            // Кожен біт починається зі ~50 µs LOW — чекаємо переходу в HIGH
            if (!waitForLevel(true, TIMEOUT_LOOPS)) {
                throw new HardwareException("DHT11: таймаут очікування HIGH для біту " + i);
            }
            // Вимірюємо тривалість HIGH-фази для визначення 0 або 1
            long t0 = System.nanoTime();
            if (!waitForLevel(false, TIMEOUT_LOOPS)) {
                throw new HardwareException("DHT11: таймаут очікування LOW після HIGH біту " + i);
            }
            long highNanos = System.nanoTime() - t0;

            // HIGH > 40 µs → біт 1 (DHT11: ~70 µs); HIGH <= 40 µs → біт 0 (~26 µs)
            if (highNanos > 40_000L) {
                // Встановлюємо відповідний біт у байті data[i/8]
                data[i / 8] |= (byte) (1 << (7 - (i % 8)));
            }
        }

        // Крок 4: перевіряємо контрольну суму — остання байт = сума перших чотирьох
        int expectedChecksum = (data[0] + data[1] + data[2] + data[3]) & 0xFF;
        int actualChecksum   = data[4] & 0xFF;
        if (actualChecksum != expectedChecksum) {
            throw new HardwareException(String.format(
                    "DHT11: помилка контрольної суми (очікувалось 0x%02X, отримано 0x%02X)",
                    expectedChecksum, actualChecksum));
        }

        // Крок 5: декодуємо дані — DHT11 не передає дійсну частину (завжди 0 для дешевої версії)
        double humidity    = (data[0] & 0xFF) + (data[1] & 0xFF) / 10.0;
        double temperature = (data[2] & 0xFF) + (data[3] & 0xFF) / 10.0;
        String timestamp   = LocalDateTime.now().format(TIMESTAMP_FMT);

        return new TemperatureReading(temperature, humidity, timestamp);
    }

    // -------------------------------------------------------------------------
    // Допоміжні методи
    // -------------------------------------------------------------------------

    /**
     * Busy-waits until the DHT11 data pin reaches the desired logic level.
     *
     * <p>Uses a simple loop counter rather than a timeout based on wall time,
     * since {@link System#nanoTime()} calls inside the loop would themselves
     * add latency and potentially shift the timing window.</p>
     *
     * @param level    desired logic level: {@code true} = HIGH, {@code false} = LOW
     * @param maxLoops maximum iterations before giving up
     * @return {@code true} if the desired level was reached; {@code false} on timeout
     */
    private boolean waitForLevel(boolean level, int maxLoops) {
        for (int i = 0; i < maxLoops; i++) {
            // Перевіряємо рівень сигналу на піні щоразу в циклі
            if (gpio.readInput(DHT11_PIN) == level) {
                return true;
            }
        }
        // Вичерпали ліміт ітерацій — датчик не відповів
        return false;
    }

    /**
     * Sleeps for the given number of milliseconds, absorbing any {@link InterruptedException}
     * and restoring the thread interrupt flag.
     *
     * @param ms sleep duration in milliseconds
     */
    private static void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // Відновлюємо прапор переривання — не "ковтаємо" переривання безслідно
            Thread.currentThread().interrupt();
        }
    }
}
