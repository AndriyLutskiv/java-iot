package ua.crowpi.projects.p01;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Appends {@link TemperatureReading} records to a CSV log file using
 * Apache Commons CSV.
 *
 * <p>On the first call to {@link #log} the parent directories are created
 * automatically and a header row is written. Subsequent calls append data rows
 * without re-writing the header, so the file grows incrementally across
 * multiple JVM runs.</p>
 *
 * <p>CSV column layout:</p>
 * <pre>
 *   timestamp,temperature,humidity,zone,threshold
 *   2024-03-15T14:23:01,23.4,61.0,COMFORT,28.0
 * </pre>
 *
 * <p>The file is opened in append mode on every {@link #log} call so there is no
 * persistent file handle to flush between scheduler ticks. This ensures data
 * reaches the filesystem even if the JVM is killed between writes.</p>
 */
public class CsvDataLogger {

    /** CSV header column names — must stay in sync with the {@link #log} write order. */
    private static final String[] HEADERS =
            {"timestamp", "temperature", "humidity", "zone", "threshold"};

    /** Absolute or relative path to the target CSV file. */
    private final String filePath;

    // Прапорець «заголовок уже записаний у цій сесії JVM» — запобігає дублюванню header
    // у довгій безперервній сесії. При наступному запуску JVM перевіряємо довжину файлу.
    private boolean headerWritten = false;

    /**
     * Creates a {@code CsvDataLogger} that writes to the given file path.
     *
     * <p>The file and parent directories are not created here; they are created
     * lazily on the first call to {@link #log}.</p>
     *
     * @param filePath path to the CSV log file, e.g. {@code "logs/thermometer.csv"}
     */
    public CsvDataLogger(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Appends one measurement row to the CSV file.
     *
     * <p>If the file does not yet exist or is empty (and no header was written in this
     * JVM session), the header row is prepended automatically. If the file already
     * contains data, the row is appended without a header.</p>
     *
     * @param reading   the sensor measurement to record
     * @param zone      the thermal zone determined for this reading
     * @param threshold the current alert threshold at the time of this reading
     * @throws IOException if the file cannot be created or written to
     */
    public void log(TemperatureReading reading, ThermalZone zone, double threshold)
            throws IOException {
        File file = new File(filePath);

        // Переконуємось що батьківська директорія (наприклад logs/) існує
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Заголовок потрібен якщо файл новий/порожній І ми ще не писали header в цій сесії
        boolean needHeader = !headerWritten && (!file.exists() || file.length() == 0);

        // Відкриваємо файл у режимі append=true — існуючі дані не перезаписуємо
        // Java 11 FileWriter(File, Charset, boolean) — безпечний для UTF-8 на всіх ОС
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(file, StandardCharsets.UTF_8, /* append= */ true))) {

            if (needHeader) {
                // Перший запис — CSVFormat з заголовком автоматично додасть header рядок
                try (CSVPrinter printer = new CSVPrinter(writer,
                        CSVFormat.DEFAULT.withHeader(HEADERS))) {
                    printer.printRecord(buildRecord(reading, zone, threshold));
                    printer.flush();
                }
                headerWritten = true;
            } else {
                // Файл вже має заголовок — дописуємо тільки рядок даних
                try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
                    printer.printRecord(buildRecord(reading, zone, threshold));
                    printer.flush();
                }
                // Позначаємо, що заголовок уже є (на випадок якщо файл існував до запуску)
                headerWritten = true;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Допоміжний метод
    // -------------------------------------------------------------------------

    /**
     * Builds the array of field values for one CSV record in the order that
     * matches the {@link #HEADERS} array.
     *
     * @param reading   sensor measurement
     * @param zone      thermal zone for this reading
     * @param threshold alert threshold at the time of this reading
     * @return ordered array of string values ready for {@link CSVPrinter#printRecord(Object...)}
     */
    private static Object[] buildRecord(
            TemperatureReading reading, ThermalZone zone, double threshold) {
        // Порядок полів відповідає заголовку: timestamp,temperature,humidity,zone,threshold
        return new Object[]{
                reading.getTimestamp(),
                String.format("%.1f", reading.getTemperatureC()),
                String.format("%.1f", reading.getHumidity()),
                zone.name(),
                String.format("%.1f", threshold)
        };
    }
}
