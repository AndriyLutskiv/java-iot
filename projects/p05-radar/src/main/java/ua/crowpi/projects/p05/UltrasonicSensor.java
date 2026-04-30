package ua.crowpi.projects.p05;

import ua.crowpi.core.exception.HardwareException;
import ua.crowpi.core.hardware.GpioFacade;

/**
 * Driver for the HC-SR04 ultrasonic distance sensor connected to Raspberry Pi GPIO.
 *
 * <p>The HC-SR04 measures distance by emitting a 40 kHz ultrasonic burst when its
 * TRIGGER pin is pulsed HIGH for at least 10 microseconds. It then drives the ECHO
 * pin HIGH for a duration proportional to the round-trip time of the sound pulse.
 * The distance is calculated from that echo duration using the speed of sound
 * (343 m/s at ~20 °C).</p>
 *
 * <p>Pin assignments (BCM numbering):</p>
 * <ul>
 *   <li>TRIGGER — BCM 23 (output)</li>
 *   <li>ECHO    — BCM 24 (input, 3.3 V tolerant via voltage divider on CrowPi)</li>
 * </ul>
 *
 * <p>Distance formula:<br>
 * {@code distanceCm = (echoMicros * 343.0) / (2.0 * 1_000_000.0) * 100.0}<br>
 * Result is clamped to [2.0, 400.0] cm — the sensor's rated operating range.</p>
 *
 * <p>Timeout handling: if the ECHO pin does not go HIGH within 30 ms after the trigger
 * pulse, a {@link HardwareException} is thrown to prevent the calling thread from
 * blocking indefinitely.</p>
 */
public class UltrasonicSensor {

    /** BCM GPIO pin number wired to HC-SR04 TRIGGER. */
    public static final int TRIGGER_PIN = 23;

    /** BCM GPIO pin number wired to HC-SR04 ECHO. */
    public static final int ECHO_PIN = 24;

    /**
     * Maximum wait time for ECHO HIGH or LOW transitions, in nanoseconds (30 ms).
     * Prevents the measurement loop from hanging on sensor failure.
     */
    private static final long TIMEOUT_NS = 30_000_000L;

    /** GPIO facade used to drive TRIGGER and sample ECHO. */
    private final GpioFacade gpio;

    /**
     * Creates a new UltrasonicSensor backed by the given GPIO facade.
     *
     * @param gpio the GPIO facade to use for TRIGGER output and ECHO input;
     *             use {@link ua.crowpi.core.mock.MockGpioFacade} for desktop testing
     */
    public UltrasonicSensor(GpioFacade gpio) {
        this.gpio = gpio;
    }

    /**
     * Triggers a distance measurement and returns the result in centimetres.
     *
     * <p>Measurement sequence:</p>
     * <ol>
     *   <li>Drive TRIGGER HIGH for 10 microseconds.</li>
     *   <li>Drive TRIGGER LOW.</li>
     *   <li>Wait for ECHO pin to go HIGH (start of echo pulse).</li>
     *   <li>Wait for ECHO pin to go LOW (end of echo pulse).</li>
     *   <li>Compute duration and convert to centimetres.</li>
     * </ol>
     *
     * @return distance in centimetres, clamped to [2.0, 400.0]
     * @throws HardwareException if the ECHO pin does not respond within the 30 ms timeout
     */
    public double measure() throws HardwareException {
        // Надсилаємо тригер-імпульс 10 мкс для ініціалізації вимірювання датчиком HC-SR04
        gpio.setOutput(TRIGGER_PIN, true);
        sleepMicros(10);
        gpio.setOutput(TRIGGER_PIN, false);

        // Чекаємо на початок відлуння — ECHO переходить у HIGH
        long start = System.nanoTime();
        while (!gpio.readInput(ECHO_PIN)) {
            if (System.nanoTime() - start > TIMEOUT_NS) {
                // Якщо ECHO не з'явився за 30 мс — датчик не відповів або розімкнений
                throw new HardwareException(
                        "HC-SR04 echo timeout: ECHO pin did not go HIGH within 30 ms");
            }
        }

        // Фіксуємо час початку луна-імпульсу
        long echoStart = System.nanoTime();

        // Чекаємо завершення луна-імпульсу — ECHO переходить у LOW
        while (gpio.readInput(ECHO_PIN)) {
            if (System.nanoTime() - echoStart > TIMEOUT_NS) {
                // Якщо ECHO залишається HIGH понад 30 мс — об'єкт поза зоною або збій
                break;
            }
        }

        long echoEnd = System.nanoTime();

        // Тривалість луна-імпульсу в мікросекундах — основа для розрахунку відстані
        long echoMicros = (echoEnd - echoStart) / 1_000L;

        // Формула: відстань = (час_луна_мкс × швидкість_звуку) / (2 × 10^6) × 100 см
        // Ділення на 2 — звук проходить відстань двічі (туди і назад)
        double distanceCm = (echoMicros * 343.0) / (2.0 * 1_000_000.0) * 100.0;

        // Обмежуємо значення в межах робочого діапазону датчика [2 см, 400 см]
        return Math.max(2.0, Math.min(400.0, distanceCm));
    }

    // -------------------------------------------------------------------------
    // Внутрішній метод очікування
    // -------------------------------------------------------------------------

    /**
     * Busy-waits for approximately the specified number of microseconds.
     *
     * <p>A busy-wait is intentional here: {@link Thread#sleep} has millisecond
     * granularity and would overshoot the required 10 µs trigger pulse width
     * significantly. The JVM timestamp resolution via {@link System#nanoTime()}
     * is sufficient for this purpose on modern JVMs.</p>
     *
     * @param micros number of microseconds to wait
     */
    private static void sleepMicros(long micros) {
        // Активне очікування замість Thread.sleep — точність < 1 мс критична для HC-SR04
        long deadline = System.nanoTime() + micros * 1_000L;
        while (System.nanoTime() < deadline) {
            // Порожній цикл — навмисний busy-wait для субмілісекундної точності
        }
    }
}
