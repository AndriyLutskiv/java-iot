package ua.crowpi.projects.p09;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.exception.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC repository for the {@code device_events} table.
 *
 * <p>Provides insert and query operations for {@link DeviceEvent} objects.
 * This table is append-only — events are never updated or deleted, only inserted
 * (event sourcing pattern: the log is the ground truth).</p>
 */
public class DeviceEventRepository {

    private static final Logger LOG = LogManager.getLogger(DeviceEventRepository.class);

    private final Connection connection;

    /**
     * Creates a DeviceEventRepository using the given JDBC connection.
     *
     * @param connection open JDBC connection (must not be {@code null})
     */
    public DeviceEventRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * Inserts a new device event into the database.
     *
     * <p>The {@code id} field of the event is set to the generated primary key
     * after a successful insert.</p>
     *
     * @param event the event to insert; must have all mandatory fields set
     * @throws DatabaseException if a SQL error occurs
     */
    public void insert(DeviceEvent event) throws DatabaseException {
        String sql = "INSERT INTO device_events "
                   + "(event_type, device, old_state, new_state, profile_id, timestamp) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, event.getEventType());
            ps.setString(2, event.getDevice());

            // old_state nullable — використовуємо setNull якщо відсутній
            if (event.getOldState() != null) {
                ps.setString(3, event.getOldState());
            } else {
                ps.setNull(3, Types.VARCHAR);
            }

            ps.setString(4, event.getNewState());

            // profile_id nullable FK
            if (event.getProfileId() != null) {
                ps.setInt(5, event.getProfileId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            ps.setString(6, event.getTimestamp());
            ps.executeUpdate();

            // Записуємо згенерований PK назад у об'єкт — зручно для тестів
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    event.setId(keys.getInt(1));
                }
            }
            LOG.debug("Inserted device event: {}", event);
        } catch (SQLException e) {
            throw new DatabaseException("insert() failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the most recent events ordered by timestamp descending.
     *
     * <p>Used by the VIEW EVENT LOG menu item to show the latest activity on the LCD.</p>
     *
     * @param limit maximum number of events to return (e.g. 10)
     * @return list of recent events, newest first; empty if no events exist
     * @throws DatabaseException if a SQL error occurs
     */
    public List<DeviceEvent> findRecent(int limit) throws DatabaseException {
        String sql = "SELECT id, event_type, device, old_state, new_state, profile_id, timestamp "
                   + "FROM device_events ORDER BY id DESC LIMIT ?";
        List<DeviceEvent> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("findRecent() failed", e);
        }
        return result;
    }

    /**
     * Counts how many events of a given type exist in the log.
     *
     * <p>Useful for analytics and automated tests. Example: counting how many
     * times the fan was turned on ({@code "FAN_ON"}).</p>
     *
     * @param eventType the event type to count, e.g. {@code "LIGHT_ON"}
     * @return number of matching rows
     * @throws DatabaseException if a SQL error occurs
     */
    public int countByType(String eventType) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM device_events WHERE event_type = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, eventType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("countByType() failed for type='" + eventType + "'", e);
        }
        return 0;
    }

    // -------------------------------------------------------------------------
    // Внутрішній маппер ResultSet → DeviceEvent
    // -------------------------------------------------------------------------

    /**
     * Maps the current row of a {@link ResultSet} to a {@link DeviceEvent}.
     *
     * @param rs ResultSet positioned at the row to map
     * @return populated DeviceEvent
     * @throws SQLException if any column read fails
     */
    private DeviceEvent mapRow(ResultSet rs) throws SQLException {
        DeviceEvent e = new DeviceEvent();
        e.setId(rs.getInt("id"));
        e.setEventType(rs.getString("event_type"));
        e.setDevice(rs.getString("device"));
        e.setOldState(rs.getString("old_state")); // returns null if SQL NULL
        e.setNewState(rs.getString("new_state"));

        // profile_id: getInt() повертає 0 якщо NULL — перевіряємо wasNull()
        int pid = rs.getInt("profile_id");
        e.setProfileId(rs.wasNull() ? null : pid);

        e.setTimestamp(rs.getString("timestamp"));
        return e;
    }
}
