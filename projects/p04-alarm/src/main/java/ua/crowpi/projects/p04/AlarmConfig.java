package ua.crowpi.projects.p04;

import ua.crowpi.core.util.PropertiesLoader;

import java.util.Properties;

/**
 * Configuration holder for the alarm system, loaded from {@code alarm.properties}.
 *
 * <p>The properties file must be present on the classpath (i.e. under
 * {@code src/main/resources/} in the p04-alarm module). All values are validated
 * at construction time; a missing or malformed file throws
 * {@link ua.crowpi.core.exception.ConfigException} (unchecked).</p>
 *
 * <p>Example {@code alarm.properties}:</p>
 * <pre>
 *   pin.hash=03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4
 *   lockout.duration.seconds=30
 *   log.file=logs/alarm.log
 * </pre>
 */
public class AlarmConfig {

    /** SHA-256 hex digest of the correct PIN code. */
    private final String pinHash;

    /** Duration in seconds for which the keypad is locked after three failed attempts. */
    private final int lockoutDurationSeconds;

    /** Path to the rolling log file relative to the working directory. */
    private final String logFile;

    /**
     * Loads alarm configuration from the {@code alarm.properties} classpath resource.
     *
     * @throws ua.crowpi.core.exception.ConfigException if the file is missing or any
     *         required property is absent or invalid
     */
    public AlarmConfig() {
        // Завантажуємо properties через утиліту ядра, яка кидає ConfigException при помилці
        Properties props = PropertiesLoader.load("alarm.properties");

        // Зчитуємо обов'язкові поля — метод getString() сам перевіряє відсутність і порожнечу
        this.pinHash = PropertiesLoader.getString(props, "pin.hash");
        this.lockoutDurationSeconds = PropertiesLoader.getInt(props, "lockout.duration.seconds");
        this.logFile = PropertiesLoader.getString(props, "log.file");
    }

    /**
     * Returns the SHA-256 hex digest of the correct PIN code.
     *
     * @return 64-character lowercase hex string
     */
    public String getPinHash() {
        return pinHash;
    }

    /**
     * Returns the lockout duration in seconds after three consecutive wrong PIN attempts.
     *
     * @return lockout duration in seconds; typically 30
     */
    public int getLockoutDurationSeconds() {
        return lockoutDurationSeconds;
    }

    /**
     * Returns the path to the alarm log file.
     *
     * <p>The path is relative to the working directory from which the JVM was launched.</p>
     *
     * @return log file path string, e.g. {@code "logs/alarm.log"}
     */
    public String getLogFile() {
        return logFile;
    }
}
