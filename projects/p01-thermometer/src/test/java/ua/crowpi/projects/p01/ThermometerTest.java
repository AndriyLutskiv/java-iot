package ua.crowpi.projects.p01;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for the p01-thermometer module.
 *
 * <p>All tests operate on plain Java objects only — no hardware, no mocks,
 * no Raspberry Pi required. The test suite verifies thermal zone classification,
 * configuration loading, and LCD formatting.</p>
 */
class ThermometerTest {

    // Допоміжний ISO-8601 форматер для тестових показань
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // -------------------------------------------------------------------------
    // ThermalZone tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link ThermalZone#forTemp(double)} returns {@link ThermalZone#COLD}
     * for a temperature that is clearly below the 18 °C lower boundary.
     */
    @Test
    void testThermalZone_cold() {
        // 15°C — нижче нижньої межі 18°C → очікуємо COLD
        ThermalZone zone = ThermalZone.forTemp(15.0);
        assertEquals(ThermalZone.COLD, zone,
                "15.0°C should map to COLD zone (below 18°C boundary)");
    }

    /**
     * Verifies that {@link ThermalZone#forTemp(double)} returns {@link ThermalZone#COMFORT}
     * for a temperature within the comfortable 18–28 °C range.
     */
    @Test
    void testThermalZone_comfort() {
        // 23°C — в середині комфортного діапазону [18, 28] → очікуємо COMFORT
        ThermalZone zone = ThermalZone.forTemp(23.0);
        assertEquals(ThermalZone.COMFORT, zone,
                "23.0°C should map to COMFORT zone (within [18, 28] range)");
    }

    /**
     * Verifies that {@link ThermalZone#forTemp(double)} returns {@link ThermalZone#HOT}
     * for a temperature that exceeds the 28 °C upper boundary.
     */
    @Test
    void testThermalZone_hot() {
        // 30°C — вище верхньої межі 28°C → очікуємо HOT
        ThermalZone zone = ThermalZone.forTemp(30.0);
        assertEquals(ThermalZone.HOT, zone,
                "30.0°C should map to HOT zone (above 28°C boundary)");
    }

    // -------------------------------------------------------------------------
    // AlertConfig tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link AlertConfig} correctly loads the
     * {@code alert.threshold.celsius} property as {@code 28.0} from
     * {@code thermometer.properties} on the classpath.
     */
    @Test
    void testAlertConfig_loadsThreshold() {
        // AlertConfig завантажує дані з classpath — файл присутній в src/main/resources/
        AlertConfig config = new AlertConfig();
        assertEquals(28.0, config.getAlertThresholdCelsius(), 0.001,
                "alert.threshold.celsius should load as 28.0 from thermometer.properties");
    }

    /**
     * Verifies that {@link AlertConfig} loads {@code poll.interval.seconds} as {@code 2}.
     */
    @Test
    void testAlertConfig_loadsPollInterval() {
        AlertConfig config = new AlertConfig();
        assertEquals(2, config.getPollIntervalSeconds(),
                "poll.interval.seconds should load as 2 from thermometer.properties");
    }

    // -------------------------------------------------------------------------
    // LcdDisplayHelper tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link LcdDisplayHelper#formatLine1(TemperatureReading)}
     * returns a string of exactly 16 characters for a typical sensor reading.
     */
    @Test
    void testLcdLine1_exactlyI16Chars() {
        // Типове показання з реалістичними значеннями
        TemperatureReading reading = new TemperatureReading(
                23.4, 61.0, LocalDateTime.now().format(FMT));
        String line = LcdDisplayHelper.formatLine1(reading);

        assertNotNull(line, "formatLine1 must not return null");
        assertEquals(16, line.length(),
                "LCD line 1 must be exactly 16 characters, got: '" + line + "'");
    }

    /**
     * Verifies that {@link LcdDisplayHelper#formatLine2(ThermalZone)}
     * returns a string of exactly 16 characters for each thermal zone.
     */
    @Test
    void testLcdLine2_exactly16Chars() {
        // Перевіряємо всі три зони — кожна повинна давати рівно 16 символів
        for (ThermalZone zone : ThermalZone.values()) {
            String line = LcdDisplayHelper.formatLine2(zone);
            assertNotNull(line, "formatLine2 must not return null for zone " + zone);
            assertEquals(16, line.length(),
                    "LCD line 2 must be exactly 16 characters for zone " + zone
                    + ", got: '" + line + "'");
        }
    }

    // -------------------------------------------------------------------------
    // TemperatureReading POJO tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link TemperatureReading} correctly stores and returns all
     * three fields via its getters.
     */
    @Test
    void testTemperatureReading_getters() {
        String ts = "2024-03-15T14:23:01";
        TemperatureReading reading = new TemperatureReading(22.5, 55.0, ts);

        assertEquals(22.5, reading.getTemperatureC(), 0.001,
                "getTemperatureC() should return the value passed to the constructor");
        assertEquals(55.0, reading.getHumidity(), 0.001,
                "getHumidity() should return the value passed to the constructor");
        assertEquals(ts, reading.getTimestamp(),
                "getTimestamp() should return the timestamp passed to the constructor");
    }
}
