package ua.crowpi.projects.p10;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for p10 Weather Station with Trend Forecast.
 *
 * <p>All tests verify pure logic (RingBuffer, TrendAnalyzer, HeatIndex, WeatherForecast, HTML)
 * without any hardware dependencies.</p>
 */
class MeteostationTest {

    private final TrendAnalyzer analyzer = new TrendAnalyzer();

    // =========================================================================
    // RingBuffer tests
    // =========================================================================

    @Test
    void testRingBuffer_overflow() {
        // 181-й елемент повинен витіснити перший — розмір залишається 180
        RingBuffer<Integer> buf = new RingBuffer<>(180);
        for (int i = 0; i < 181; i++) {
            buf.add(i);
        }
        assertEquals(180, buf.size(), "Buffer size must not exceed capacity");
        // Перший елемент (0) витіснений — список починається з 1
        assertEquals(1, buf.toList().get(0).intValue(), "Oldest element must have been evicted");
    }

    @Test
    void testRingBuffer_toList_order() {
        // Порядок вставки повинен зберігатись
        RingBuffer<Integer> buf = new RingBuffer<>(10);
        buf.add(1); buf.add(2); buf.add(3);
        List<Integer> list = buf.toList();
        assertEquals(Arrays.asList(1, 2, 3), list, "toList() must preserve insertion order");
    }

    // =========================================================================
    // TrendAnalyzer tests
    // =========================================================================

    @Test
    void testTrendAnalyzer_risingSlope() {
        // [20,21,22,23,24] → МНК slope ≈ +1.0 (лінійне зростання на 1 за крок)
        List<Double> rising = Arrays.asList(20.0, 21.0, 22.0, 23.0, 24.0);
        double slope = analyzer.calculateSlope(rising);
        assertEquals(1.0, slope, 0.01, "Rising sequence must have slope ≈ +1.0");
    }

    @Test
    void testTrendAnalyzer_fallingSlope() {
        // [24,23,22,21,20] → slope ≈ -1.0
        List<Double> falling = Arrays.asList(24.0, 23.0, 22.0, 21.0, 20.0);
        double slope = analyzer.calculateSlope(falling);
        assertEquals(-1.0, slope, 0.01, "Falling sequence must have slope ≈ -1.0");
    }

    @Test
    void testTrendAnalyzer_flatSlope() {
        // [22,22,22,22,22] → slope = 0.0
        List<Double> flat = Arrays.asList(22.0, 22.0, 22.0, 22.0, 22.0);
        double slope = analyzer.calculateSlope(flat);
        assertEquals(0.0, slope, 0.001, "Constant sequence must have slope ≈ 0.0");
    }

    // =========================================================================
    // WeatherForecast tests
    // =========================================================================

    @Test
    void testWeatherForecast_fromSlope_improving() {
        // slope > +0.05 → IMPROVING
        assertEquals(WeatherForecast.IMPROVING, WeatherForecast.fromSlope(0.1));
    }

    @Test
    void testWeatherForecast_fromSlope_worsening() {
        // slope < -0.05 → WORSENING
        assertEquals(WeatherForecast.WORSENING, WeatherForecast.fromSlope(-0.1));
    }

    @Test
    void testWeatherForecast_fromSlope_stable() {
        // |slope| <= 0.05 → STABLE
        assertEquals(WeatherForecast.STABLE, WeatherForecast.fromSlope(0.01));
    }

    // =========================================================================
    // HeatIndex test
    // =========================================================================

    @Test
    void testHeatIndex_knownValue() {
        // При 35°C та 80% вологості heat index > 40°C (зона Heat Stroke risk)
        double hi = HeatIndex.calculate(35.0, 80.0);
        assertTrue(hi > 40.0, "HI at 35°C / 80% must exceed 40°C (heat stroke zone)");
    }

    // =========================================================================
    // HtmlReportGenerator test
    // =========================================================================

    @Test
    void testHtmlReportGenerator_containsTitle() {
        HtmlReportGenerator gen = new HtmlReportGenerator();
        WeatherReading r = new WeatherReading(22.0, 60.0, false, false, "2024-01-15T12:00:00");
        String html = gen.generate(List.of(r), WeatherForecast.STABLE, 22.5);
        assertTrue(html.contains("<title>"), "Generated HTML must contain a <title> tag");
        assertTrue(html.contains("Weather"), "Generated HTML must mention Weather in the title");
    }
}
