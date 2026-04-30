package ua.crowpi.projects.p09;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.exception.DatabaseException;
import ua.crowpi.core.hardware.I2cFacade;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Menu item [3]: toggles the fan relay and records a DeviceEvent.
 *
 * <p>Mirrors the structure of {@link LightMenuItem} but controls Relay 2 (fan).
 * Note that {@link AutoTempControl} can also turn the fan on automatically —
 * this menu item allows manual override in either direction.</p>
 */
public class FanMenuItem implements MenuItem {

    private static final Logger LOG = LogManager.getLogger(FanMenuItem.class);
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final FanController fanController;
    private final DeviceState deviceState;
    private final DeviceEventRepository eventRepo;
    private final I2cFacade lcd;

    /**
     * Creates a FanMenuItem.
     *
     * @param fanController relay controller for the fan
     * @param deviceState   shared runtime state (updated on toggle)
     * @param eventRepo     repository for logging DeviceEvents
     * @param lcd           I2C LCD facade for feedback display
     */
    public FanMenuItem(FanController fanController, DeviceState deviceState,
                       DeviceEventRepository eventRepo, I2cFacade lcd) {
        this.fanController = fanController;
        this.deviceState = deviceState;
        this.eventRepo = eventRepo;
        this.lcd = lcd;
    }

    /** {@inheritDoc} */
    @Override
    public String getLabel() {
        return deviceState.isFanOn() ? "FAN:   TURN OFF " : "FAN:   TURN ON  ";
    }

    /** {@inheritDoc} */
    @Override
    public void execute() {
        String oldState = deviceState.isFanOn() ? "ON" : "OFF";
        fanController.toggle();
        boolean newOn = fanController.isOn();
        deviceState.setFanOn(newOn);
        String newState = newOn ? "ON" : "OFF";

        LcdHelper.writeLine(lcd, 0, "FAN: " + newState + "           ");
        LcdHelper.writeLine(lcd, 1, "                ");

        DeviceEvent event = DeviceEvent.of(
                newOn ? "FAN_ON" : "FAN_OFF",
                "FAN", oldState, newState,
                null,
                LocalDateTime.now().format(TS_FMT));
        try {
            eventRepo.insert(event);
        } catch (DatabaseException e) {
            LOG.error("Failed to log fan event: {}", e.getMessage());
        }
        LOG.info("Fan toggled: {} → {}", oldState, newState);
    }
}
