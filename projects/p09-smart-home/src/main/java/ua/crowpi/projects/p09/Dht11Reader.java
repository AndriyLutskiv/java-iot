package ua.crowpi.projects.p09;

import ua.crowpi.core.exception.HardwareException;
import ua.crowpi.core.hardware.GpioFacade;
import ua.crowpi.core.hardware.SensorReader;

/**
 * Reads temperature and humidity from a DHT11 sensor connected to a single GPIO pin.
 *
 * <p>The DHT11 uses a proprietary single-wire protocol:</p>
 * <ol>
 *   <li>Host drives pin LOW for ≥ 18 ms (start signal)</li>
 *   <li>Host releases pin HIGH; DHT11 responds with 80 µs LOW + 80 µs HIGH</li>
 *   <li>DHT11 transmits 40 bits (5 bytes) MSB-first:
 *       8b humidity integer · 8b humidity decimal · 8b temp integer · 8b temp decimal · 8b checksum</li>
 *   <li>Each bit: 50 µs LOW then either ~26 µs HIGH (0) or ~70 µs HIGH (1)</li>
 * </ol>
 *
 * <p><strong>Timing note:</strong> Java's {@link Thread#sleep} precision on Linux ARM is
 * typically ~1 ms. The start-signal LOW (18 ms) is safe. The bit-timing (26–70 µs) uses
 * {@link System#nanoTime()}-based busy-waiting. This is acceptable on a
 * lightly-loaded Raspberry Pi 3 but may occasionally fail under load — the checksum
 * byte catches any corruption.</p>
 *
 * <p>In {@code --mock} mode this class is never instantiated; {@link SmartHomeProject}
 * uses {@link ua.crowpi.core.mock.MockSensorReader} instead.</p>
 */
public class Dht11Reader implements SensorReader<DhtReading> {

    /** BCM GPIO pin connected to the DHT11 DATA line on the CrowPi. */
    public static final int DHT11_PIN = 4;

    /** Duration of the start-signal LOW pulse in milliseconds (DHT11 requires ≥ 18 ms). */
    private static final long START_LOW_MS = 20;

    /** Maximum number of bits to read per transaction (40 bits = 5 bytes). */
    private static final int TOTAL_BITS = 40;

    /** Busy-wait loop counter limit to avoid infinite loops on sensor failure. */
    private static final int TIMEOUT_LOOPS = 100_000;

    private final GpioFacade gpio;

    /**
     * Creates a Dht11Reader for the default CrowPi DHT11 pin ({@value #DHT11_PIN}).
     *
     * @param gpio GPIO facade for pin control
     */
    public Dht11Reader(GpioFacade gpio) {
        this.gpio = gpio;
    }

    /**
     * Triggers a measurement and reads 40 bits from the DHT11 sensor.
     *
     * <p>Blocks for approximately 20 ms (start signal) plus ~5 ms for data
     * transmission = ~25 ms total.</p>
     *
     * @return parsed {@link DhtReading} with temperature and humidity
     * @throws HardwareException if the sensor does not respond or the checksum fails
     */
    @Override
    public DhtReading read() throws HardwareException {
        // Крок 1: відправляємо стартовий сигнал — тягнемо лінію LOW на 20 мс
        gpio.setOutput(DHT11_PIN, false);
        sleepMs(START_LOW_MS);
        gpio.setOutput(DHT11_PIN, true); // короткий HIGH щоб DHT11 побачив наростаючий фронт
        gpio.setInput(DHT11_PIN);        // перемикаємо у вхід — датчик тепер може тягти лінію LOW

        // Крок 2: очікуємо відповідь датчика (80 µs LOW → 80 µs HIGH)
        if (!waitForLevel(false, TIMEOUT_LOOPS)) {
            throw new HardwareException("DHT11: no LOW response after start signal");
        }
        if (!waitForLevel(true, TIMEOUT_LOOPS)) {
            throw new HardwareException("DHT11: no HIGH response after start LOW");
        }
        if (!waitForLevel(false, TIMEOUT_LOOPS)) {
            throw new HardwareException("DHT11: no data start LOW after response HIGH");
        }

        // Крок 3: зчитуємо 40 бітів
        byte[] data = new byte[5];
        for (int i = 0; i < TOTAL_BITS; i++) {
            // Кожен біт починається з 50 µs LOW — чекаємо кінця цього LOW
            if (!waitForLevel(true, TIMEOUT_LOOPS)) {
                throw new HardwareException("DHT11: timeout reading bit " + i);
            }
            // Вимірюємо тривалість HIGH:  ~26 µs = 0,  ~70 µs = 1
            long t0 = System.nanoTime();
            if (!waitForLevel(false, TIMEOUT_LOOPS)) {
                throw new HardwareException("DHT11: timeout after bit HIGH " + i);
            }
            long highNanos = System.nanoTime() - t0;

            // HIGH > 40 µs → біт 1; інакше → біт 0
            if (highNanos > 40_000L) {
                data[i / 8] |= (byte) (1 << (7 - (i % 8)));
            }
        }

        // Крок 4: перевірка контрольної суми
        int checksum = (data[0] + data[1] + data[2] + data[3]) & 0xFF;
        if ((data[4] & 0xFF) != checksum) {
            throw new HardwareException(String.format(
                    "DHT11: checksum mismatch (expected 0x%02X, got 0x%02X)",
                    checksum, data[4] & 0xFF));
        }

        // Крок 5: декодуємо значення (DHT11 не передає десяткову частину — завжди 0)
        double humidity    = (data[0] & 0xFF) + (data[1] & 0xFF) / 10.0;
        double temperature = (data[2] & 0xFF) + (data[3] & 0xFF) / 10.0;
        return new DhtReading(temperature, humidity);
    }

    // -------------------------------------------------------------------------
    // Допоміжні методи
    // -------------------------------------------------------------------------

    /**
     * Busy-waits until the GPIO pin reaches the desired level.
     *
     * @param level    target level: {@code true}=HIGH, {@code false}=LOW
     * @param maxLoops maximum iteration count before giving up
     * @return {@code true} if the level was reached; {@code false} on timeout
     */
    private boolean waitForLevel(boolean level, int maxLoops) {
        for (int i = 0; i < maxLoops; i++) {
            if (gpio.readInput(DHT11_PIN) == level) {
                return true;
            }
        }
        return false;
    }

    /** Sleeps for the specified milliseconds, ignoring interruptions. */
    private static void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
