package ua.crowpi.projects.p06;

import ua.crowpi.core.hardware.GpioFacade;

/**
 * Plays a short ascending victory melody on the CrowPi passive buzzer when a
 * winner is announced at the end of a game session.
 *
 * <p>The jingle consists of three notes from the C major triad played in
 * ascending order to signal success:</p>
 * <ol>
 *   <li>C5 — 523 Hz, 200 ms</li>
 *   <li>E5 — 659 Hz, 200 ms</li>
 *   <li>G5 — 784 Hz, 200 ms</li>
 * </ol>
 *
 * <p>Tone generation uses the hardware PWM capability of the GPIO facade.
 * The buzzer is silenced after each note and fully after the sequence ends.</p>
 *
 * <p>The buzzer is wired to BCM GPIO 18 (hardware PWM0 on the Raspberry Pi 3).</p>
 */
public class VictoryJingle {

    /** BCM GPIO pin number of the passive piezo buzzer on the CrowPi board. */
    public static final int BUZZER_PIN = 18;

    /** Duration of each individual note in the jingle, in milliseconds. */
    private static final int NOTE_DURATION_MS = 200;

    /** PWM duty cycle for tone generation (50% — square wave for buzzers). */
    private static final float DUTY_CYCLE = 0.5f;

    /** Frequency of the C5 note in Hertz. */
    private static final int FREQ_C5 = 523;

    /** Frequency of the E5 note in Hertz. */
    private static final int FREQ_E5 = 659;

    /** Frequency of the G5 note in Hertz. */
    private static final int FREQ_G5 = 784;

    /** GPIO facade used to drive the buzzer pin with PWM. */
    private final GpioFacade gpio;

    // -------------------------------------------------------------------------
    // Конструктор
    // -------------------------------------------------------------------------

    /**
     * Creates a {@code VictoryJingle} backed by the given GPIO facade.
     *
     * @param gpio the GPIO abstraction; may be a mock or real Pi4J implementation
     */
    public VictoryJingle(GpioFacade gpio) {
        this.gpio = gpio;
    }

    // -------------------------------------------------------------------------
    // Публічний API
    // -------------------------------------------------------------------------

    /**
     * Plays the three-note ascending victory melody on the buzzer.
     *
     * <p>Execution is synchronous — the method blocks the calling thread for
     * approximately 600 ms (3 × 200 ms) plus any OS scheduling jitter.
     * It is not run in a daemon thread so the melody always completes before
     * the application proceeds.</p>
     *
     * @param winnerName the display name of the winning player; included in the
     *                   console log but not used for audio (purely informational)
     */
    public void play(String winnerName) {
        System.out.printf("[JINGLE] Playing victory melody for: %s%n", winnerName);

        // Масив нот у порядку висхідного тризвуку До мажор
        int[] notes = { FREQ_C5, FREQ_E5, FREQ_G5 };

        for (int freq : notes) {
            // Вмикаємо ноту на половинному робочому циклі — пасивний buzzer генерує тон
            gpio.pwm(BUZZER_PIN, freq, DUTY_CYCLE);
            try {
                // Тримаємо ноту задану кількість мілісекунд
                Thread.sleep(NOTE_DURATION_MS);
            } catch (InterruptedException e) {
                // Відновлюємо прапор переривання і виходимо з циклу достроково
                Thread.currentThread().interrupt();
                break;
            }
            // Заглушуємо buzzer між нотами щоб уникнути злиття звуків
            gpio.pwm(BUZZER_PIN, 0, 0.0f);
        }

        // Гарантовано вимикаємо buzzer після завершення мелодії
        gpio.pwm(BUZZER_PIN, 0, 0.0f);
        System.out.println("[JINGLE] Done.");
    }
}
