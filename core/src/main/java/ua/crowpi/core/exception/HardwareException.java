package ua.crowpi.core.exception;

/**
 * Thrown when a hardware operation fails (GPIO, I2C, SPI, PWM).
 *
 * <p>This is a checked exception so callers must either handle it or declare it,
 * forcing conscious error handling at every hardware boundary — important in
 * embedded code where silent failures can damage equipment.</p>
 */
public class HardwareException extends Exception {

    /** Serial version for stable serialisation across JVM runs. */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a HardwareException with a descriptive message.
     *
     * @param message human-readable description of the failure
     */
    public HardwareException(String message) {
        super(message);
    }

    /**
     * Constructs a HardwareException wrapping a lower-level cause.
     *
     * @param message human-readable description of the failure
     * @param cause   the original exception from Pi4J or OS
     */
    public HardwareException(String message, Throwable cause) {
        super(message, cause);
    }
}
