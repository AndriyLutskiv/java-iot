package ua.crowpi.projects.p05;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Exports a completed {@link ScanResult} to a JSON file using manual string building.
 *
 * <p>No external JSON library is used — the output is built with a {@link StringBuilder}
 * to keep this module's dependencies minimal and to demonstrate manual serialisation
 * as an educational exercise.</p>
 *
 * <p>Output JSON structure:</p>
 * <pre>{@code
 * {
 *   "sessionId": "20240115_142311",
 *   "scanCount": 73,
 *   "scans": [
 *     {"angle": 0, "distance": 32.4, "timestamp": "20240115_142311"},
 *     ...
 *   ]
 * }
 * }</pre>
 *
 * <p>Files are written using UTF-8 encoding via {@link Files#write}.</p>
 */
public class RadarDataExporter {

    /**
     * Serialises the given {@link ScanResult} to a JSON string and writes it
     * to the specified file path, creating or overwriting the file as needed.
     *
     * @param result   the scan session to export; must not be {@code null}
     * @param filePath absolute or relative path of the output JSON file
     * @throws IOException if the file cannot be created or written
     */
    public void exportToFile(ScanResult result, String filePath) throws IOException {
        String json = toJson(result);
        // Записуємо у файл у кодуванні UTF-8 — стандарт для JSON за RFC 8259
        Files.write(Paths.get(filePath), json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Converts a {@link ScanResult} into a JSON string representation.
     *
     * <p>Uses {@link StringBuilder} for efficient string construction and avoids
     * any third-party JSON library. Each double value is formatted to two decimal
     * places for compact but human-readable output.</p>
     *
     * @param result the scan session to serialise; must not be {@code null}
     * @return a valid JSON string containing {@code sessionId}, {@code scanCount}
     *         and {@code scans} array
     */
    public String toJson(ScanResult result) {
        StringBuilder sb = new StringBuilder();

        // Відкриваємо кореневий об'єкт JSON
        sb.append("{");

        // Поле sessionId — ідентифікатор сесії сканування
        sb.append("\"sessionId\":\"").append(escapeJson(result.getSessionId())).append("\",");

        // Поле scanCount — кількість вимірів у сесії (зручно для швидкої перевірки)
        sb.append("\"scanCount\":").append(result.getScanCount()).append(",");

        // Масив scans — основні дані вимірів
        sb.append("\"scans\":[");

        List<RadarScan> scans = result.getScans();
        for (int i = 0; i < scans.size(); i++) {
            RadarScan scan = scans.get(i);

            // Кожен елемент масиву — JSON-об'єкт із кутом, відстанню та часовою міткою
            sb.append("{");
            sb.append("\"angle\":").append(scan.getAngleDeg()).append(",");
            // Дистанція з двома знаками після коми для балансу точності і розміру файлу
            sb.append("\"distance\":").append(String.format("%.2f", scan.getDistanceCm())).append(",");
            sb.append("\"timestamp\":\"").append(escapeJson(scan.getTimestamp())).append("\"");
            sb.append("}");

            // Кома між елементами масиву, але не після останнього
            if (i < scans.size() - 1) {
                sb.append(",");
            }
        }

        // Закриваємо масив і кореневий об'єкт
        sb.append("]");
        sb.append("}");

        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Внутрішній метод екранування рядків
    // -------------------------------------------------------------------------

    /**
     * Escapes special characters in a string value for safe embedding in JSON.
     *
     * <p>Handles the minimum set of characters required by the JSON specification
     * (RFC 8259 section 7): backslash, double-quote, and the ASCII control characters
     * newline, carriage-return, and tab.</p>
     *
     * @param input the raw string to escape; may be {@code null}
     * @return the escaped string safe for inclusion between JSON double quotes,
     *         or an empty string if {@code input} is {@code null}
     */
    private static String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        // Замінюємо спецсимволи відповідно до специфікації JSON RFC 8259
        return input
                .replace("\\", "\\\\")   // зворотний слеш має бути першим
                .replace("\"", "\\\"")   // подвійні лапки потрібно екранувати
                .replace("\n", "\\n")    // символ нового рядка
                .replace("\r", "\\r")    // символ повернення каретки
                .replace("\t", "\\t");   // символ табуляції
    }
}
