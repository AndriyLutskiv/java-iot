package ua.crowpi.projects.p02;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Appends {@link EventRecord} entries to a plain-text log file.
 *
 * <p>Each call to {@link #append(EventRecord)} writes exactly one line to the
 * target file, creating the file (and any missing parent directories) if needed.
 * Existing content is preserved — only new lines are appended.</p>
 *
 * <p>The log file path is supplied at construction time so it can be redirected
 * during tests without touching the production path.</p>
 *
 * <p>Example log line written by this class:</p>
 * <pre>
 *   2024-01-15T14:23:11 | COUNT_UP | value=7
 * </pre>
 */
public class EventFileLogger {

    // Шлях до файлу зберігаємо як java.nio.file.Path для зручності роботи з Files API
    private final Path filePath;

    /**
     * Creates a logger that will write records to the specified file path.
     *
     * <p>If the parent directory does not exist it will be created on the first
     * call to {@link #append(EventRecord)}.</p>
     *
     * @param filePath absolute or relative path to the target log file;
     *                 must not be {@code null} or empty
     */
    public EventFileLogger(String filePath) {
        // Перетворюємо рядок на Path один раз у конструкторі — далі використовуємо тільки Path
        this.filePath = Paths.get(filePath);
    }

    /**
     * Appends one formatted log line to the target file.
     *
     * <p>The line is written as {@link EventRecord#toLogLine()} followed by a
     * system-line separator. The file is opened in append mode so concurrent
     * writes from multiple sessions accumulate correctly.</p>
     *
     * <p>If the parent directory does not exist, it is created automatically
     * before the first write to avoid {@link java.nio.file.NoSuchFileException}.</p>
     *
     * @param record the event to persist; must not be {@code null}
     * @throws IOException if the file cannot be created or the write fails
     */
    public void append(EventRecord record) throws IOException {
        // Створюємо батьківську директорію, якщо її не існує (наприклад, logs/)
        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        // Формуємо рядок з символом нового рядка — кожен запис на окремому рядку
        String line = record.toLogLine() + System.lineSeparator();

        // StandardOpenOption.APPEND — дописуємо в кінець, не перезаписуємо існуючі дані
        // StandardOpenOption.CREATE — автоматично створюємо файл, якщо він ще не існує
        Files.write(
                filePath,
                line.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.APPEND,
                StandardOpenOption.CREATE
        );
    }
}
