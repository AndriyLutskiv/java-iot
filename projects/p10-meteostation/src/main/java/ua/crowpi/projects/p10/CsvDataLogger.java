package ua.crowpi.projects.p10;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Appends weather readings to a CSV file using Apache Commons CSV.
 *
 * <p>On the first call to {@link #log(WeatherReading, double, WeatherForecast)}, the file
 * is created and a header row is written.  Subsequent calls append data rows.
 * If the file already exists (e.g. after a restart), new rows are appended without
 * rewriting the header.</p>
 *
 * <p>CSV columns: {@code timestamp,temp_c,humidity_pct,heat_index,noisy,wind,forecast}</p>
 */
public class CsvDataLogger {

    private final String filePath;
    private boolean headerWritten;

    /**
     * Creates a CsvDataLogger that writes to the given file path.
     *
     * <p>The parent directories are created automatically on the first write.</p>
     *
     * @param filePath path of the target CSV file (e.g. {@code "logs/weather.csv"})
     */
    public CsvDataLogger(String filePath) {
        this.filePath = filePath;
        // Перевіряємо чи файл вже існує — якщо так, не пишемо header повторно
        this.headerWritten = Files.exists(Paths.get(filePath));
    }

    /**
     * Appends a single row for the given reading to the CSV file.
     *
     * <p>Creates parent directories and the file if they do not exist.
     * Writes the header row on the first call (unless the file already existed).</p>
     *
     * @param reading   the weather reading to log
     * @param heatIndex calculated heat index in °C
     * @param forecast  current trend forecast
     * @throws IOException if the file cannot be written
     */
    public void log(WeatherReading reading, double heatIndex, WeatherForecast forecast)
            throws IOException {
        // Створюємо батьківські директорії якщо вони відсутні
        Files.createDirectories(Paths.get(filePath).getParent() != null
                ? Paths.get(filePath).getParent()
                : Paths.get("."));

        // Відкриваємо у режимі append=true — не перезаписуємо попередні дані
        try (CSVPrinter printer = new CSVPrinter(
                new FileWriter(filePath, StandardCharsets.UTF_8, true),
                headerWritten ? CSVFormat.DEFAULT : CSVFormat.DEFAULT.withHeader(
                        "timestamp", "temp_c", "humidity_pct", "heat_index",
                        "noisy", "wind", "forecast"))) {

            if (!headerWritten) {
                // Перший запис у файл — header вже вставлено CSVFormat.withHeader()
                headerWritten = true;
            }

            printer.printRecord(
                    reading.getTimestamp(),
                    String.format("%.1f", reading.getTempC()),
                    String.format("%.1f", reading.getHumidity()),
                    String.format("%.1f", heatIndex),
                    reading.isNoisy(),
                    reading.isWindDetected(),
                    forecast.getLabel()
            );
        }
    }
}
