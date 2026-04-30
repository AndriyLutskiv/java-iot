package ua.crowpi.projects.p08;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Structured logger for RFID access attempts.
 *
 * <p>Wraps a Log4j 2 {@link Logger} and provides two domain-specific logging methods
 * that produce consistent, machine-parseable log entries. The underlying appender
 * configuration (console / rolling file) is controlled by {@code log4j2.xml}
 * on the classpath.</p>
 *
 * <p>All log messages are in English for CI/log-aggregation compatibility.</p>
 */
public class AccessAttemptLogger {

    // Статичний логер — один на весь клас, конфігурується через log4j2.xml
    private static final Logger LOG = LogManager.getLogger(AccessAttemptLogger.class);

    // Формат відмітки часу для рядків логу — ISO-подібний, легко парситься grep/awk
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates an AccessAttemptLogger.
     * The underlying Log4j 2 logger is obtained at construction time.
     */
    public AccessAttemptLogger() {
        // Конструктор існує явно — дозволяє майбутнє розширення (наприклад, ін'єкція імені)
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Logs a single access attempt at INFO or WARN level depending on the result.
     *
     * <p>Log format:</p>
     * <pre>
     *   INFO  ACCESS ATTEMPT | uid=AA:BB:CC:DD | result=GRANTED     | owner=Student Ivanenko | ts=2026-03-20 12:00:00
     *   WARN  ACCESS ATTEMPT | uid=FF:FF:FF:FF | result=DENIED      | owner=Deactivated Card | ts=2026-03-20 12:00:05
     *   WARN  ACCESS ATTEMPT | uid=00:00:00:00 | result=UNKNOWN_CARD | owner=UNKNOWN         | ts=2026-03-20 12:00:10
     * </pre>
     *
     * @param uid       the UID that was read from the RFID reader
     * @param result    the {@link AccessResult} determined by {@link CardDatabase#check(String)}
     * @param ownerName the card owner's name, or {@code "UNKNOWN"} for unregistered UIDs
     */
    public void logAttempt(String uid, AccessResult result, String ownerName) {
        // Формуємо структурований рядок для зручного grep у prod-логах
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        String entry = String.format(
                "ACCESS ATTEMPT | uid=%-17s | result=%-15s | owner=%-20s | ts=%s",
                uid, result.name(), ownerName, timestamp);

        if (result == AccessResult.GRANTED) {
            // Успішний прохід — рівень INFO, не викликає alert у моніторингу
            LOG.info(entry);
        } else {
            // Відмова або невідома картка — рівень WARN, може запустити alert
            LOG.warn(entry);
        }
    }

    /**
     * Logs an error condition at ERROR level.
     *
     * <p>Use this for hardware failures, unexpected exceptions or configuration problems
     * that prevent normal operation of the RFID system.</p>
     *
     * @param message a human-readable description of the error condition
     */
    public void logError(String message) {
        // ERROR рівень — завжди видимий навіть при INFO-порозі логування
        LOG.error("RFID ERROR: {}", message);
    }
}
