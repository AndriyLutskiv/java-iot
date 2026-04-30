package ua.crowpi.projects.p09;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.exception.DatabaseException;
import ua.crowpi.core.hardware.I2cFacade;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Menu item [4]: sets a timer that automatically turns off the light or fan
 * after a specified number of minutes.
 *
 * <p>The user enters the number of minutes via the {@link NavigationInput}
 * UP/DOWN actions (increment/decrement) and confirms with SELECT. The timer is
 * implemented with a single-threaded {@link ScheduledExecutorService} that fires
 * a one-shot task.</p>
 *
 * <p>Only one timer can be active at a time; starting a new timer cancels the previous one.</p>
 */
public class TimerMenuItem implements MenuItem {

    private static final Logger LOG = LogManager.getLogger(TimerMenuItem.class);
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final LightController lightController;
    private final FanController fanController;
    private final DeviceState deviceState;
    private final DeviceEventRepository eventRepo;
    private final I2cFacade lcd;
    private final NavigationInput input;

    // ScheduledExecutorService керує таймером вимкнення пристроїв
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "device-timer");
                t.setDaemon(true);
                return t;
            });

    // Поточний активний таймер (може бути null якщо таймер не встановлений)
    private ScheduledFuture<?> activeTimer = null;

    /**
     * Creates a TimerMenuItem.
     *
     * @param lightController light relay for turn-off action
     * @param fanController   fan relay for turn-off action
     * @param deviceState     shared state for tracking what is on
     * @param eventRepo       for logging TIMER_EXPIRED events
     * @param lcd             I2C LCD facade
     * @param input           navigation input for minute selection
     */
    public TimerMenuItem(LightController lightController, FanController fanController,
                         DeviceState deviceState, DeviceEventRepository eventRepo,
                         I2cFacade lcd, NavigationInput input) {
        this.lightController = lightController;
        this.fanController = fanController;
        this.deviceState = deviceState;
        this.eventRepo = eventRepo;
        this.lcd = lcd;
        this.input = input;
    }

    /** {@inheritDoc} */
    @Override
    public String getLabel() {
        return activeTimer != null ? "TIMER: ACTIVE   " : "SET TIMER       ";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Enters a sub-menu where the user increments/decrements minutes with UP/DOWN,
     * then confirms with SELECT. A previous active timer is cancelled first.</p>
     */
    @Override
    public void execute() {
        // Якщо таймер вже активний — скасовуємо його перед встановленням нового
        cancelActiveTimer();

        int minutes = 5; // початкове значення за замовчуванням
        boolean confirmed = false;

        LcdHelper.writeLine(lcd, 0, "SET TIMER (min) ");
        LcdHelper.writeLine(lcd, 1, String.format("  %3d min       ", minutes));

        // Цикл вибору кількості хвилин: UP/DOWN змінюють значення, SELECT підтверджує
        while (!confirmed) {
            NavigationInput.NavAction action = input.poll(10_000);
            if (action == null) {
                // Таймаут без дії — скасовуємо
                LcdHelper.writeLine(lcd, 1, "CANCELLED       ");
                return;
            }
            switch (action) {
                case UP:
                    minutes = Math.min(minutes + 1, 99);
                    LcdHelper.writeLine(lcd, 1, String.format("  %3d min       ", minutes));
                    break;
                case DOWN:
                    minutes = Math.max(minutes - 1, 1);
                    LcdHelper.writeLine(lcd, 1, String.format("  %3d min       ", minutes));
                    break;
                case SELECT:
                    confirmed = true;
                    break;
                case BACK:
                    LcdHelper.writeLine(lcd, 1, "CANCELLED       ");
                    return;
                default:
                    break;
            }
        }

        final int finalMinutes = minutes;
        // Плануємо вимкнення всіх активних пристроїв через N хвилин
        activeTimer = scheduler.schedule(() -> {
            String now = LocalDateTime.now().format(TS_FMT);
            if (deviceState.isLightOn()) {
                lightController.turnOff();
                deviceState.setLightOn(false);
                logTimerEvent("LIGHT", "ON", "OFF", now);
            }
            if (deviceState.isFanOn()) {
                fanController.turnOff();
                deviceState.setFanOn(false);
                logTimerEvent("FAN", "ON", "OFF", now);
            }
            activeTimer = null;
            LOG.info("Timer expired after {} min — devices turned off", finalMinutes);
        }, finalMinutes, TimeUnit.MINUTES);

        LcdHelper.writeLine(lcd, 0, "TIMER SET       ");
        LcdHelper.writeLine(lcd, 1, String.format("  %3d min OK    ", minutes));
        LOG.info("Timer set for {} minutes", minutes);
    }

    /**
     * Cancels the active timer and shuts down the scheduler.
     * Called from {@link SmartHomeProject#shutdown()}.
     */
    public void cancel() {
        cancelActiveTimer();
        scheduler.shutdownNow();
    }

    // -------------------------------------------------------------------------

    /** Cancels the currently scheduled timer task (if any). */
    private void cancelActiveTimer() {
        if (activeTimer != null && !activeTimer.isDone()) {
            activeTimer.cancel(false);
            activeTimer = null;
            LOG.debug("Previous active timer cancelled");
        }
    }

    /** Logs a TIMER_EXPIRED event, ignoring database errors. */
    private void logTimerEvent(String device, String oldState, String newState, String ts) {
        DeviceEvent event = DeviceEvent.of("TIMER_EXPIRED", device, oldState, newState, null, ts);
        try {
            eventRepo.insert(event);
        } catch (DatabaseException e) {
            LOG.error("Failed to log TIMER_EXPIRED event: {}", e.getMessage());
        }
    }
}
