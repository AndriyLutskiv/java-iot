package ua.crowpi.projects.p09;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.exception.DatabaseException;
import ua.crowpi.core.hardware.I2cFacade;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Menu item [2]: toggles the light relay and records a DeviceEvent.
 *
 * <p>The label shown on the LCD reflects the <em>current</em> state so the user
 * always sees what pressing SELECT will do:</p>
 * <ul>
 *   <li>When light is off → {@code "LIGHT → TURN ON "}</li>
 *   <li>When light is on  → {@code "LIGHT → TURN OFF"}</li>
 * </ul>
 */
public class LightMenuItem implements MenuItem {

    private static final Logger LOG = LogManager.getLogger(LightMenuItem.class);
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final LightController lightController;
    private final DeviceState deviceState;
    private final DeviceEventRepository eventRepo;
    private final I2cFacade lcd;

    /**
     * Creates a LightMenuItem.
     *
     * @param lightController relay controller for the light
     * @param deviceState     shared runtime state (updated on toggle)
     * @param eventRepo       repository for logging DeviceEvents
     * @param lcd             I2C LCD facade for feedback display
     */
    public LightMenuItem(LightController lightController, DeviceState deviceState,
                         DeviceEventRepository eventRepo, I2cFacade lcd) {
        this.lightController = lightController;
        this.deviceState = deviceState;
        this.eventRepo = eventRepo;
        this.lcd = lcd;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a label that indicates the action that will be performed,
     * not the current state (more intuitive UX for beginners).</p>
     */
    @Override
    public String getLabel() {
        // Показуємо що СТАНЕТЬСЯ при виборі, а не поточний стан
        return deviceState.isLightOn() ? "LIGHT: TURN OFF " : "LIGHT: TURN ON  ";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Toggles the light, updates shared state, logs the DeviceEvent, and
     * writes brief confirmation to the LCD.</p>
     */
    @Override
    public void execute() {
        String oldState = deviceState.isLightOn() ? "ON" : "OFF";
        lightController.toggle();
        boolean newOn = lightController.isOn();
        deviceState.setLightOn(newOn);
        String newState = newOn ? "ON" : "OFF";

        // Пишемо підтвердження на LCD
        LcdHelper.writeLine(lcd, 0, "LIGHT: " + newState + "        ");
        LcdHelper.writeLine(lcd, 1, "                ");

        // Записуємо подію в журнал
        DeviceEvent event = DeviceEvent.of(
                newOn ? "LIGHT_ON" : "LIGHT_OFF",
                "LIGHT", oldState, newState,
                null,
                LocalDateTime.now().format(TS_FMT));
        try {
            eventRepo.insert(event);
        } catch (DatabaseException e) {
            LOG.error("Failed to log light event: {}", e.getMessage());
        }
        LOG.info("Light toggled: {} → {}", oldState, newState);
    }
}
