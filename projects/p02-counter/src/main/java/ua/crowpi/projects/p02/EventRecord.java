package ua.crowpi.projects.p02;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Immutable snapshot of a single PIR detection event.
 *
 * <p>Each time the PIR sensor fires, the project creates an {@code EventRecord}
 * capturing the timestamp, the current {@link CounterMode}, and the resulting
 * counter value. The record can then be formatted as a log line and persisted
 * by {@link EventFileLogger}.</p>
 */
public class EventRecord {

    // Форматер без мілісекунд — відповідає вимозі: "2024-01-15T14:23:11"
    private static final DateTimeFormatter LOG_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final LocalDateTime time;
    private final CounterMode mode;
    private final int value;

    /**
     * Creates a new event record with the given context.
     *
     * @param time  the exact timestamp of the PIR detection event
     * @param mode  the counter mode that was active when the event fired
     * @param value the counter value <em>after</em> applying the event
     */
    public EventRecord(LocalDateTime time, CounterMode mode, int value) {
        // Всі поля фінальні — запис незмінний після створення (immutable POJO)
        this.time = time;
        this.mode = mode;
        this.value = value;
    }

    /**
     * Returns the timestamp at which the detection event occurred.
     *
     * @return event timestamp; never {@code null}
     */
    public LocalDateTime getTime() {
        return time;
    }

    /**
     * Returns the counter mode that was active at the time of the event.
     *
     * @return active {@link CounterMode}; never {@code null}
     */
    public CounterMode getMode() {
        return mode;
    }

    /**
     * Returns the counter value recorded for this event.
     *
     * @return counter value in the range [0..9] (wraps modulo 10)
     */
    public int getValue() {
        return value;
    }

    /**
     * Formats the record as a pipe-separated log line.
     *
     * <p>Example output: {@code 2024-01-15T14:23:11 | COUNT_UP | value=7}</p>
     *
     * @return formatted log line string; never {@code null}
     */
    public String toLogLine() {
        // Формат: "timestamp | MODE_NAME | value=N" — узгоджено зі специфікацією проекту
        return String.format("%s | %s | value=%d",
                time.format(LOG_FORMATTER),
                mode.name(),
                value);
    }
}
