package ua.crowpi.core.exception;

/**
 * Thrown when a database operation fails (SQLite read/write, schema creation, migration).
 *
 * <p>Kept as a separate type from {@link HardwareException} so callers can distinguish
 * persistence failures from physical hardware failures in catch blocks.</p>
 */
public class DatabaseException extends Exception {

    /** Serial version for stable serialisation across JVM runs. */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a DatabaseException with a descriptive message.
     *
     * @param message human-readable description of the failure
     */
    public DatabaseException(String message) {
        super(message);
    }

    /**
     * Constructs a DatabaseException wrapping a lower-level cause (e.g. SQLException).
     *
     * @param message human-readable description of the failure
     * @param cause   the original SQL or JDBC exception
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
