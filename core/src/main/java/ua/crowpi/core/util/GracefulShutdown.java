package ua.crowpi.core.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.CrowPiProject;

/**
 * Registers a JVM shutdown hook that calls {@link CrowPiProject#shutdown()} when the
 * application exits normally or receives SIGTERM / SIGINT (Ctrl+C).
 *
 * <p>On Raspberry Pi, releasing GPIO pins on exit is critical: if a pin is left HIGH
 * when the JVM exits, the connected component (LED, relay, buzzer) stays active.
 * The shutdown hook guarantees cleanup even when the user presses Ctrl+C.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 *   CrowPiProject project = registry.findById("rfid").get();
 *   GracefulShutdown.register(project);
 *   project.run(mockMode);
 *   // If Ctrl+C is pressed while run() is blocked → shutdown() is called automatically
 * }</pre>
 */
public final class GracefulShutdown {

    private static final Logger LOG = LogManager.getLogger(GracefulShutdown.class);

    // Утилітний клас — конструктор приватний, щоб не можна було створити екземпляр
    private GracefulShutdown() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Registers a JVM shutdown hook for the given project.
     *
     * <p>The hook runs in a dedicated thread named {@code "shutdown-<projectId>"} so
     * Log4j can attribute any shutdown log messages to the correct project.</p>
     *
     * <p>If {@link CrowPiProject#shutdown()} throws an unchecked exception the hook
     * logs the error but does not re-throw it, because exceptions thrown from shutdown
     * hooks are silently swallowed by the JVM anyway.</p>
     *
     * @param project the project whose {@code shutdown()} will be called on JVM exit
     */
    public static void register(CrowPiProject project) {
        String hookName = "shutdown-" + project.getProjectId();

        Thread hook = new Thread(() -> {
            LOG.info("Shutdown hook fired for project '{}' — releasing resources",
                    project.getProjectId());
            try {
                project.shutdown();
                LOG.info("Project '{}' shutdown completed", project.getProjectId());
            } catch (Exception e) {
                // Логуємо помилку, але не кидаємо — JVM все одно завершиться
                LOG.error("Error during shutdown of project '{}': {}",
                        project.getProjectId(), e.getMessage(), e);
            }
        }, hookName);

        // setDaemon(false) — shutdown hook повинен бути не-daemon, щоб JVM дочекалась
        // його завершення перед виходом (за замовчуванням Thread вже не-daemon,
        // але явно вказуємо для ясності)
        hook.setDaemon(false);

        Runtime.getRuntime().addShutdownHook(hook);
        LOG.debug("Registered shutdown hook '{}' for project '{}'",
                hookName, project.getProjectId());
    }

    /**
     * Registers shutdown hooks for multiple projects at once.
     *
     * <p>Convenience overload — delegates to {@link #register(CrowPiProject)}
     * for each element in the iterable.</p>
     *
     * @param projects the projects to register
     */
    public static void registerAll(Iterable<CrowPiProject> projects) {
        for (CrowPiProject project : projects) {
            register(project);
        }
    }
}
