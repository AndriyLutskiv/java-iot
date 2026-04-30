package ua.crowpi.projects.p04;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Structured logger wrapper for the alarm system.
 *
 * <p>Provides three domain-specific logging methods that produce consistent,
 * machine-parseable log lines. The underlying {@link Logger} is obtained from
 * Log4j 2 via {@link LogManager#getLogger(String)}, so the appender configuration
 * (console, rolling file) is controlled entirely by the {@code log4j2.xml}
 * resource in the classpath.</p>
 *
 * <p>All log messages are in English to match the log4j2 standard practice and
 * to ensure they are readable in CI environments that may not support non-ASCII.</p>
 */
public class AlarmLogger {

    // Делегуємо до стандартного Log4j2 логера — конфігурація в log4j2.xml
    private final Logger logger;

    /**
     * Creates an AlarmLogger backed by a Log4j 2 logger with the given name.
     *
     * @param loggerName the logger name; typically the fully-qualified class name
     *                   of the component that owns this logger
     */
    public AlarmLogger(String loggerName) {
        // Отримуємо іменований логер — це дозволяє задавати різні рівні для різних компонентів
        this.logger = LogManager.getLogger(loggerName);
    }

    /**
     * Logs an FSM state transition at INFO level.
     *
     * <p>Format: {@code FSM transition: <FROM> --[<EVENT>]--> <TO>}</p>
     *
     * @param from  the state before the transition
     * @param event the event that triggered the transition
     * @param to    the state after the transition
     */
    public void logEvent(AlarmState from, AlarmEvent event, AlarmState to) {
        // Структурований рядок дозволяє легко grep-нути переходи з файлу логу
        logger.info("FSM transition: {} --[{}]--> {}", from, event, to);
    }

    /**
     * Logs a PIN entry attempt at INFO or WARN level depending on success.
     *
     * <p>On success: {@code PIN attempt: SUCCESS (failCount reset to 0)}<br>
     * On failure: {@code PIN attempt: FAILED (failCount=N)}</p>
     *
     * @param success   {@code true} if the correct PIN was entered
     * @param failCount current fail count after this attempt
     */
    public void logPinAttempt(boolean success, int failCount) {
        if (success) {
            // Успішна аутентифікація — логуємо як інформацію
            logger.info("PIN attempt: SUCCESS (failCount reset to 0)");
        } else {
            // Невірний PIN — логуємо як попередження, бо це потенційна атака
            logger.warn("PIN attempt: FAILED (failCount={})", failCount);
        }
    }

    /**
     * Logs an error condition at ERROR level with the causing exception.
     *
     * @param message a human-readable description of what went wrong
     * @param cause   the exception that caused the error; may be {@code null}
     */
    public void logError(String message, Throwable cause) {
        // Передаємо Throwable другим аргументом — Log4j2 автоматично виводить stack trace
        logger.error(message, cause);
    }
}
