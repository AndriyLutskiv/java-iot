package ua.crowpi.projects.p09;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.exception.DatabaseException;
import ua.crowpi.core.hardware.I2cFacade;

import java.util.Optional;

/**
 * Menu item [7]: saves the current device state into the active profile in the database.
 *
 * <p>Updates the active profile's {@code light_on} and {@code fan_on} columns so
 * future activations of this profile restore the current state.</p>
 */
public class SaveStateMenuItem implements MenuItem {

    private static final Logger LOG = LogManager.getLogger(SaveStateMenuItem.class);

    private final ProfileRepository profileRepo;
    private final DeviceState deviceState;
    private final I2cFacade lcd;

    /**
     * Creates a SaveStateMenuItem.
     *
     * @param profileRepo for finding and updating the active profile
     * @param deviceState current device state to persist
     * @param lcd         I2C LCD for confirmation feedback
     */
    public SaveStateMenuItem(ProfileRepository profileRepo,
                             DeviceState deviceState, I2cFacade lcd) {
        this.profileRepo = profileRepo;
        this.deviceState = deviceState;
        this.lcd = lcd;
    }

    /** {@inheritDoc} */
    @Override
    public String getLabel() {
        return "SAVE STATE      ";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Finds the active profile by name, updates its {@code light_on}
     * and {@code fan_on} fields to match the current {@link DeviceState}, and
     * calls {@link ProfileRepository#update(Profile)}.</p>
     */
    @Override
    public void execute() {
        String profileName = deviceState.getActiveProfileName();
        try {
            Optional<Profile> found = profileRepo.findByName(profileName);
            if (!found.isPresent()) {
                LcdHelper.writeLine(lcd, 0, "PROFILE MISSING ");
                LcdHelper.writeLine(lcd, 1, profileName.substring(0,
                        Math.min(16, profileName.length())));
                LOG.warn("Cannot save state: profile '{}' not found in DB", profileName);
                return;
            }

            Profile profile = found.get();
            // Оновлюємо поля профілю поточним станом пристроїв
            profile.setLightOn(deviceState.isLightOn());
            profile.setFanOn(deviceState.isFanOn());
            profileRepo.update(profile);

            LcdHelper.writeLine(lcd, 0, "STATE SAVED     ");
            LcdHelper.writeLine(lcd, 1, String.format("%-16s", profileName));
            LOG.info("State saved to profile '{}': light={}, fan={}",
                    profileName, deviceState.isLightOn(), deviceState.isFanOn());
        } catch (DatabaseException e) {
            LOG.error("Failed to save state: {}", e.getMessage());
            LcdHelper.writeLine(lcd, 0, "SAVE FAILED     ");
            LcdHelper.writeLine(lcd, 1, "                ");
        }
    }
}
