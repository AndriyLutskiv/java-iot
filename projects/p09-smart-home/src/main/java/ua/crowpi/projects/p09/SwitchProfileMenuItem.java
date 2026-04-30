package ua.crowpi.projects.p09;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.exception.DatabaseException;
import ua.crowpi.core.hardware.I2cFacade;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Menu item [5]: cycles through the available profiles (DAY → NIGHT → VACATION → DAY)
 * and switches the active one.
 *
 * <p>Each time the user selects this item, the next profile in the list becomes active.
 * A {@code PROFILE_SWITCH} event is logged to the database, and
 * {@link AutoTempControl} is notified of the new temperature threshold.</p>
 */
public class SwitchProfileMenuItem implements MenuItem {

    private static final Logger LOG = LogManager.getLogger(SwitchProfileMenuItem.class);
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ProfileRepository profileRepo;
    private final DeviceEventRepository eventRepo;
    private final DeviceState deviceState;
    private final AutoTempControl autoTempControl;
    private final I2cFacade lcd;

    // Поточний індекс у списку профілів — циклічно збільшується при кожному виборі
    private int currentProfileIndex = 0;
    private List<Profile> cachedProfiles = null;

    /**
     * Creates a SwitchProfileMenuItem.
     *
     * @param profileRepo     to load the list of available profiles from DB
     * @param eventRepo       to log PROFILE_SWITCH events
     * @param deviceState     updated with new active profile name
     * @param autoTempControl notified of the new temperature threshold
     * @param lcd             I2C LCD for feedback
     */
    public SwitchProfileMenuItem(ProfileRepository profileRepo,
                                 DeviceEventRepository eventRepo,
                                 DeviceState deviceState,
                                 AutoTempControl autoTempControl,
                                 I2cFacade lcd) {
        this.profileRepo = profileRepo;
        this.eventRepo = eventRepo;
        this.deviceState = deviceState;
        this.autoTempControl = autoTempControl;
        this.lcd = lcd;
    }

    /** {@inheritDoc} */
    @Override
    public String getLabel() {
        return String.format("PROFILE:%-8s", deviceState.getActiveProfileName());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Advances to the next profile in the DB-loaded list and activates it.</p>
     */
    @Override
    public void execute() {
        List<Profile> profiles = loadProfiles();
        if (profiles.isEmpty()) {
            LcdHelper.writeLine(lcd, 0, "NO PROFILES     ");
            return;
        }

        // Переходимо до наступного профілю циклічно
        currentProfileIndex = (currentProfileIndex + 1) % profiles.size();
        Profile next = profiles.get(currentProfileIndex);

        String oldProfileName = deviceState.getActiveProfileName();
        deviceState.setActiveProfileName(next.getName());

        // Повідомляємо AutoTempControl про новий поріг температури
        autoTempControl.setActiveProfile(next);

        // Відображаємо підтвердження переключення
        LcdHelper.writeLine(lcd, 0, "PROFILE CHANGED ");
        LcdHelper.writeLine(lcd, 1, String.format("%-16s", next.getName()));

        // Логуємо зміну профілю
        DeviceEvent event = DeviceEvent.of(
                "PROFILE_SWITCH", "SYSTEM",
                oldProfileName, next.getName(),
                next.getId() > 0 ? next.getId() : null,
                LocalDateTime.now().format(TS_FMT));
        try {
            eventRepo.insert(event);
        } catch (DatabaseException e) {
            LOG.error("Failed to log PROFILE_SWITCH event: {}", e.getMessage());
        }
        LOG.info("Profile switched: {} → {}", oldProfileName, next.getName());
    }

    // -------------------------------------------------------------------------

    /**
     * Loads profiles from DB on first call, then uses the cached list.
     * DB errors are logged and an empty list is returned so the menu doesn't crash.
     */
    private List<Profile> loadProfiles() {
        if (cachedProfiles == null) {
            try {
                cachedProfiles = profileRepo.findAll();
                // Встановлюємо currentIndex на активний профіль при першому завантаженні
                for (int i = 0; i < cachedProfiles.size(); i++) {
                    if (cachedProfiles.get(i).getName()
                            .equals(deviceState.getActiveProfileName())) {
                        currentProfileIndex = i;
                        break;
                    }
                }
            } catch (DatabaseException e) {
                LOG.error("Failed to load profiles: {}", e.getMessage());
                return java.util.Collections.emptyList();
            }
        }
        return cachedProfiles;
    }
}
