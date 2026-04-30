package ua.crowpi.projects.p08;

import ua.crowpi.core.hardware.GpioFacade;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controls the electromagnetic door-lock relay connected to BCM GPIO pin 21.
 *
 * <p>The relay is normally-open (NO): driving the pin HIGH energises the coil and
 * unlocks the door; driving it LOW de-energises the coil and re-locks the door.
 * The {@link #unlock(int)} method uses a {@link ScheduledExecutorService} daemon thread
 * to schedule the automatic re-lock after the specified duration without blocking the
 * caller.</p>
 *
 * <p>Thread safety: {@code locked} is declared {@code volatile} so the main loop can
 * safely read it from a different thread than the scheduler that writes it.</p>
 */
public class RelayDoorLock {

    /** BCM GPIO pin connected to the relay's IN1 signal. */
    public static final int RELAY_PIN = 21;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** GPIO facade used to drive the relay pin. */
    private final GpioFacade gpio;

    /**
     * Whether the door is currently locked.
     * volatile — головний потік і потік планувальника можуть читати/писати безпечно
     */
    private volatile boolean locked = true;

    /**
     * Single-thread scheduler for the timed re-lock operation.
     * Daemon thread — не заважає JVM завершитись, якщо програма вийшла раніше
     */
    private final ScheduledExecutorService scheduler;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a RelayDoorLock and ensures the door starts in the locked state.
     *
     * @param gpio the GPIO facade used to drive the relay pin
     */
    public RelayDoorLock(GpioFacade gpio) {
        this.gpio = gpio;
        // Планувальник з daemon-потоком — не блокує завершення JVM
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rfid-relay-scheduler");
            t.setDaemon(true);
            return t;
        });
        // Гарантуємо початковий стан — двері замкнені при старті
        lock();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Unlocks the door by driving the relay pin HIGH, then schedules automatic re-lock
     * after {@code durationMs} milliseconds.
     *
     * <p>If called while the door is already unlocked, the re-lock timer is rescheduled
     * from the moment of this call (effectively extending the unlock window).</p>
     *
     * @param durationMs milliseconds to keep the door unlocked; must be positive
     */
    public void unlock(int durationMs) {
        // Встановлюємо стан ДО зміни GPIO, щоб isLocked() давав правильну відповідь
        // навіть якщо scheduler спрацює дуже швидко
        locked = false;
        gpio.setOutput(RELAY_PIN, true);  // HIGH → реле замикає контакти → двері відчиняються
        System.out.printf("[RELAY] Door UNLOCKED for %d ms%n", durationMs);

        // Планувальник повторно замкне двері через durationMs мілісекунд
        scheduler.schedule(() -> {
            // Зворотний відлік закінчився — повертаємо двері у стан "замкнено"
            lock();
            System.out.println("[RELAY] Auto-lock triggered after timeout");
        }, durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Immediately locks the door by driving the relay pin LOW.
     *
     * <p>Idempotent: safe to call even if the door is already locked.</p>
     */
    public void lock() {
        locked = true;
        gpio.setOutput(RELAY_PIN, false);  // LOW → реле розмикає контакти → двері замикаються
        System.out.println("[RELAY] Door LOCKED");
    }

    /**
     * Returns whether the door is currently in the locked state.
     *
     * @return {@code true} if the door is locked; {@code false} if unlocked
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * Shuts down the re-lock scheduler, releasing its daemon thread.
     * Should be called from the project's {@code shutdown()} method.
     */
    public void shutdown() {
        // Зупиняємо планувальник — якщо є очікуючі задачі, вони не виконуються
        scheduler.shutdownNow();
    }
}
