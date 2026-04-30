package ua.crowpi.projects.p03;

import ua.crowpi.core.hardware.GpioFacade;

/**
 * Plays a {@link Melody} on the CrowPi hardware in a dedicated daemon thread.
 *
 * <p>Each call to {@link #play(Melody)} cancels any in-progress playback and starts
 * a new daemon thread that:</p>
 * <ol>
 *   <li>Iterates over every {@link Note} in the melody.</li>
 *   <li>For pitched notes: enables hardware PWM on {@link #BUZZER_PIN} at the note's
 *       frequency with 50 % duty cycle, and turns the RGB LED white.</li>
 *   <li>For rests: disables the buzzer and turns the RGB LED off.</li>
 *   <li>Sleeps for the note's duration, then advances to the next note.</li>
 *   <li>After all notes: silences the buzzer and turns off the LED.</li>
 * </ol>
 *
 * <p>Playback can be cancelled at any time by calling {@link #stop()}, which sets
 * the {@code volatile boolean stopped} flag. The playback thread checks this flag
 * before every note so cancellation is prompt (at most one note duration late).</p>
 *
 * <p>GPIO pin assignments follow the CrowPi wiring spec:</p>
 * <ul>
 *   <li>Buzzer PWM — BCM 18 (hardware PWM0)</li>
 *   <li>RGB Red    — BCM 22</li>
 *   <li>RGB Green  — BCM 27</li>
 *   <li>RGB Blue   — BCM 17</li>
 * </ul>
 */
public final class MelodyPlayer {

    // -------------------------------------------------------------------------
    // GPIO pin constants
    // -------------------------------------------------------------------------

    /** BCM GPIO pin number of the passive buzzer (hardware PWM0). */
    public static final int BUZZER_PIN = 18;

    /** BCM GPIO pin number of the RGB LED red channel. */
    public static final int RGB_R = 22;

    /** BCM GPIO pin number of the RGB LED green channel. */
    public static final int RGB_G = 27;

    /** BCM GPIO pin number of the RGB LED blue channel. */
    public static final int RGB_B = 17;

    // -------------------------------------------------------------------------
    // State fields — volatile для безпечного доступу між потоками
    // -------------------------------------------------------------------------

    /**
     * Stop flag — set to {@code true} by {@link #stop()} to signal the playback
     * thread to exit its note loop after the current note finishes.
     *
     * <p>Must be {@code volatile} because it is written by the calling thread
     * (e.g. the main loop) and read by the playback daemon thread.</p>
     */
    private volatile boolean stopped = false;

    /**
     * Tracks how many milliseconds of the current melody have already been played.
     *
     * <p>Updated by the playback thread after each note completes.
     * Read by the main loop to drive the LCD progress bar.
     * {@code volatile} ensures the main thread always sees the latest value.</p>
     */
    private volatile int elapsedMs = 0;

    /** Reference to the currently running playback thread (may be {@code null}). */
    private Thread playbackThread = null;

    /** Hardware facade used to drive the buzzer and RGB LED. */
    private final GpioFacade gpio;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a MelodyPlayer backed by the given GPIO facade.
     *
     * @param gpio GPIO facade (real or mock) used for PWM and LED control
     */
    public MelodyPlayer(GpioFacade gpio) {
        this.gpio = gpio;
    }

    // -------------------------------------------------------------------------
    // Playback control
    // -------------------------------------------------------------------------

    /**
     * Starts playing the given melody in a new daemon thread.
     *
     * <p>If a melody is already playing it is stopped first (the previous playback
     * thread's {@code stopped} flag is set and the new thread starts independently
     * — there is no blocking join so the main loop stays responsive).</p>
     *
     * @param melody the melody to play; must not be {@code null}
     */
    public void play(Melody melody) {
        // Зупиняємо попередню мелодію перед початком нової
        stop();

        // Скидаємо стан для нового відтворення
        stopped  = false;
        elapsedMs = 0;

        // Зберігаємо посилання щоб isPlaying() міг перевірити стан потоку
        playbackThread = new Thread(() -> runPlayback(melody), "melody-player");
        // Daemon-потік не заважає JVM завершитися після виходу з main()
        playbackThread.setDaemon(true);
        playbackThread.start();
    }

