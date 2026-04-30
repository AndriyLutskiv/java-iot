package ua.crowpi.core;

import ua.crowpi.core.exception.HardwareException;

/**
 * Contract that every CrowPi educational project must satisfy.
 *
 * <p>The {@link ua.crowpi.core.launcher.Launcher} discovers all implementations
 * via {@link java.util.ServiceLoader} and exposes them through the interactive menu.
 * Each implementation must be listed in
 * {@code META-INF/services/ua.crowpi.core.CrowPiProject} inside its own module.</p>
 *
 * <p>Lifecycle:</p>
 * <ol>
 *   <li>{@link #run(boolean)} — starts the project; blocks until the user exits.</li>
 *   <li>{@link #shutdown()} — called by the JVM shutdown hook (via
 *       {@link ua.crowpi.core.util.GracefulShutdown}) to release GPIO pins,
 *       flush buffers and stop threads cleanly.</li>
 * </ol>
 */
public interface CrowPiProject {

    /**
     * Returns the human-readable display name shown in the interactive menu.
     *
     * @return project display name, e.g. {@code "RFID Door Lock"}
     */
    String getName();

    /**
     * Returns the short identifier used with the {@code --project} CLI flag.
     *
     * @return lower-case kebab-case identifier, e.g. {@code "rfid"}
     */
    String getProjectId();

    /**
     * Returns a one-sentence description of what the project demonstrates.
     *
     * @return single-sentence project description
     */
    String getDescription();

    /**
     * Starts the project and blocks until it completes or the user interrupts.
     *
     * @param mockMode {@code true} to use mock hardware facades (no real GPIO);
     *                 {@code false} to use real Pi4J GPIO on Raspberry Pi
     * @throws HardwareException if a hardware initialisation or communication error occurs
     */
    void run(boolean mockMode) throws HardwareException;

    /**
     * Releases all resources held by this project (GPIO pins, threads, file handles).
     *
     * <p>Must be idempotent — calling it multiple times must not throw.</p>
     */
    void shutdown();
}
