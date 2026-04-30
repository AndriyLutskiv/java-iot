package ua.crowpi.core.hardware;

/**
 * Callback invoked when a GPIO digital input pin changes state.
 *
 * <p>Implementations are called from a background GPIO-listener thread managed
 * by Pi4J or the mock facade. Implementations must be thread-safe and should
 * complete quickly to avoid blocking the listener thread.</p>
 *
 * <p>Annotated as {@link FunctionalInterface} so callers can supply a lambda:</p>
 * <pre>{@code
 *   gpio.addListener(PIR_PIN, (pin, high) -> counter.incrementAndGet());
 * }</pre>
 */
@FunctionalInterface
public interface PinChangeListener {

    /**
     * Called when the monitored GPIO pin changes state.
     *
     * @param pin  the BCM GPIO pin number that changed
     * @param high {@code true} if the pin went HIGH (3.3 V); {@code false} if LOW (0 V)
     */
    void onPinChange(int pin, boolean high);
}
