package ua.crowpi.projects.p09;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.exception.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC repository for the {@code profiles} table.
 *
 * <p>Provides CRUD operations for {@link Profile} objects. Each method opens
 * and closes its own {@link PreparedStatement}; the shared {@link Connection}
 * is managed by {@link DatabaseManager} and must remain open for the lifetime
 * of this repository.</p>
 *
 * <p>Concurrency note: all methods are called from the menu thread or the
 * AutoTempControl thread — callers must not invoke these methods simultaneously
 * without external synchronisation.</p>
 */
public class ProfileRepository {

    private static final Logger LOG = LogManager.getLogger(ProfileRepository.class);

    private final Connection connection;

    /**
     * Creates a ProfileRepository using the given JDBC connection.
     *
     * @param connection open JDBC connection (must not be {@code null})
     */
    public ProfileRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * Returns all profiles stored in the database, ordered by ID.
     *
     * @return list of all profiles; empty list if none exist
     * @throws DatabaseException if a SQL error occurs
     */
    public List<Profile> findAll() throws DatabaseException {
        String sql = "SELECT id, name, temp_threshold_c, light_on, fan_on, created_at "
                   + "FROM profiles ORDER BY id";
        List<Profile> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("findAll() failed", e);
        }
        return result;
    }

    /**
     * Finds a profile by its unique name (case-sensitive).
     *
     * @param name the profile name to search for, e.g. {@code "DAY"}
     * @return an {@link Optional} containing the profile, or empty if not found
     * @throws DatabaseException if a SQL error occurs
     */
    public Optional<Profile> findByName(String name) throws DatabaseException {
        String sql = "SELECT id, name, temp_threshold_c, light_on, fan_on, created_at "
                   + "FROM profiles WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("findByName() failed for name='" + name + "'", e);
        }
        return Optional.empty();
    }

    /**
     * Inserts a new profile into the database and returns it with the generated ID.
     *
     * @param profile the profile to insert (id field is ignored and overwritten)
     * @return the same profile object with its new database-assigned {@code id} set
     * @throws DatabaseException if a SQL error occurs (e.g. duplicate name)
     */
    public Profile save(Profile profile) throws DatabaseException {
        String sql = "INSERT INTO profiles (name, temp_threshold_c, light_on, fan_on, created_at) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, profile.getName());
            ps.setDouble(2, profile.getTempThreshold());
            // SQLite зберігає boolean як INTEGER 0/1
            ps.setInt(3, profile.isLightOn() ? 1 : 0);
            ps.setInt(4, profile.isFanOn() ? 1 : 0);
            ps.setString(5, profile.getCreatedAt());
            ps.executeUpdate();

            // Отримуємо згенерований PRIMARY KEY і записуємо назад у об'єкт
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    profile.setId(keys.getInt(1));
                }
            }
            LOG.debug("Saved profile: {}", profile);
        } catch (SQLException e) {
            throw new DatabaseException("save() failed for profile='" + profile.getName() + "'", e);
        }
        return profile;
    }

    /**
     * Updates an existing profile's settings.
     *
     * <p>The profile must already exist in the database (identified by {@code id}).
     * If no row is updated (unknown ID), the method returns silently.</p>
     *
     * @param profile the profile with updated fields; {@code id} must be a valid PK
     * @throws DatabaseException if a SQL error occurs
     */
    public void update(Profile profile) throws DatabaseException {
        String sql = "UPDATE profiles SET temp_threshold_c = ?, light_on = ?, fan_on = ? "
                   + "WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, profile.getTempThreshold());
            ps.setInt(2, profile.isLightOn() ? 1 : 0);
            ps.setInt(3, profile.isFanOn() ? 1 : 0);
            ps.setInt(4, profile.getId());
            int rows = ps.executeUpdate();
            if (rows == 0) {
                LOG.warn("update() matched 0 rows for profile id={}", profile.getId());
            } else {
                LOG.debug("Updated profile id={}", profile.getId());
            }
        } catch (SQLException e) {
            throw new DatabaseException("update() failed for profile id=" + profile.getId(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Внутрішній маппер ResultSet → Profile
    // -------------------------------------------------------------------------

    /**
     * Maps the current row of a {@link ResultSet} to a {@link Profile} object.
     *
     * @param rs ResultSet positioned at the row to map
     * @return populated Profile
     * @throws SQLException if any column read fails
     */
    private Profile mapRow(ResultSet rs) throws SQLException {
        Profile p = new Profile();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setTempThreshold(rs.getDouble("temp_threshold_c"));
        // SQLite повертає INTEGER 1/0 — перетворюємо у boolean
        p.setLightOn(rs.getInt("light_on") == 1);
        p.setFanOn(rs.getInt("fan_on") == 1);
        p.setCreatedAt(rs.getString("created_at"));
        return p;
    }
}
