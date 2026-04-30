package ua.crowpi.projects.p06;

import ua.crowpi.core.hardware.GpioFacade;

import java.util.Random;

/**
 * Controls the RGB LED to deliver a visual stimulus to both players at a random time.
 *
 * <p>When {@link #generate()} is called, the generator waits a random delay
 * of 1000–4000 ms (to prevent anticipation), then drives all three RGB channels
 * HIGH, producing a bright white flash. The exact wall-clock millisecond of the
 * flash is returned so that {@link ReactionEngine} can compute per-player
 * reaction deltas.</p>
 *
 * <p>Pin assignments follow the CrowPi BCM numbering:</p>
 * <ul>
 *   <li>RGB Red   → BCM 11</li>
 *   <li>RGB Green → BCM 9</li>
 *   <li>RGB Blue  → BCM 10</li>
 * </ul>
 */
public class StimulusGenerator {

    /** BCM GPIO pin number for the red channel of the RGB LED. */
    public static final int RGB_RED_PIN   = 11;

    /** BCM GPIO pin number for the green channel of the RGB LED. */
    public static final int RGB_GREEN_PIN = 9;

    /** BCM GPIO pin number for the blue channel of the RGB LED. */
    public static final int RGB_BLUE_PIN  = 10;

    /** Minimum random pre-stimulus delay in milliseconds. */
    private static final int DELAY_MIN_MS = 1000;

    /** Maximum random pre-stimulus delay in milliseconds. */
    private static final int DELAY_MAX_MS = 4000;

    /** GPIO facade used to control the LED pins. */
    private final GpioFacade gpio;

    /** Random number generator for the pre-stimulus delay. */
    private final Random random;

    // -------------------------------------------------------------------------
    // Конструктор
    // -------------------------------------------------------------------------

    /**
     * Creates a {@code StimulusGenerator} backed by the given GPIO facade.
     *
     * @param gpio the GPIO abstraction; may be a mock or real Pi4J implementation
     */
    public StimulusGenerator(GpioFacade gpio) {
        this.gpio   = gpio;
        // Один екземпляр Random на генератор — достатньо для некритичного рандому
        this.random = new Random();
    }

    // -------------------------------------------------------------------------
    // Публічний API
    // -------------------------------------------------------------------------

    /**
     * Waits a random delay of 1000–4000 ms and then activates the RGB LED in white.
     *
     * <p>The calling thread is blocked during the delay. The timestamp captured
     * immediately before the GPIO write is returned as the canonical stimulus time
     * so both players are measured relative to the same moment.</p>
     *
     * @return wall-clock time in milliseconds ({@link System#currentTimeMillis()})
     *         at the moment the LED turned on
     */
    public long generate() {
        // Розраховуємо випадкову затримку щоб гравці не могли передбачити момент спалаху
        int delay = DELAY_MIN_MS + random.nextInt(DELAY_MAX_MS - DELAY_MIN_MS + 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            // Відновлюємо прапор переривання потоку — верхній рівень може його перевірити
            Thread.currentThread().interrupt();
        }

        // Фіксуємо час безпосередньо ПЕРЕД включенням LED — це і є момент стимулу
        long stimulusTime = System.currentTimeMillis();

        // Вмикаємо всі три канали RGB одночасно → білий колір
        gpio.setOutput(RGB_RED_PIN,   true);
        gpio.setOutput(RGB_GREEN_PIN, true);
        gpio.setOutput(RGB_BLUE_PIN,  true);

        return stimulusTime;
    }

    /**
     * Turns off the RGB LED by driving all three channels LOW.
     *
     * <p>Should be called after the response window closes to reset the LED
     * before the next round.</p>
     */
    public void stopFlash() {
        // Гасимо всі канали — повертаємо LED у вимкнений стан між раундами
        gpio.setOutput(RGB_RED_PIN,   false);
        gpio.setOutput(RGB_GREEN_PIN, false);
        gpio.setOutput(RGB_BLUE_PIN,  false);
    }
}
