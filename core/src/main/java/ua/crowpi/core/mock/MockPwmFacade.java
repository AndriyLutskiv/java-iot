package ua.crowpi.core.mock;

import ua.crowpi.core.hardware.PwmFacade;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of {@link PwmFacade} for running on a laptop (no RPi required).
 *
 * <p>All PWM operations are logged to stdout with enough detail so a student can
 * verify that the correct frequency and duty cycle are being calculated by their
 * project code. Active channels are tracked in an in-memory map so
 * {@link #stopPwm(int)} can confirm a channel was actually running.</p>
 */
public class MockPwmFacade implements PwmFacade {

    // Зберігаємо активні PWM-канали: pin → "freq@duty" — для реалістичних повідомлень
    private final Map<Integer, String> activeChannels = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     *
     * <p>In mock mode: logs frequency and duty cycle, stores state for reference.</p>
     */
    @Override
    public void setPwm(int pin, int frequencyHz, float dutyCycle) {
        // Перетворюємо duty cycle у відсотки для зручного читання у консолі
        String description = String.format("%d Hz @ %.1f%%", frequencyHz, dutyCycle * 100);
        activeChannels.put(pin, description);
        System.out.printf("[MOCK PWM] pin=%d  START  %s%n", pin, description);
    }

    /**
     * {@inheritDoc}
     *
     * <p>In mock mode: removes the channel from the active map and logs the stop.</p>
     */
    @Override
    public void stopPwm(int pin) {
        String prev = activeChannels.remove(pin);
        if (prev != null) {
            // Канал справді був активний — повідомляємо що він зупинений
            System.out.printf("[MOCK PWM] pin=%d  STOP   (was %s)%n", pin, prev);
        } else {
            // Зупинка неактивного каналу — попередження, але без виключення
            System.out.printf("[MOCK PWM] pin=%d  STOP   (channel was already inactive)%n", pin);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>In mock mode: stops all active channels and logs a summary.</p>
     */
    @Override
    public void close() {
        // Зупиняємо усі активні канали, щоб не залишити симульованих ресурсів відкритими
        activeChannels.forEach((pin, desc) ->
                System.out.printf("[MOCK PWM] pin=%d  CLOSE  (was %s)%n", pin, desc));
        activeChannels.clear();
        System.out.println("[MOCK PWM] closed — all channels stopped");
    }
}
