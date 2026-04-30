package ua.crowpi.projects.p04;

import ua.crowpi.core.hardware.GpioFacade;

/**
 * Controls a passive buzzer connected to a PWM-capable GPIO pin to produce a two-tone siren.
 *
 * <p>The siren alternates between a high-frequency tone ({@value #FREQ_HIGH} Hz) and a
 * low-frequency tone ({@value #FREQ_LOW} Hz) every {@value #INTERVAL_MS} ms, creating
 * the characteristic wail of an alarm siren.</p>
 *
 * <p>The siren runs on a dedicated daemon thread so it does not block the main alarm
 * control loop. The thread uses a {@code volatile boolean} flag for graceful stop
 * without requiring thread interruption.</p>
 *
 * <p>On the CrowPi the passive buzzer is wired to BCM GPIO 18 (hardware PWM0).</p>
 */
public class SirenPlayer {

    /** BCM GPIO pin number of the passive buzzer (hardware PWM0 on RPi 3). */
    public static final int SIREN_PIN = 18;

    /** High-frequency tone in Hertz for the "wail up" phase. */
    public static final int FREQ_HIGH = 880;

    /** Low-frequency tone in Hertz for the "wail down" phase. */
    public static final int FREQ_LOW = 660;

    /** Duration in milliseconds for each tone phase before switching. */
    public static final int INTERVAL_MS = 300;

    // GPIO абстракція — дозволяє використовувати MockGpioFacade в тестах
    private final GpioFacade gpio;

    // volatile — забезпечує видимість між потоком сирени і потоком управління
    // без синхронізації (достатньо для простого boolean-прапорця зупинки)
    private volatile boolean playing;

    // Посилання на потік сирени — потрібне для перевірки стану та daemon-налаштування
    private Thread sirenThread;

    /**
     * Creates a SirenPlayer that will drive the passive buzzer via the given GPIO facade.
     *
     * <p>The siren is initially stopped.</p>
     *
     * @param gpio the GPIO facade used to generate PWM signals on {@value #SIREN_PIN}
     */
    public SirenPlayer(GpioFacade gpio) {
        this.gpio = gpio;
        this.playing = false;
    }

    /**
     * Starts the siren on a new daemon thread that alternates between two tones every
     * {@value #INTERVAL_MS} ms.
     *
     * <p>If the siren is already playing this method does nothing.
     * The thread is marked daemon so it will not prevent JVM shutdown.</p>
     */
    public void start() {
        // Захист від подвійного запуску — не створюємо другий потік якщо вже грає
        if (playing) {
            return;
        }

        playing = true;

        // Daemon-потік — JVM може завершити його без явного виклику stop(), якщо main-потік вийде
        sirenThread = new Thread(() -> {
            // Чергуємо дві частоти поки прапорець playing залишається true
            boolean highPhase = true;
            while (playing) {
                int freq = highPhase ? FREQ_HIGH : FREQ_LOW;
                // Увімкнення PWM зі скважністю 50% — максимальна гучність для пасивного buzzer
                gpio.pwm(SIREN_PIN, freq, 0.5f);
                highPhase = !highPhase;

                try {
                    Thread.sleep(INTERVAL_MS);
                } catch (InterruptedException e) {
                    // Переривання потоку — відновлюємо прапорець і виходимо
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Вимикаємо PWM після завершення циклу — повна тиша
            gpio.pwm(SIREN_PIN, 0, 0.0f);
        }, "siren-player");

        // Daemon — потік не утримує JVM при виході з main()
        sirenThread.setDaemon(true);
        sirenThread.start();
    }

    /**
     * Stops the siren by clearing the play flag.
     *
     * <p>The siren thread will complete its current sleep interval and then exit.
     * After {@link #stop()} returns the GPIO PWM may still be active for up to
     * {@value #INTERVAL_MS} ms. This is acceptable for an alarm system — the siren
     * does not need sample-accurate stopping.</p>
     */
    public void stop() {
        // Знімаємо прапорець — потік сирени помітить це на наступній ітерації циклу
        playing = false;
    }

    /**
     * Returns whether the siren is currently playing.
     *
     * @return {@code true} if the siren thread is running and generating tones
     */
    public boolean isPlaying() {
        return playing;
    }
}
