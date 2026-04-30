package ua.crowpi.projects.p11;

import ua.crowpi.core.hardware.GpioFacade;

/**
 * Sound effects for the LCD Platform Game, each played via hardware PWM on the buzzer.
 *
 * <p>Each constant encodes a frequency (Hz) and duration (ms). Calling
 * {@link #play(GpioFacade)} drives the buzzer at that frequency for that duration,
 * then silences it.</p>
 */
public enum SoundEffects {

    /** Short upward chirp played when the player jumps. */
    JUMP(900, 80),

    /** Two-tone chime played when a coin is collected. */
    COIN_COLLECT(1200, 100),

    /** Low buzz played when the player takes a hit. */
    HIT(400, 300),

    /** Ascending arpeggio played when the player wins. */
    WIN(784, 500),

    /** Descending tone played on game over. */
    GAME_OVER(262, 800);

    // -------------------------------------------------------------------------

    private final int frequencyHz;
    private final int durationMs;

    /** BCM GPIO pin of the passive buzzer (PWM capable). */
    public static final int BUZZER_PIN = 18;

    SoundEffects(int frequencyHz, int durationMs) {
        this.frequencyHz = frequencyHz;
        this.durationMs  = durationMs;
    }

    /**
     * Plays this sound effect through the buzzer connected to {@link #BUZZER_PIN}.
     *
     * <p>Blocks the calling thread for {@link #durationMs} milliseconds.
     * Call from a dedicated sound thread if non-blocking behaviour is needed.</p>
     *
     * @param gpio the GPIO facade used to drive the PWM buzzer
     */
    public void play(GpioFacade gpio) {
        // Вмикаємо PWM зі скважністю 50% — стандартне значення для пасивного п'єзозумера
        gpio.pwm(BUZZER_PIN, frequencyHz, 0.5f);
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Вимикаємо сигнал після закінчення — duty cycle 0 = тиша
        gpio.pwm(BUZZER_PIN, frequencyHz, 0.0f);
    }

    /** Returns the buzzer frequency in Hz. @return frequency */
    public int getFrequencyHz() { return frequencyHz; }

    /** Returns the play duration in milliseconds. @return duration */
    public int getDurationMs() { return durationMs; }
}
