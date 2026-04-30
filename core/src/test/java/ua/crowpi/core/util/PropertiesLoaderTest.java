package ua.crowpi.core.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ua.crowpi.core.exception.ConfigException;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link PropertiesLoader}.
 *
 * <p>Tests do not rely on actual {@code .properties} files on the classpath;
 * instead they build {@link Properties} objects in-memory to test the parsing and
 * validation methods in isolation. The {@link PropertiesLoader#load(String)} method
 * is tested separately with a resource file placed in the test resources directory.</p>
 */
class PropertiesLoaderTest {

    private Properties props;

    @BeforeEach
    void setUp() {
        props = new Properties();
    }

    // -------------------------------------------------------------------------
    // getString
    // -------------------------------------------------------------------------

    @Test
    void testGetString_presentKey_returnsValue() {
        props.setProperty("app.name", "CrowPi Suite");

        String result = PropertiesLoader.getString(props, "app.name");

        assertEquals("CrowPi Suite", result);
    }

    @Test
    void testGetString_trailingWhitespaceIsTrimmed() {
        props.setProperty("key", "  hello  ");

        assertEquals("hello", PropertiesLoader.getString(props, "key"));
    }

    @Test
    void testGetString_missingKey_throwsConfigException() {
        assertThrows(ConfigException.class,
                () -> PropertiesLoader.getString(props, "missing.key"));
    }

    @Test
    void testGetString_blankValue_throwsConfigException() {
        props.setProperty("blank", "   ");

        assertThrows(ConfigException.class,
                () -> PropertiesLoader.getString(props, "blank"));
    }

    // -------------------------------------------------------------------------
    // getInt
    // -------------------------------------------------------------------------

    @Test
    void testGetInt_validInteger_returnsParsedValue() {
        props.setProperty("alert.threshold.celsius", "28");

        int result = PropertiesLoader.getInt(props, "alert.threshold.celsius");

        assertEquals(28, result);
    }

    @Test
    void testGetInt_negativeInteger_returnsParsedValue() {
        props.setProperty("min.temp", "-10");

        assertEquals(-10, PropertiesLoader.getInt(props, "min.temp"));
    }

    @Test
    void testGetInt_nonNumericValue_throwsConfigException() {
        props.setProperty("interval", "two");

        assertThrows(ConfigException.class,
                () -> PropertiesLoader.getInt(props, "interval"));
    }

    // -------------------------------------------------------------------------
    // getDouble
    // -------------------------------------------------------------------------

    @Test
    void testGetDouble_validDouble_returnsParsedValue() {
        props.setProperty("temp.threshold", "26.5");

        double result = PropertiesLoader.getDouble(props, "temp.threshold");

        assertEquals(26.5, result, 0.001);
    }

    @Test
    void testGetDouble_integerString_returnsParsedAsDouble() {
        props.setProperty("temp.threshold", "30");

        assertEquals(30.0, PropertiesLoader.getDouble(props, "temp.threshold"), 0.001);
    }

    // -------------------------------------------------------------------------
    // getStringOrDefault
    // -------------------------------------------------------------------------

    @Test
    void testGetStringOrDefault_missingKey_returnsDefault() {
        String result = PropertiesLoader.getStringOrDefault(props, "absent", "fallback");

        assertEquals("fallback", result);
    }

    @Test
    void testGetStringOrDefault_presentKey_returnsActualValue() {
        props.setProperty("log.file", "logs/app.csv");

        assertEquals("logs/app.csv",
                PropertiesLoader.getStringOrDefault(props, "log.file", "default.csv"));
    }

    // -------------------------------------------------------------------------
    // getIntOrDefault
    // -------------------------------------------------------------------------

    @Test
    void testGetIntOrDefault_missingKey_returnsDefault() {
        int result = PropertiesLoader.getIntOrDefault(props, "poll.interval", 5);

        assertEquals(5, result);
    }

    @Test
    void testGetIntOrDefault_malformedValue_returnsDefault() {
        props.setProperty("poll.interval", "not-a-number");

        // Некоректне значення → повертаємо default замість виключення
        assertEquals(10, PropertiesLoader.getIntOrDefault(props, "poll.interval", 10));
    }

    @Test
    void testGetIntOrDefault_validValue_returnsParsedValue() {
        props.setProperty("poll.interval", "30");

        assertEquals(30, PropertiesLoader.getIntOrDefault(props, "poll.interval", 5));
    }

    // -------------------------------------------------------------------------
    // load() — тест з реальним ресурсом
    // -------------------------------------------------------------------------

    @Test
    void testLoad_nonExistentResource_throwsConfigException() {
        // Неіснуючий файл повинен кинути ConfigException, а не NullPointerException
        assertThrows(ConfigException.class,
                () -> PropertiesLoader.load("this-file-does-not-exist.properties"));
    }
}
