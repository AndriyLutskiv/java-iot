package ua.crowpi.core.mock;

import ua.crowpi.core.hardware.GpioFacade;
import ua.crowpi.core.hardware.PinChangeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mock implementation of {@link GpioFacade} for running on a laptop (no RPi required).
 *
 * <p>All write operations are logged to stdout so the student can see what signal
 * the real hardware would receive. {@link #readInput(int)} alternates between
 * {@code true} and {@code false} on successive calls to simulate a toggling sensor.
 * Registered {@link PinChangeListener}s are fired every 5 seconds for every watched
 * pin, simulating periodic GPIO events (e.g. PIR motion detection).</p>
 */
public class MockGpioFacade implements GpioFacade {

    // Зберігаємо поточний стан кожного виходу для реалістичного logування
    private final Map<Integer, Boolean> outputStates = new ConcurrentHashMap<>();

    // Лічильник для readInput — непарні виклики повертають true, парні — false
    private final Map<Integer, AtomicBoolean> inputToggles = new ConcurrentHashMap<>();

    // Список слухачів, яких треба повідомляти при симульованих змінах рівня
    private final Map<Integer, List<PinChangeListener>> listeners = new ConcurrentHashMap<>();

    // Планувальник запускає симульовані події GPIO кожні 5 секунд
    private final ScheduledExecutorService simulator =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "mock-gpio-simulator");
                // Daemon-потік не заважає JVM завершитись після виходу з main()
                t.setDaemon(true);
                return t;
            });

    /**
     * Creates a MockGpioFacade and starts the background simulator thread.
     * The simulator fires all registered listeners every 5 seconds.
     */
    public MockGpioFacade() {
        // Кожні 5 секунд надсилаємо симульований HIGH-імпульс усім зареєстрованим слухачам
        simulator.scheduleAtFixedRate(this::fireAllListeners, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * {@inheritDoc}
     *
     * <p>In mock mode: no-op — direction switching is only meaningful on real hardware.</p>
     */
    @Override
    public void setInput(int pin) {
        System.out.printf("[MOCK GPIO] setInput(pin=%d) — direction switched to input%n", pin);
    }

    /**
     * {@inheritDoc}
     *
     * <p>In mock mode: logs the operation and stores the new state for reference.</p>
     */
    @Override
    public void setOutput(int pin, boolean high) {
        outputStates.put(pin, high);
        System.out.printf("[MOCK GPIO] pin=%d → %s%n", pin, high ? "HIGH" : "LOW");
    }

    /**
     * {@inheritDoc}
     *
     * <p>In mock mode: alternates between {@code true} and {@code false} on each call
     * to the same pin. This simulates a sensor that toggles its output.</p>
     */
    @Override
    public boolean readInput(int pin) {
        // getOrDefault з putIfAbsent для потокобезпечної ініціалізації стану
        inputToggles.putIfAbsent(pin, new AtomicBoolean(false));
        // compareAndSet(false,true) повертає true якщо значення було false (тобто тепер true)
        boolean current = inputToggles.get(pin).getAndSet(
                !inputToggles.get(pin).get()
        );
        System.out.printf("[MOCK GPIO] readInput(pin=%d) → %s%n", pin, current);
        return current;
    }

    /**
     * {@inheritDoc}
     *
     * <p>In mock mode: stores the listener so the background simulator can fire it.</p>
     */
    @Override
    public void addListener(int pin, PinChangeListener listener) {
        listeners.computeIfAbsent(pin, k -> new ArrayList<>()).add(listener);
        System.out.printf("[MOCK GPIO] addListener registered on pin=%d%n", pin);
    }

    /**
     * {@inheritDoc}
     *
     * <p>In mock mode: logs the PWM parameters without touching real hardware.</p>
     */
    @Override
    public void pwm(int pin, int frequencyHz, float dutyCycle) {
        System.out.printf("[MOCK GPIO] pwm(pin=%d, freq=%d Hz, duty=%.1f%%)%n",
                pin, frequencyHz, dutyCycle * 100);
    }

    /**
     * {@inheritDoc}
     *
     * <p>In mock mode: shuts down the simulator thread gracefully.</p>
     */
    @Override
    public void close() {
        simulator.shutdownNow();
        System.out.println("[MOCK GPIO] closed — simulator stopped");
    }

    // -------------------------------------------------------------------------
    // Внутрішня логіка симулятора
    // -------------------------------------------------------------------------

    /**
     * Fires a simulated HIGH event on every registered listener pin.
     * Called by the background scheduler every 5 seconds.
     */
    private void fireAllListeners() {
        for (Map.Entry<Integer, List<PinChangeListener>> entry : listeners.entrySet()) {
            int pin = entry.getKey();
            // Симулюємо короткий HIGH-імпульс — аналогічно спрацюванню PIR-датчика
            for (PinChangeListener listener : entry.getValue()) {
                try {
                    listener.onPinChange(pin, true);
                } catch (Exception e) {
                    System.err.printf("[MOCK GPIO] Listener error on pin=%d: %s%n",
                            pin, e.getMessage());
                }
            }
        }
    }
}
