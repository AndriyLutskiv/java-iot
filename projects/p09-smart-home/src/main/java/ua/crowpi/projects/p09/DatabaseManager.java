package ua.crowpi.projects.p09;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Manages the SQLite database lifecycle for the Smart Home Assistant.
 *
 * <p>Responsibilities:</p>
 * <ol>
 *   <li>Open / close the JDBC connection (file-based for production, in-memory for tests).</li>
 *   <li>Create tables via {@link #createTables()} — idempotent, safe to call multiple times.</li>
 *   <li>Seed the three default profiles (DAY, NIGHT, VACATION) if they don't exist yet.</li>
 * </ol>
 *
 * <p>The constructor accepts an already-open {@link Connection} so tests can pass an
 * {@code jdbc:sqlite::memory:} connection without touching the filesystem:</p>
 * <pre>{@code
 *   Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
 *   DatabaseManager db = new DatabaseManager(conn);
 *   db.createTables();
 * }</pre>
 */
public class DatabaseManager {

    private static final Logger LOG = LogManager.getLogger(DatabaseManager.class);

    /** ISO-8601 formatter used when recording creation timestamps. */
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** DDL for the profiles table. */
    private static final String DDL_PROFILES =
            "CREATE TABLE IF NOT EXISTS profiles ("
            + "id               INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "name             TEXT    NOT NULL UNIQUE,"
            + "temp_threshold_c REAL    NOT NULL DEFAULT 26.0,"
            + "light_on         INTEGER NOT NULL DEFAULT 0,"
            + "fan_on           INTEGER NOT NULL DEFAULT 0,"
            + "created_at       TEXT    NOT NULL"
            + ")";

    /** DDL for the device_events table. */
    private static final String DDL_DEVICE_EVENTS =
            "CREATE TABLE IF NOT EXISTS device_events ("
            + "id         INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "event_type TEXT    NOT NULL,"
            + "device     TEXT    NOT NULL,"
            + "old_state  TEXT,"
            + "new_state  TEXT    NOT NULL,"
            + "profile_id INTEGER REFERENCES profiles(id),"
            + "timestamp  TEXT    NOT NULL"
            + ")";

    private final Connection connection;

    /**
     * Creates a DatabaseManager wrapping an already-open JDBC connection.
     *
     * <p>Used directly in tests with {@code jdbc:sqlite::memory:}.</p>
     *
     * @param connection open JDBC connection to use for all operations
     */
    public DatabaseManager(Connection connection) {
        this.connection = connection;
    }

    /**
     * Opens a file-based SQLite database and returns a ready-to-use DatabaseManager.
     *
     * <p>The database file is created automatically if it does not exist.
     * On Raspberry Pi, {@code dbPath} is typically {@code "smartHome.db"} in the
     * current working directory.</p>
     *
     * @param dbPath path to the SQLite database file (e.g. {@code "smartHome.db"})
     * @return configured DatabaseManager with an open connection
     * @throws DatabaseException if the JDBC driver is missing or the file cannot be opened
     */
    public static DatabaseManager forFile(String dbPath) throws DatabaseException {
        try {
            // Явне завантаження драйвера — необхідне у fat JAR де ServiceLoader
            // не завжди знаходить JDBC-провайдерів автоматично
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            LOG.info("Opened SQLite database: {}", dbPath);
            return new DatabaseManager(conn);
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("SQLite JDBC driver not found on classpath", e);
        } catch (SQLException e) {
            throw new DatabaseException("Cannot open database file: " + dbPath, e);
        }
    }

    /**
     * Creates all required tables if they do not already exist.
     *
     * <p>Safe to call multiple times ({@code CREATE TABLE IF NOT EXISTS} semantics).
     * Must be called before any repository operations.</p>
     *
     * @throws DatabaseException if SQL execution fails
     */
    public void createTables() throws DatabaseException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(DDL_PROFILES);
            stmt.execute(DDL_DEVICE_EVENTS);
            LOG.debug("Tables created (or already existed)");
        } catch (SQLException e) {
            throw new DatabaseException("createTables() failed: " + e.getMessage(), e);
        }
    }

    /**
     * Inserts the three default profiles if they are not already present.
     *
     * <p>Default thresholds:</p>
     * <ul>
     *   <li>DAY — 26 °C</li>
     *   <li>NIGHT — 22 °C</li>
     *   <li>VACATION — 30 °C</li>
     * </ul>
     *
     * @param repo the ProfileRepository to use for existence checks and inserts
     * @throws DatabaseException if any insert fails
     */
    public void seedDefaultProfiles(ProfileRepository repo) throws DatabaseException {
        // Три дефолтних профілі покривають типові режими використання квартири
        String[] names     = {"DAY",  "NIGHT", "VACATION"};
        double[] thresholds = {26.0,   22.0,    30.0};

        String now = LocalDateTime.now().format(TS_FMT);

        for (int i = 0; i < names.length; i++) {
            if (!repo.findByName(names[i]).isPresent()) {
                // Профіль відсутній — вставляємо з дефолтними налаштуваннями
                Profile p = new Profile();
                p.setName(names[i]);
                p.setTempThreshold(thresholds[i]);
                p.setLightOn(false);
                p.setFanOn(false);
                p.setCreatedAt(now);
                repo.save(p);
                LOG.info("Seeded default profile: {}", names[i]);
            }
        }
    }

    /**
     * Returns the underlying JDBC connection for use by repository classes.
     *
     * @return open JDBC connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Closes the underlying JDBC connection.
     *
     * <p>Should be called from {@link SmartHomeProject#shutdown()} to release
     * the database file lock on the Raspberry Pi filesystem.</p>
     *
     * @throws DatabaseException if the connection cannot be closed cleanly
     */
    public void close() throws DatabaseException {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOG.info("Database connection closed");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to close database connection", e);
        }
    }
}
