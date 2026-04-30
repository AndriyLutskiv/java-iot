package ua.crowpi.projects.p09;

/**
 * Mutable snapshot of the current runtime state of Smart Home devices.
 *
 * <p>Shared between the menu items, AutoTempControl, and the main project class.
 * All mutations are performed on the EDT-equivalent thread (the menu loop), so
 * no additional synchronisation is required for the menu items themselves.
 * {@link AutoTempControl} reads {@code fanOn} and writes it; since it runs in a
 * separate thread, the field is declared {@code volatile}.</p>
 */
public class DeviceState {

    private volatile boolean lightOn;
    private volatile boolean fanOn;
    private volatile String activeProfileName;

    /**
     * Creates a DeviceState with all values explicitly specified.
     *
     * @param lightOn           current light relay state
     * @param fanOn             current fan relay state
     * @param activeProfileName name of the currently active profile
     */
    public DeviceState(boolean lightOn, boolean fanOn, String activeProfileName) {
        this.lightOn = lightOn;
        this.fanOn = fanOn;
        this.activeProfileName = activeProfileName;
    }

    /** @return {@code true} if the light relay is currently on */
    public boolean isLightOn() { return lightOn; }

    /** @param lightOn new light relay state */
    public void setLightOn(boolean lightOn) { this.lightOn = lightOn; }

    /** @return {@code true} if the fan relay is currently on */
    public boolean isFanOn() { return fanOn; }

    /** @param fanOn new fan relay state */
    public void setFanOn(boolean fanOn) { this.fanOn = fanOn; }

    /** @return name of the active profile */
    public String getActiveProfileName() { return activeProfileName; }

    /** @param activeProfileName name of the new active profile */
    public void setActiveProfileName(String activeProfileName) {
        this.activeProfileName = activeProfileName;
    }

    @Override
    public String toString() {
        return String.format("DeviceState{light=%s, fan=%s, profile='%s'}",
                lightOn, fanOn, activeProfileName);
    }
}
