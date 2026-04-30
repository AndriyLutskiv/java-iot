package ua.crowpi.projects.p09;

/**
 * Represents a single recorded event in the Smart Home device log.
 *
 * <p>Corresponds to a row in the {@code device_events} SQLite table.
 * Both {@code oldState} and {@code profileId} are nullable (can be {@code null}).</p>
 *
 * <p>Valid {@code eventType} values: {@code LIGHT_ON}, {@code LIGHT_OFF},
 * {@code FAN_ON}, {@code FAN_OFF}, {@code PROFILE_SWITCH},
 * {@code TEMP_ALERT}, {@code TIMER_EXPIRED}.</p>
 *
 * <p>Valid {@code device} values: {@code LIGHT}, {@code FAN}, {@code SYSTEM}.</p>
 */
public class DeviceEvent {

    private int id;
    private String eventType;
    private String device;
    private String oldState;   // nullable — null for the first-ever state change
    private String newState;
    private Integer profileId; // nullable — null if no active profile at event time
    private String timestamp;  // ISO-8601 text

    /**
     * No-argument constructor for JDBC row mapping.
     */
    public DeviceEvent() {
    }

    /**
     * Full constructor.
     *
     * @param id        database primary key (0 for new records)
     * @param eventType event type constant, e.g. {@code "LIGHT_ON"}
     * @param device    device name, e.g. {@code "LIGHT"}
     * @param oldState  previous state string, or {@code null} if not applicable
     * @param newState  new state string after the event
     * @param profileId FK to the active profile at event time, or {@code null}
     * @param timestamp ISO-8601 event timestamp
     */
    public DeviceEvent(int id, String eventType, String device,
                       String oldState, String newState,
                       Integer profileId, String timestamp) {
        this.id = id;
        this.eventType = eventType;
        this.device = device;
        this.oldState = oldState;
        this.newState = newState;
        this.profileId = profileId;
        this.timestamp = timestamp;
    }

    /**
     * Convenience factory for creating a new (unsaved) event.
     *
     * @param eventType event type constant
     * @param device    device name
     * @param oldState  previous state, or {@code null}
     * @param newState  new state
     * @param profileId active profile FK, or {@code null}
     * @param timestamp ISO-8601 timestamp
     * @return new DeviceEvent with id=0
     */
    public static DeviceEvent of(String eventType, String device,
                                 String oldState, String newState,
                                 Integer profileId, String timestamp) {
        return new DeviceEvent(0, eventType, device, oldState, newState, profileId, timestamp);
    }

    /** @return database primary key */
    public int getId() { return id; }

    /** @param id database primary key */
    public void setId(int id) { this.id = id; }

    /** @return event type string */
    public String getEventType() { return eventType; }

    /** @param eventType event type string */
    public void setEventType(String eventType) { this.eventType = eventType; }

    /** @return device name string */
    public String getDevice() { return device; }

    /** @param device device name */
    public void setDevice(String device) { this.device = device; }

    /** @return previous state, may be {@code null} */
    public String getOldState() { return oldState; }

    /** @param oldState previous state */
    public void setOldState(String oldState) { this.oldState = oldState; }

    /** @return new state after the event */
    public String getNewState() { return newState; }

    /** @param newState new state */
    public void setNewState(String newState) { this.newState = newState; }

    /** @return active profile FK at event time, or {@code null} */
    public Integer getProfileId() { return profileId; }

    /** @param profileId profile FK */
    public void setProfileId(Integer profileId) { this.profileId = profileId; }

    /** @return ISO-8601 event timestamp */
    public String getTimestamp() { return timestamp; }

    /** @param timestamp ISO-8601 timestamp */
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return String.format("DeviceEvent{id=%d, type='%s', device='%s', %s→%s, t=%s}",
                id, eventType, device, oldState, newState, timestamp);
    }
}
