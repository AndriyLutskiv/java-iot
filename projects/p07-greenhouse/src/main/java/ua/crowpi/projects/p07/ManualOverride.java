package ua.crowpi.projects.p07;

/**
 * Tracks manual override flags that allow the user to force actuators on
 * regardless of the automatic rules evaluated by {@link GreenhouseController}.
 *
 * <p>The pump override is toggled by pressing the physical button on GPIO 11.
 * When {@link #isPumpForced()} returns {@code true}, the pump stays on even
 * if the soil moisture is above the configured threshold.</p>
 *
 * <p>All fields are {@code volatile} because they are written by the GPIO
 * button listener callback (runs on the MockGpioFacade simulator thread or
 * the Pi4J event thread) and read by the {@link GreenhouseController} on the
 * {@link java.util.concurrent.ScheduledExecutorService} polling thread.</p>
 */
public class ManualOverride {

    // volatile гарантує видимість між потоком кнопки та потоком планувальника
    // без необхідності у synchronized блоці (одна операція read/write — атомарна для boolean)
    private volatile boolean pumpForced = false;

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the pump manual override is currently active.
     *
     * <p>When {@code true}, the rules engine ({@link GreenhouseController#evaluate})
     * will force the pump on regardless of soil moisture readings.</p>
     *
     * @return {@code true} if pump is manually forced on
     */
    public boolean isPumpForced() {
        return pumpForced;
    }

    // -------------------------------------------------------------------------
    // Mutators
    // -------------------------------------------------------------------------

    /**
     * Toggles the pump forced state between {@code true} and {@code false}.
     *
     * <p>Called by the button press listener on GPIO 11. Each button press
     * alternates between manual pump override on and off.</p>
     */
    public void togglePump() {
        // Інвертуємо прапор — кожне натискання кнопки перемикає стан
        pumpForced = !pumpForced;
        System.out.printf("[OVERRIDE] Pump forced → %s%n", pumpForced ? "ON" : "OFF");
    }

    /**
     * Resets all manual overrides to their default off state.
     *
     * <p>Called during shutdown or when the project is restarted to ensure
     * a clean initial state.</p>
     */
    public void reset() {
        // Скидаємо всі ручні перевизначення до початкового стану
        pumpForced = false;
        System.out.println("[OVERRIDE] All overrides reset");
    }
}
