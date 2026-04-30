package ua.crowpi.projects.p02;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ua.crowpi.core.mock.MockI2cFacade;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the p02-counter module.
 *
 * <p>Tests cover:</p>
 * <ul>
 *   <li>7-segment display — all digits 0-9 rendered without exceptions</li>
 *   <li>7-segment display — specific segment pattern for digit 5</li>
 *   <li>{@link CounterMode#toggle()} — bidirectional toggle</li>
 *   <li>{@link EventRecord#toLogLine()} — exact log line format</li>
 *   <li>{@link EventFileLogger} — file is created and content is correct</li>
 *   <li>{@link CounterProject} — identity methods return expected values</li>
 * </ul>
 */
class CounterTest {

    // -------------------------------------------------------------------------
    // SevenSegmentDisplay tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link SevenSegmentDisplay#showDigit(int)} accepts all valid
     * digit values (0–9) without throwing any exception.
     *
     * <p>Uses a {@link MockI2cFacade} so no real hardware is required.</p>
     */
    @Test
    void testSevenSegmentMapping_allDigits() {
        // MockGpioFacade дозволяє викликати setOutput без реального GPIO
        SevenSegmentDisplay display = new SevenSegmentDisplay(new MockI2cFacade());

        // Перевіряємо, що жодна цифра не викидає виключення
        for (int digit = 0; digit <= 9; digit++) {
            final int d = digit;
            assertDoesNotThrow(
                    () -> display.showDigit(d),
                    "showDigit(" + digit + ") must not throw"
            );
        }
    }

    /**
     * Verifies that {@link SevenSegmentDisplay#showDigit(int)} throws
     * {@link IllegalArgumentException} for values outside [0, 9].
     */
    @Test
    void testSevenSegmentDisplay_invalidDigit_throwsException() {
        SevenSegmentDisplay display = new SevenSegmentDisplay(new MockI2cFacade());

        // Числа поза діапазоном 0-9 є програмною помилкою
        assertThrows(IllegalArgumentException.class, () -> display.showDigit(-1));
        assertThrows(IllegalArgumentException.class, () -> display.showDigit(10));
    }

    /**
     * Verifies the exact segment encoding for digit {@code 5}.
     *
     * <p>Expected pattern: A=1, B=0, C=1, D=1, E=0, F=1, G=1.
     * This matches the standard 7-segment encoding where digit 5 uses
     * segments A, C, D, F, G.</p>
     */
    @Test
    void testSevenSegmentDisplay_digit5_correctSegments() {
        // Сегментна маска для цифри 5: {A=1, B=0, C=1, D=1, E=0, F=1, G=1}
        int[] expected = {1, 0, 1, 1, 0, 1, 1};
        int[] actual   = SevenSegmentDisplay.SEGMENTS[5];

        // Перевіряємо кожен із 7 сегментів окремо для чіткого повідомлення про помилку
        String[] segNames = {"A", "B", "C", "D", "E", "F", "G"};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(
                    expected[i],
                    actual[i],
                    "Segment " + segNames[i] + " for digit 5 is incorrect"
            );
        }
    }

    /**
     * Verifies the segment encoding for digit {@code 0}.
     *
     * <p>Digit 0 illuminates all segments except G (the middle bar):
     * A=1, B=1, C=1, D=1, E=1, F=1, G=0.</p>
     */
    @Test
    void testSevenSegmentDisplay_digit0_correctSegments() {
        // Цифра 0: всі сегменти крім G (середня горизонтальна риска)
        int[] expected = {1, 1, 1, 1, 1, 1, 0};
        int[] actual   = SevenSegmentDisplay.SEGMENTS[0];

        assertEquals(7, actual.length, "Each digit must have exactly 7 segment entries");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], "Segment index " + i + " for digit 0");
        }
    }

    // -------------------------------------------------------------------------
    // CounterMode tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link CounterMode#toggle()} correctly switches between
     * both modes in both directions.
     */
    @Test
    void testCounterMode_toggle() {
        // COUNT_UP → COUNTDOWN
        assertEquals(
                CounterMode.COUNTDOWN,
                CounterMode.COUNT_UP.toggle(),
                "COUNT_UP.toggle() must return COUNTDOWN"
        );

        // COUNTDOWN → COUNT_UP
        assertEquals(
                CounterMode.COUNT_UP,
                CounterMode.COUNTDOWN.toggle(),
                "COUNTDOWN.toggle() must return COUNT_UP"
        );
    }

    /**
     * Verifies that double-toggling returns the original mode (idempotent pair).
     */
    @Test
    void testCounterMode_doubleToogle_returnsOriginal() {
        // Подвійне перемикання завжди має повертати початковий режим
        assertEquals(CounterMode.COUNT_UP,   CounterMode.COUNT_UP.toggle().toggle());
        assertEquals(CounterMode.COUNTDOWN,  CounterMode.COUNTDOWN.toggle().toggle());
    }

    /**
     * Verifies that {@link CounterMode#getLabel()} returns a non-null, non-empty string
     * for each mode constant.
     */
    @Test
    void testCounterMode_getLabel_notBlank() {
        for (CounterMode m : CounterMode.values()) {
            assertNotNull(m.getLabel(), "getLabel() must not return null");
            assertTrue(!m.getLabel().isEmpty(), "getLabel() must not return an empty string");
        }
    }

    // -------------------------------------------------------------------------
    // EventRecord tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link EventRecord#toLogLine()} produces the expected
     * pipe-separated format.
     *
     * <p>Expected: {@code "2024-01-15T14:23:11 | COUNT_UP | value=7"}</p>
     */
    @Test
    void testEventRecord_toLogLine() {
        // Фіксований час для детермінованої перевірки формату
        LocalDateTime time  = LocalDateTime.of(2024, 1, 15, 14, 23, 11);
        EventRecord record  = new EventRecord(time, CounterMode.COUNT_UP, 7);

        String expected = "2024-01-15T14:23:11 | COUNT_UP | value=7";
        assertEquals(expected, record.toLogLine(), "toLogLine() format must match spec");
    }

    /**
     * Verifies {@link EventRecord#toLogLine()} for the COUNTDOWN mode.
     */
    @Test
    void testEventRecord_toLogLine_countdownMode() {
        LocalDateTime time = LocalDateTime.of(2025, 6, 3, 9, 5, 0);
        EventRecord record = new EventRecord(time, CounterMode.COUNTDOWN, 3);

        // Формат залишається незмінним — лише назва режиму відрізняється
        assertEquals("2025-06-03T09:05:00 | COUNTDOWN | value=3", record.toLogLine());
    }

    /**
     * Verifies that all getters of {@link EventRecord} return the values
     * supplied to the constructor.
     */
    @Test
    void testEventRecord_getters_returnConstructorValues() {
        LocalDateTime time = LocalDateTime.of(2024, 12, 31, 23, 59, 59);
        CounterMode   mode  = CounterMode.COUNTDOWN;
        int           value = 9;

        EventRecord record = new EventRecord(time, mode, value);

        assertEquals(time,  record.getTime(),  "getTime() must return constructor arg");
        assertEquals(mode,  record.getMode(),  "getMode() must return constructor arg");
        assertEquals(value, record.getValue(), "getValue() must return constructor arg");
    }

    // -------------------------------------------------------------------------
    // EventFileLogger tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link EventFileLogger#append(EventRecord)} creates the log
     * file and writes the correct content.
     *
     * @param tempDir JUnit-provided temporary directory, cleaned up after the test
     */
    @Test
    void testEventFileLogger_createsFileAndWritesLine(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("events.log");
        EventFileLogger logger = new EventFileLogger(logFile.toString());

        LocalDateTime time   = LocalDateTime.of(2024, 3, 20, 10, 0, 0);
        EventRecord   record = new EventRecord(time, CounterMode.COUNT_UP, 4);

        logger.append(record);

        // Файл має бути створений після першого запису
        assertTrue(Files.exists(logFile), "Log file must be created after first append");

        String content = Files.readString(logFile).trim();
        assertEquals("2024-03-20T10:00:00 | COUNT_UP | value=4", content);
    }

    /**
     * Verifies that subsequent calls to {@link EventFileLogger#append(EventRecord)}
     * accumulate lines rather than overwriting.
     *
     * @param tempDir JUnit-provided temporary directory
     */
    @Test
    void testEventFileLogger_appendsMultipleLines(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("test_events.log");
        EventFileLogger logger = new EventFileLogger(logFile.toString());

        // Два послідовні записи мають стати двома рядками у файлі
        logger.append(new EventRecord(
                LocalDateTime.of(2024, 1, 1, 0, 0, 0), CounterMode.COUNT_UP, 1));
        logger.append(new EventRecord(
                LocalDateTime.of(2024, 1, 1, 0, 0, 5), CounterMode.COUNT_UP, 2));

        java.util.List<String> lines = Files.readAllLines(logFile);
        assertEquals(2, lines.size(), "Two appends must produce two log lines");
        assertTrue(lines.get(0).contains("value=1"), "First line must contain value=1");
        assertTrue(lines.get(1).contains("value=2"), "Second line must contain value=2");
    }

    // -------------------------------------------------------------------------
    // CounterProject identity tests
    // -------------------------------------------------------------------------

    /**
     * Verifies the identity methods of {@link CounterProject}.
     */
    @Test
    void testCounterProject_identity() {
        CounterProject project = new CounterProject();

        assertEquals("p02",                    project.getProjectId(), "project ID must be p02");
        assertEquals("Motion Event Counter",   project.getName(),      "name must match spec");
        assertNotNull(project.getDescription(), "description must not be null");
        assertTrue(!project.getDescription().isEmpty(), "description must not be empty");
    }
}
