package ua.crowpi.projects.p10;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates a self-contained HTML weather report from a list of {@link WeatherReading}s.
 *
 * <p>No external libraries are used — the entire HTML document is built using
 * {@link StringBuilder} and standard Java string operations. The generated file
 * ({@code weather_report.html}) can be opened in any browser directly on the Raspberry Pi
 * or transferred to a laptop for viewing.</p>
 *
 * <p>Report sections:</p>
 * <ol>
 *   <li>Header with title and generation timestamp.</li>
 *   <li>Current conditions table (latest reading + heat index + forecast).</li>
 *   <li>Trend summary with arrow and label.</li>
 *   <li>Reading history table (all readings in the buffer, newest last).</li>
 * </ol>
 */
public class HtmlReportGenerator {

    /** Date-time formatter used for the report generation timestamp. */
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Builds a complete HTML report document from the given data.
     *
     * <p>The method uses a {@link StringBuilder} to concatenate all HTML tags
     * and data rows without any external templating or serialisation library.</p>
     *
     * @param readings   full history of weather readings from the {@link RingBuffer};
     *                   the last element is treated as the most recent reading
     * @param forecast   current trend forecast computed by {@link TrendAnalyzer}
     *                   and classified by {@link WeatherForecast#fromSlope(double)}
     * @param heatIndex  apparent temperature computed by {@link HeatIndex#calculate}
     *                   for the most recent reading
     * @return complete, self-contained HTML document as a {@link String}
     */
    public String generate(List<WeatherReading> readings,
                           WeatherForecast forecast,
                           double heatIndex) {

        // Визначаємо найновіше читання (останній елемент буфера)
        WeatherReading current = readings.isEmpty() ? null : readings.get(readings.size() - 1);

        String generatedAt = LocalDateTime.now().format(TS_FMT);

        StringBuilder sb = new StringBuilder(4096);

        // ----------------------------------------------------------------
        // DOCTYPE + head
        // ----------------------------------------------------------------
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html lang=\"en\">\n");
        sb.append("<head>\n");
        sb.append("  <meta charset=\"UTF-8\">\n");
        sb.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("  <title>CrowPi Weather Station Report</title>\n");
        // Вбудований CSS — мінімальний, щоб звіт виглядав охайно навіть без мережі
        sb.append("  <style>\n");
        sb.append("    body { font-family: Arial, sans-serif; margin: 24px; color: #222; }\n");
        sb.append("    h1 { color: #1a5276; }\n");
        sb.append("    h2 { color: #1f618d; border-bottom: 1px solid #aed6f1; }\n");
        sb.append("    table { border-collapse: collapse; width: 100%; margin-bottom: 24px; }\n");
        sb.append("    th { background: #1f618d; color: #fff; padding: 8px 12px; text-align: left; }\n");
        sb.append("    td { padding: 6px 12px; border-bottom: 1px solid #d5d8dc; }\n");
        sb.append("    tr:nth-child(even) td { background: #eaf4fb; }\n");
        sb.append("    .forecast { font-size: 1.4em; font-weight: bold; color: #117a65; }\n");
        sb.append("    .footer { color: #888; font-size: 0.85em; margin-top: 32px; }\n");
        sb.append("  </style>\n");
        sb.append("</head>\n");

        // ----------------------------------------------------------------
        // body
        // ----------------------------------------------------------------
        sb.append("<body>\n");
        sb.append("  <h1>CrowPi Weather Station Report</h1>\n");
        sb.append("  <p>Generated: ").append(escapeHtml(generatedAt)).append("</p>\n");

        // ----------------------------------------------------------------
        // Current conditions
        // ----------------------------------------------------------------
        sb.append("  <h2>Current Conditions</h2>\n");
        sb.append("  <table>\n");
        sb.append("    <tr><th>Parameter</th><th>Value</th></tr>\n");

        if (current != null) {
            // Форматуємо числові значення з розумною точністю
            sb.append("    <tr><td>Timestamp</td><td>")
              .append(escapeHtml(current.getTimestamp())).append("</td></tr>\n");
            sb.append("    <tr><td>Temperature (&#176;C)</td><td>")
              .append(String.format("%.1f", current.getTempC())).append("</td></tr>\n");
            sb.append("    <tr><td>Humidity (%)</td><td>")
              .append(String.format("%.0f", current.getHumidity())).append("</td></tr>\n");
            sb.append("    <tr><td>Heat Index (&#176;C)</td><td>")
              .append(String.format("%.1f", heatIndex)).append("</td></tr>\n");
            sb.append("    <tr><td>Noise Detected</td><td>")
              .append(current.isNoisy() ? "Yes" : "No").append("</td></tr>\n");
            sb.append("    <tr><td>Wind / Tilt Detected</td><td>")
              .append(current.isWindDetected() ? "Yes" : "No").append("</td></tr>\n");
        } else {
            sb.append("    <tr><td colspan=\"2\">No readings available yet.</td></tr>\n");
        }

        sb.append("  </table>\n");

        // ----------------------------------------------------------------
        // Trend forecast
        // ----------------------------------------------------------------
        sb.append("  <h2>Trend Forecast</h2>\n");
        sb.append("  <p class=\"forecast\">");
        sb.append(escapeHtml(forecast.getArrow())).append(" ").append(escapeHtml(forecast.getLabel()));
        sb.append("</p>\n");
        sb.append("  <p>Temperature trend is <strong>")
          .append(escapeHtml(forecast.getLabel().toLowerCase()))
          .append("</strong> based on the last ").append(readings.size())
          .append(" readings.</p>\n");

        // ----------------------------------------------------------------
        // History table
        // ----------------------------------------------------------------
        sb.append("  <h2>Reading History (").append(readings.size()).append(" entries)</h2>\n");
        sb.append("  <table>\n");
        sb.append("    <tr>\n");
        sb.append("      <th>#</th>\n");
        sb.append("      <th>Timestamp</th>\n");
        sb.append("      <th>Temp (&#176;C)</th>\n");
        sb.append("      <th>Humidity (%)</th>\n");
        sb.append("      <th>Heat Index (&#176;C)</th>\n");
        sb.append("      <th>Noise</th>\n");
        sb.append("      <th>Wind</th>\n");
        sb.append("    </tr>\n");

        // Виводимо всі читання у порядку від найстарішого до найновішого
        for (int i = 0; i < readings.size(); i++) {
            WeatherReading r = readings.get(i);
            // Обчислюємо Heat Index для кожного читання в таблиці
            double hi = HeatIndex.calculate(r.getTempC(), r.getHumidity());
            sb.append("    <tr>\n");
            sb.append("      <td>").append(i + 1).append("</td>\n");
            sb.append("      <td>").append(escapeHtml(r.getTimestamp())).append("</td>\n");
            sb.append("      <td>").append(String.format("%.1f", r.getTempC())).append("</td>\n");
            sb.append("      <td>").append(String.format("%.0f", r.getHumidity())).append("</td>\n");
            sb.append("      <td>").append(String.format("%.1f", hi)).append("</td>\n");
            sb.append("      <td>").append(r.isNoisy() ? "Yes" : "No").append("</td>\n");
            sb.append("      <td>").append(r.isWindDetected() ? "Yes" : "No").append("</td>\n");
            sb.append("    </tr>\n");
        }

        sb.append("  </table>\n");

        // ----------------------------------------------------------------
        // Footer
        // ----------------------------------------------------------------
        sb.append("  <p class=\"footer\">CrowPi Educational Suite &mdash; p10 Weather Station</p>\n");
        sb.append("</body>\n");
        sb.append("</html>\n");

        return sb.toString();
    }

    /**
     * Writes the given HTML string to a file at the specified path using UTF-8 encoding.
     *
     * <p>The file is created if it does not exist; if it already exists it is overwritten.</p>
     *
     * @param html     complete HTML document string to write
     * @param filePath absolute or relative path to the output file
     * @throws IOException if the file cannot be created or written
     */
    public void writeToFile(String html, String filePath) throws IOException {
        // Використовуємо OutputStreamWriter + FileOutputStream для явного задання кодування UTF-8
        // (не покладаємось на системне кодування, яке може бути іншим на RPi)
        try (Writer writer = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            writer.write(html);
        }
    }

    // -------------------------------------------------------------------------
    // Приватні допоміжні методи
    // -------------------------------------------------------------------------

    /**
     * Escapes special HTML characters in the given string to prevent XSS-style injection
     * and broken markup in the generated report.
     *
     * @param text raw text value; may be {@code null}
     * @return HTML-safe string with &amp;, &lt;, &gt;, &quot; and &#39; escaped
     */
    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        // Виконуємо заміну в порядку: спочатку & щоб не подвоїти escape
        return text
                .replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&#39;");
    }
}
