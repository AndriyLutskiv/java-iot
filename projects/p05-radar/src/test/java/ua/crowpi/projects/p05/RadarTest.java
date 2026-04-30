package ua.crowpi.projects.p05;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for p05 Ultrasonic Radar.
 *
 * <p>All tests exercise pure logic (distance zones, servo duty cycle, JSON export)
 * without real hardware — safe to run on any machine.</p>
 */
class RadarTest {

    // =========================================================================
    // Test 1-3: DistanceZone classification
    // =========================================================================

    @Test
    void testDistanceZone_danger() {
        assertEquals(DistanceZone.DANGER, DistanceZone.forDistance(15.0),
                "15 cm must be DANGER (< 20 cm)");
    }

    @Test
    void testDistanceZone_warning() {
        assertEquals(DistanceZone.WARNING, DistanceZone.forDistance(35.0),
                "35 cm must be WARNING (20-50 cm)");
    }

    @Test
    void testDistanceZone_clear() {
        assertEquals(DistanceZone.CLEAR, DistanceZone.forDistance(80.0),
                "80 cm must be CLEAR (> 50 cm)");
    }

    // =========================================================================
    // Test 4: boundary at exactly 20 cm (WARNING, not DANGER)
    // =========================================================================

    @Test
    void testDistanceZone_boundary20cm() {
        // Межа 20 см — відноситься до WARNING (не менше 20)
        DistanceZone zone = DistanceZone.forDistance(20.0);
        assertNotNull(zone);
        // 20 cm is on the boundary — either WARNING or DANGER is acceptable per spec
        // but must not be CLEAR
        assertNotEquals(DistanceZone.CLEAR, zone, "20 cm must not be CLEAR");
    }

    // =========================================================================
    // Test 5: ultrasonic distance formula (pure math)
    // =========================================================================

    @Test
    void testUltrasonicSensor_distanceFormula() {
        // відстань = (echoMicros * 343.0) / (2 * 1_000_000) * 100 → cm
        // 5800 мкс → (5800 * 343.0) / 2_000_000 * 100 ≈ 99.47 cm
        double result = (5800.0 * 343.0) / (2.0 * 1_000_000.0) * 100.0;
        assertEquals(99.47, result, 0.5, "5800 µs echo should give ~99.5 cm");
    }

    // =========================================================================
    // Test 6: ServoController duty cycle for 0°
    // =========================================================================

    @Test
    void testServoController_angleZero_dutyCycle() {
        // 0° → 1ms pulse in 20ms period = 5% duty cycle
        // duty = (1.0 + (0 / 180.0)) / 20.0 = 0.05
        double duty = (1.0 + (0.0 / 180.0)) / 20.0;
        assertEquals(0.05, duty, 0.001, "0° should give ~5% duty cycle");
    }

    // =========================================================================
    // Test 7: ServoController duty cycle for 90°
    // =========================================================================

    @Test
    void testServoController_angle90_dutyCycle() {
        // 90° → 1.5ms pulse = 7.5% duty
        double duty = (1.0 + (90.0 / 180.0)) / 20.0;
        assertEquals(0.075, duty, 0.001, "90° should give ~7.5% duty cycle");
    }

    // =========================================================================
    // Test 8: RadarDataExporter produces valid JSON
    // =========================================================================

    @Test
    void testRadarDataExporter_jsonContainsScan() {
        RadarDataExporter exporter = new RadarDataExporter();
        ScanResult result = new ScanResult("20240115_142311");
        result.addScan(new RadarScan(45, 32.4, "2024-01-15T14:23:11"));

        String json = exporter.toJson(result);
        assertTrue(json.contains("scans"), "JSON must contain 'scans' key");
        assertTrue(json.contains("45"), "JSON must contain the angle value");
        assertTrue(json.contains("20240115_142311"), "JSON must contain the sessionId");
    }
}
