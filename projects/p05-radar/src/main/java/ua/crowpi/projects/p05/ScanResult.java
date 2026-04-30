package ua.crowpi.projects.p05;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregates all {@link RadarScan} measurements collected during one full radar sweep session.
 *
 * <p>A session begins when the servo starts at 0° and ends after it completes the return
 * sweep back to 0°. After each complete sweep the session data is exported to a JSON file
 * and a fresh {@code ScanResult} is created for the next sweep.</p>
 *
 * <p>Thread-safety note: {@link #addScan(RadarScan)} is called from the main project loop
 * only, so no synchronisation is required.</p>
 */
public class ScanResult {

    /**
     * Unique identifier for this session — equal to the timestamp at which the sweep started,
     * formatted as {@code yyyyMMdd_HHmmss}.
     */
    private final String sessionId;

    /** Ordered list of scan measurements collected during this session. */
    private final List<RadarScan> scans;

    /**
     * Creates a new, empty ScanResult for the sweep session identified by {@code sessionId}.
     *
     * @param sessionId timestamp string ({@code yyyyMMdd_HHmmss}) marking session start;
     *                  also used as the exported JSON filename suffix
     */
    public ScanResult(String sessionId) {
        this.sessionId = sessionId;
        // ArrayList обирається через O(1) append — сканування йде послідовно
        this.scans = new ArrayList<>();
    }

    /**
     * Appends a new radar measurement to this session.
     *
     * @param scan the completed {@link RadarScan}; must not be {@code null}
     */
    public void addScan(RadarScan scan) {
        // Додаємо скан в кінець списку — зберігаємо хронологічний порядок кутів
        scans.add(scan);
    }

    /**
     * Returns an unmodifiable view of all scans collected so far in this session.
     *
     * @return read-only list of {@link RadarScan} objects in insertion order
     */
    public List<RadarScan> getScans() {
        // Захищаємо внутрішній список від зовнішньої модифікації
        return Collections.unmodifiableList(scans);
    }

    /**
     * Returns the session identifier (start timestamp) for this scan session.
     *
     * @return session ID string, never {@code null}
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns the number of individual measurements collected in this session so far.
     *
     * @return count of scans, 0 or greater
     */
    public int getScanCount() {
        return scans.size();
    }
}
