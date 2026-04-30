package ua.crowpi.projects.p05;

import java.util.List;

/**
 * Renders radar scan data to the system console in two formats:
 * a per-measurement CSV-style line and a full ASCII semicircle chart.
 *
 * <p>The per-line format is machine-parseable CSV designed for real-time monitoring:</p>
 * <pre>
 *   SCAN,045,32.4
 *   SCAN,090,80.0
 * </pre>
 *
 * <p>The ASCII semicircle visualises the last full sweep (up to 180° in 5° steps)
 * as a 20-character wide half-circle. Target positions are marked with {@code 'X'};
 * the sweep arc is drawn with {@code '-'} for empty positions. A scale legend and
 * zone summary are printed below the chart.</p>
 */
public class ConsoleRadarRenderer {

    /**
     * Width of the ASCII display in characters.
     * The semicircle radius scales to fit within this width.
     */
    private static final int DISPLAY_WIDTH = 20;

    /**
     * Radius of the ASCII semicircle in character columns.
     * Must be less than DISPLAY_WIDTH / 2.
     */
    private static final int RADIUS = 9;

    /**
     * Prints a single scan measurement in {@code SCAN,angle,distance} format.
     *
     * <p>Angle is zero-padded to three digits; distance is printed with one decimal
     * place. This format is compatible with simple CSV parsers and serial monitors.</p>
     *
     * @param scan the completed {@link RadarScan} to print; must not be {@code null}
     */
    public void printScanLine(RadarScan scan) {
        // Формат CSV: ключове слово SCAN, кут з нулями, відстань з одним знаком
        System.out.printf("SCAN,%03d,%.1f%n", scan.getAngleDeg(), scan.getDistanceCm());
    }

    /**
     * Renders an ASCII semicircle radar chart from a completed {@link ScanResult}.
     *
     * <p>The chart maps each scan's polar coordinates (angle, distance) onto a
     * 20×10-character grid. Targets closer than 50 cm are marked with {@code 'X'};
     * arc positions without detections are filled with {@code '-'}. After the
     * chart, a per-zone count summary is printed.</p>
     *
     * @param result the completed scan session to visualise; must not be {@code null}
     */
    public void render(ScanResult result) {
        System.out.println("\n=== RADAR SCAN — session: " + result.getSessionId() + " ===");

        // Побудова 2D символьного буфера для ASCII-відображення радару
        // Рядки — вісь Y (0 = верх дисплею), стовпці — вісь X
        int rows = RADIUS + 2;
        int cols = DISPLAY_WIDTH + 2;
        char[][] grid = new char[rows][cols];

        // Заповнюємо фон пробілами
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = ' ';
            }
        }

        // Центр напівкола — нижній середній рядок дисплею
        int cx = DISPLAY_WIDTH / 2;
        int cy = rows - 1;

        // Малюємо базову дугу напівкола (пусті позиції = '-')
        for (int angleDeg = 0; angleDeg <= 180; angleDeg += 5) {
            double rad = Math.toRadians(angleDeg);
            // Перетворюємо полярні координати у декартові для символьної сітки
            int x = cx + (int) Math.round(RADIUS * Math.cos(rad));
            // Y-вісь інвертована: 0 — верх дисплею, rows — низ
            int y = cy - (int) Math.round(RADIUS * Math.sin(rad));
            if (y >= 0 && y < rows && x >= 0 && x < cols) {
                grid[y][x] = '-';
            }
        }

        // Нанесення виявлених об'єктів на сітку
        List<RadarScan> scans = result.getScans();
        int dangerCount   = 0;
        int warningCount  = 0;
        int clearCount    = 0;

        for (RadarScan scan : scans) {
            DistanceZone zone = DistanceZone.forDistance(scan.getDistanceCm());

            // Підраховуємо кількість вимірів у кожній зоні для підсумкової статистики
            switch (zone) {
                case DANGER:  dangerCount++;  break;
                case WARNING: warningCount++; break;
                default:      clearCount++;   break;
            }

            // Позначаємо об'єкти зон DANGER і WARNING символом 'X' на радарі
            // CLEAR-зони не відображаємо — шлях вільний
            if (zone != DistanceZone.CLEAR) {
                // Нормуємо відстань до масштабу радіуса дисплею
                double normalised = Math.min(scan.getDistanceCm(), 50.0) / 50.0;
                double plotRadius = RADIUS * (1.0 - normalised) + 1.0;
                double rad = Math.toRadians(scan.getAngleDeg());
                int x = cx + (int) Math.round(plotRadius * Math.cos(rad));
                int y = cy - (int) Math.round(plotRadius * Math.sin(rad));
                if (y >= 0 && y < rows && x >= 0 && x < cols) {
                    grid[y][x] = 'X';
                }
            }
        }

        // Виводимо символьну сітку рядок за рядком
        for (char[] row : grid) {
            System.out.println(new String(row));
        }

        // Підпис горизонтальної осі — позначки 0°, 90°, 180°
        System.out.printf("%s0%s90%s180%n",
                spaces(1), spaces(cx - 2), spaces(DISPLAY_WIDTH - cx - 3));

        // Роздільник та підсумкова статистика зон
        System.out.println("-------------------------------------------");
        System.out.printf("Scans: %d | CLEAR: %d | WARNING: %d | DANGER: %d%n",
                result.getScanCount(), clearCount, warningCount, dangerCount);
        System.out.println("===========================================\n");
    }

    // -------------------------------------------------------------------------
    // Допоміжні методи
    // -------------------------------------------------------------------------

    /**
     * Returns a string of {@code count} space characters, used for ASCII layout alignment.
     *
     * @param count number of spaces; if negative, returns an empty string
     * @return string of spaces with the given length
     */
    private static String spaces(int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
