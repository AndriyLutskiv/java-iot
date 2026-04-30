package ua.crowpi.projects.p09;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.exception.DatabaseException;
import ua.crowpi.core.hardware.I2cFacade;

import java.util.List;

/**
 * Menu item [6]: displays the last 10 device events on the LCD with scroll navigation.
 *
 * <p>Each event occupies two LCD rows:</p>
 * <pre>
 *   Row 0: event_type  (e.g. "LIGHT_ON        ")
 *   Row 1: timestamp   (e.g. "2024-01-15T14:23")
 * </pre>
 * <p>UP and DOWN scroll through the list. BACK exits back to the main menu.</p>
 */
public class ViewEventLogMenuItem implements MenuItem {

    private static final Logger LOG = LogManager.getLogger(ViewEventLogMenuItem.class);

    private static final int EVENT_LOG_LIMIT = 10;

    private final DeviceEventRepository eventRepo;
    private final I2cFacade lcd;
    private final NavigationInput input;

    /**
     * Creates a ViewEventLogMenuItem.
     *
     * @param eventRepo repository for fetching recent events
     * @param lcd       I2C LCD facade
     * @param input     navigation input for scroll control
     */
    public ViewEventLogMenuItem(DeviceEventRepository eventRepo,
                                I2cFacade lcd, NavigationInput input) {
        this.eventRepo = eventRepo;
        this.lcd = lcd;
        this.input = input;
    }

    /** {@inheritDoc} */
    @Override
    public String getLabel() {
        return "VIEW EVENT LOG  ";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Loads the last {@value #EVENT_LOG_LIMIT} events and enters a scroll loop.
     * UP/DOWN navigate; BACK or SELECT returns to the main menu.</p>
     */
    @Override
    public void execute() {
        List<DeviceEvent> events;
        try {
            events = eventRepo.findRecent(EVENT_LOG_LIMIT);
        } catch (DatabaseException e) {
            LOG.error("Failed to load event log: {}", e.getMessage());
            LcdHelper.writeLine(lcd, 0, "DB ERROR        ");
            LcdHelper.writeLine(lcd, 1, "                ");
            return;
        }

        if (events.isEmpty()) {
            LcdHelper.writeLine(lcd, 0, "NO EVENTS YET   ");
            LcdHelper.writeLine(lcd, 1, "                ");
            // Коротка затримка щоб студент встиг прочитати повідомлення перед поверненням
            sleep(2000);
            return;
        }

        int index = 0;
        displayEvent(events, index);

        // Цикл прокрутки — виходимо по BACK або SELECT
        while (true) {
            NavigationInput.NavAction action = input.poll(15_000);
            if (action == null || action == NavigationInput.NavAction.BACK
                    || action == NavigationInput.NavAction.SELECT) {
                break;
            }
            if (action == NavigationInput.NavAction.DOWN && index < events.size() - 1) {
                index++;
                displayEvent(events, index);
            } else if (action == NavigationInput.NavAction.UP && index > 0) {
                index--;
                displayEvent(events, index);
            }
        }
    }

    // -------------------------------------------------------------------------

    /**
     * Renders the event at {@code index} onto the LCD.
     *
     * @param events list of events
     * @param index  which event to show (0 = newest)
     */
    private void displayEvent(List<DeviceEvent> events, int index) {
        DeviceEvent e = events.get(index);
        // Рядок 1: тип події з лічильником (n/total)
        String line0 = String.format("%-11s%2d/%-2d",
                e.getEventType(), index + 1, events.size());
        // Рядок 2: перші 16 символів timestamp
        String line1 = e.getTimestamp() != null
                ? e.getTimestamp().substring(0, Math.min(16, e.getTimestamp().length()))
                : "               ";
        LcdHelper.writeLine(lcd, 0, line0);
        LcdHelper.writeLine(lcd, 1, line1);
    }

    /**
     * Sleeps for the specified number of milliseconds, ignoring interruptions.
     *
     * @param ms milliseconds to sleep
     */
    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
