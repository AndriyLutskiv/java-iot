package ua.crowpi.core.exception;

/**
 * Thrown when a configuration file is missing, malformed, or contains invalid values.
 *
 * <p>Implemented as an unchecked (runtime) exception because a bad configuration is
 * typically a programming or deployment error that cannot be recovered at runtime —
 * it must be fixed before the application is started again.</p>
 */
public class ConfigException extends RuntimeException {

    /** Serial version for stable serialisation across JVM runs. */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a ConfigException with a descriptive message.
     *
     * @param message human-readable description of the configuration problem
     */
    public ConfigException(String message) {
        super(message);
    }

    /**
     * Constructs a ConfigException wrapping a lower-level cause (e.g. IOException).
     *
     * @param message human-readable description of the configuration problem
     * @param cause   the original IO or parse exception
     */
    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
