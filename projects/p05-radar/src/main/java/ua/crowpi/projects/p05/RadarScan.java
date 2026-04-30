package ua.crowpi.projects.p05;

/**
 * Immutable snapshot of a single radar measurement taken at a given servo angle.
 *
 * <p>A {@code RadarScan} captures the angle of the servo motor at the time of
 * measurement, the distance returned by the HC-SR04 ultrasonic sensor, and an
 * ISO-8601 timestamp string produced by the project's main loop. Instances are
 * created once and never mutated, making them safe to share across threads.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 *   RadarScan scan = new RadarScan(90, 42.7, "20240115_142311");
 *   System.out.println(scan); // RadarScan{angle=90, distance=42.70, ts=20240115_142311}
 * }</pre>
 */
public class RadarScan {

    /** Servo angle at which this measurement was taken, in degrees (0–180). */
    private final int angleDeg;

    /** Distance measured by the HC-SR04 sensor, in centimetres. */
    private final double distanceCm;

    /**
     * Timestamp of the measurement in {@code yyyyMMdd_HHmmss} format,
     * produced by {@link java.time.LocalDateTime#format}.
     */
    private final String timestamp;

    /**
     * Creates a new RadarScan with all fields set at construction time.
     *
     * @param angleDeg   servo angle in degrees (0–180 inclusive)
     * @param distanceCm distance reading from HC-SR04 in centimetres (2.0–400.0)
     * @param timestamp  measurement timestamp as {@code yyyyMMdd_HHmmss} string
     */
    public RadarScan(int angleDeg, double distanceCm, String timestamp) {
        this.angleDeg   = angleDeg;
        this.distanceCm = distanceCm;
        this.timestamp  = timestamp;
    }

    /**
     * Returns the servo angle at which this measurement was taken.
     *
     * @return angle in degrees, 0–180
     */
    public int getAngleDeg() {
        return angleDeg;
    }

    /**
     * Returns the ultrasonic distance reading in centimetres.
     *
     * @return distance in cm, clamped to [2.0, 400.0]
     */
    public double getDistanceCm() {
        return distanceCm;
    }

    /**
     * Returns the ISO-like timestamp string for this measurement.
     *
     * @return timestamp in {@code yyyyMMdd_HHmmss} format, never {@code null}
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Returns a human-readable representation useful for logging and debugging.
     *
     * @return string of the form {@code RadarScan{angle=90, distance=42.70, ts=20240115_142311}}
     */
    @Override
    public String toString() {
        // Формат з двома знаками після коми для читабельності дистанції
        return String.format("RadarScan{angle=%d, distance=%.2f, ts=%s}",
                angleDeg, distanceCm, timestamp);
    }
}
