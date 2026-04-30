package ua.crowpi.projects.p07;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Appends greenhouse sensor readings and actuator states to a CSV log file
 * using Apache Commons CSV.
 *
 * <p>The CSV file uses the following column format:</p>
 * <pre>
 *   timestamp,temp_c,humidity_pct,soil_pct,pump_state,fan_state
 *   2026-03-20T14:35:00,24.1,55.0,45,OFF,ON
 * </pre>
 *
 * <p>The header row is written exactly once — on the first call to
 * {@link #log(GreenhouseReading, ActuatorState)} if the file is new or empty.
 * Subsequent calls append data rows only.</p>
 *
 * <p>The log directory is created automatically if it does not exist.</p>
 */
public class CsvDataLogger {

    /** CSV column headers matching the spec. */
    private static final String[] HEADERS = {
            "timestamp", "temp_c", "humidity_pct", "soil_pct", "pump_state", "fan_state"
    };

    /** Absolute or relative path to the CSV output file. */
    private final String filePath;

    // Прапорець — чи вже записаний заголовок у файл (перевіряємо один раз при першому логу)
    private boolean headerWritten = false;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a CsvDataLogger that writes to the given file path.
     *
     * <p>The file and any parent directories are created on the first call to
     * {@link #log(GreenhouseReading, ActuatorState)} if they do not already exist.</p>
     *
     * @param filePath path to the CSV file (relative or absolute)
     */
    public CsvDataLogger(String filePath) {
        this.filePath = filePath;
    }

    // -------------------------------------------------------------------------
    // Logging
    // -------------------------------------------------------------------------

    /**
     * Appends one data row to the CSV log file.
     *
     * <p>If the file does not yet exist or is empty, the header row is written
     * first. All text is written in UTF-8 encoding. The file is opened in append
     * mode so existing data is never overwritten.</p>
     *
     * @param reading the sensor snapshot to log (non-null)
     * @param state   the actuator state active at the time of the reading (non-null)
     * @throws IOException if the file cannot be opened, created, or written
     */
    public void log(GreenhouseReading reading, ActuatorState state) throws IOException {
        // Переконуємось що директорія існує (наприклад logs/) — створюємо якщо ні
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Перевіряємо чи потрібно писати заголовок:
        // заголовок потрібен якщо файл новий або порожній і ще не записаний у цій сесії
        boolean needHeader = !headerWritten && (!file.exists() || file.length() == 0);

        // Відкриваємо файл у режимі append=true щоб не затерти попередні дані
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(file, StandardCharsets.UTF_8, true))) {

            // Використовуємо CSVFormat.DEFAULT з назвами колонок для сумісності з Excel/LibreOffice
            CSVFormat format = CSVFormat.DEFAULT.withHeader(HEADERS);

            // Якщо файл новий — CSVPrinter з форматом що містить header автоматично його запише
            // Якщо файл існує — створюємо printer без автоматичного header і пишемо тільки рядок
            if (needHeader) {
                // Перший запис у файл — CSVFormat з заголовком запише header рядок
                try (CSVPrinter printer = new CSVPrinter(writer, format)) {
                    printer.printRecord(
                            reading.getTimestamp(),
                            String.format("%.1f", reading.getTempC()),
                            String.format("%.1f", reading.getHumidity()),
                            reading.getSoilPercent(),
                            state.isPumpOn() ? "ON" : "OFF",
                            state.isFanOn()  ? "ON" : "OFF"
                    );
                    printer.flush();
                }
                headerWritten = true;
            } else {
                // Файл вже має заголовок — пишемо тільки рядок даних без header
                try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
                    printer.printRecord(
                            reading.getTimestamp(),
                            String.format("%.1f", reading.getTempC()),
                            String.format("%.1f", reading.getHumidity()),
                            reading.getSoilPercent(),
                            state.isPumpOn() ? "ON" : "OFF",
                            state.isFanOn()  ? "ON" : "OFF"
                    );
                    printer.flush();
                }
                // Якщо headerWritten ще false (файл існував до запуску програми) — позначаємо
                headerWritten = true;
            }
        }

        System.out.printf("[CSV] Logged: %s%n", reading.getTimestamp());
    }
}