    /**
     * Stops playback as soon as the current note finishes.
     *
     * <p>Setting {@code stopped = true} is the only write; the playback thread
     * checks this flag at the start of each note iteration. The method returns
     * immediately without waiting for the thread to actually finish.</p>
     */
    public void stop() {
        // Сигналізуємо потоку зупинитися — наступна нота вже не буде відтворена
        stopped = true;
    }

    /**
     * Returns {@code true} while a melody is actively being played.
     *
     * <p>The check is: the playback thread exists, is still alive, and the
     * {@code stopped} flag has not been raised.</p>
     *
     * @return {@code true} if playback is ongoing
     */
    public boolean isPlaying() {
        // Перевіряємо і живість потоку, і прапорець зупинки
        return playbackThread != null && playbackThread.isAlive() && !stopped;
    }

    /**
     * Returns the number of milliseconds of the current melody that have been played.
     *
     * <p>The value is monotonically increasing during playback and resets to 0
     * when a new melody starts via {@link #play(Melody)}.</p>
     *
     * @return elapsed playback time in milliseconds
     */
    public int getElapsedMs() {
        return elapsedMs;
    }

    // -------------------------------------------------------------------------
    // Private playback logic
    // -------------------------------------------------------------------------

    /**
     * Runs the note-by-note playback loop on the dedicated playback thread.
     *
     * <p>For each note the method:</p>
     * <ol>
     *   <li>Checks the {@code stopped} flag and exits immediately if set.</li>
     *   <li>Configures the buzzer and RGB LED according to note type.</li>
     *   <li>Sleeps for the note's duration.</li>
     *   <li>Accumulates the duration into {@code elapsedMs}.</li>
     * </ol>
     * After all notes (or after early stop) the buzzer and LED are silenced.
     *
     * @param melody the melody to iterate over
     */
    private void runPlayback(Melody melody) {
        try {
            for (Note note : melody.getNotes()) {
                // Перевіряємо прапорець на початку кожної ноти
                if (stopped) {
                    break;
                }

                if (note.isRest()) {
                    // Пауза: вимикаємо buzzer і RGB LED на час паузи
                    gpio.pwm(BUZZER_PIN, 0, 0.0f);
                    setRgbLed(false);
                } else {
                    // Нота: вмикаємо PWM на відповідній частоті з 50% duty cycle
                    gpio.pwm(BUZZER_PIN, note.getFrequencyHz(), 0.5f);
                    // Білий колір: всі три канали RGB увімкнені
                    setRgbLed(true);
                }

                // Чекаємо тривалість ноти
                Thread.sleep(note.getDurationMs());

                // Накопичуємо пройдений час для прогрес-бару
                elapsedMs += note.getDurationMs();
            }
        } catch (InterruptedException e) {
            // Відновлюємо прапорець переривання — не гасимо його мовчки
            Thread.currentThread().interrupt();
        } finally {
            // Завжди вимикаємо buzzer і LED після завершення або зупинки
            gpio.pwm(BUZZER_PIN, 0, 0.0f);
            setRgbLed(false);
        }
    }

    /**
     * Turns the RGB LED fully on (white) or fully off.
     *
     * <p>White is produced by setting all three colour channels HIGH simultaneously.
     * The LED pulses in sync with note timing — on during a note, off during a rest.</p>
     *
     * @param on {@code true} to turn all RGB channels on (white); {@code false} to turn off
     */
    private void setRgbLed(boolean on) {
        gpio.setOutput(RGB_R, on);
        gpio.setOutput(RGB_G, on);
        gpio.setOutput(RGB_B, on);
    }
}
