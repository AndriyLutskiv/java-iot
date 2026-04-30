package ua.crowpi.projects.p09;

/**
 * Represents a named configuration profile for the Smart Home Assistant.
 *
 * <p>Three default profiles are seeded on startup: {@code DAY} (26 °C threshold),
 * {@code NIGHT} (22 °C), and {@code VACATION} (30 °C). The active profile controls
 * the fan auto-on temperature and the initial light/fan states.</p>
 *
 * <p>Corresponds to a row in the {@code profiles} SQLite table.</p>
 */
public class Profile {

    private int id;
    private String name;
    private double tempThreshold;
    private boolean lightOn;
    private boolean fanOn;
    private String createdAt;  // ISO-8601 text, e.g. "2024-01-15T08:00:00"

    /**
     * No-argument constructor required for JDBC row mapping.
     */
    public Profile() {
    }

    /**
     * Full constructor for creating a profile with all fields.
     *
     * @param id            database primary key (0 for new, unsaved profiles)
     * @param name          profile name, e.g. {@code "DAY"}
     * @param tempThreshold fan auto-on threshold in degrees Celsius
     * @param lightOn       whether light should be on for this profile
     * @param fanOn         whether fan should be on for this profile
     * @param createdAt     ISO-8601 creation timestamp string
     */
    public Profile(int id, String name, double tempThreshold,
                   boolean lightOn, boolean fanOn, String createdAt) {
        this.id = id;
        this.name = name;
        this.tempThreshold = tempThreshold;
        this.lightOn = lightOn;
        this.fanOn = fanOn;
        this.createdAt = createdAt;
    }

    /** @return database primary key */
    public int getId() { return id; }

    /** @param id database primary key */
    public void setId(int id) { this.id = id; }

    /** @return profile name (e.g. {@code "DAY"}) */
    public String getName() { return name; }

    /** @param name profile name */
    public void setName(String name) { this.name = name; }

    /** @return fan auto-on temperature threshold in °C */
    public double getTempThreshold() { return tempThreshold; }

    /** @param tempThreshold fan auto-on threshold in °C */
    public void setTempThreshold(double tempThreshold) { this.tempThreshold = tempThreshold; }

    /** @return {@code true} if light should be on in this profile */
    public boolean isLightOn() { return lightOn; }

    /** @param lightOn light state for this profile */
    public void setLightOn(boolean lightOn) { this.lightOn = lightOn; }

    /** @return {@code true} if fan should be on in this profile */
    public boolean isFanOn() { return fanOn; }

    /** @param fanOn fan state for this profile */
    public void setFanOn(boolean fanOn) { this.fanOn = fanOn; }

    /** @return ISO-8601 creation timestamp */
    public String getCreatedAt() { return createdAt; }

    /** @param createdAt ISO-8601 creation timestamp */
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format("Profile{id=%d, name='%s', threshold=%.1f°C, light=%s, fan=%s}",
                id, name, tempThreshold, lightOn, fanOn);
    }
}
