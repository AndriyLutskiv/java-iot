package ua.crowpi.core.util;

import ua.crowpi.core.exception.ConfigException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility for loading {@code .properties} configuration files from the classpath.
 *
 * <p>Each CrowPi project stores its tunable parameters (GPIO pins, thresholds,
 * file paths) in a {@code .properties} file under {@code src/main/resources/}.
 * Gradle includes this directory in the JAR, so the file is always on the
 * classpath at runtime.</p>
 *
 * <p>All load methods throw {@link ConfigException} (unchecked) rather than
 * checked {@link IOException} so callers in project constructors do not need
 * to declare {@code throws} clauses — a bad config is a deployment error, not
 * a recoverable runtime condition.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 *   Properties cfg = PropertiesLoader.load("thermometer.properties");
 *   int threshold = PropertiesLoader.getInt(cfg, "alert.threshold.celsius");
 * }</pre>
 */
public final class PropertiesLoader {

    // Утилітний клас — конструктор приватний
    private PropertiesLoader() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Loads a {@code .properties} file from the classpath root.
     *
     * <p>The file must be on the classpath (i.e. under {@code src/main/resources/}
     * in the project module). The path must not start with {@code /}.</p>
     *
     * @param resourcePath classpath-relative path, e.g. {@code "thermometer.properties"}
     * @return loaded {@link Properties} object; never {@code null}
     * @throws ConfigException if the resource is not found or cannot be parsed
     */
    public static Properties load(String resourcePath) {
        // Використовуємо ClassLoader поточного потоку — найнадійніший спосіб знайти
        // ресурс у fat JAR, де всі .properties файли зібрані в корені classpath
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream in = cl.getResourceAsStream(resourcePath);

        if (in == null) {
            throw new ConfigException(
                    "Configuration file not found on classpath: " + resourcePath);
        }

        Properties props = new Properties();
        try (InputStream stream = in) {
            props.load(stream);
        } catch (IOException e) {
            throw new ConfigException(
                    "Failed to parse configuration file: " + resourcePath, e);
        }
        return props;
    }

    /**
     * Reads a required {@code String} property.
     *
     * @param props        the loaded properties object
     * @param key          the property key
     * @return property value trimmed of leading/trailing whitespace
     * @throws ConfigException if the key is missing or the value is blank
     */
    public static String getString(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new ConfigException("Required property missing or blank: " + key);
        }
        return value.trim();
    }

    /**
     * Reads a required {@code int} property.
     *
     * @param props the loaded properties object
     * @param key   the property key
     * @return parsed integer value
     * @throws ConfigException if the key is missing or the value is not a valid integer
     */
    public static int getInt(Properties props, String key) {
        String raw = getString(props, key);
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new ConfigException(
                    "Property '" + key + "' is not a valid integer: " + raw, e);
        }
    }

    /**
     * Reads a required {@code double} property.
     *
     * @param props the loaded properties object
     * @param key   the property key
     * @return parsed double value
     * @throws ConfigException if the key is missing or the value is not a valid double
     */
    public static double getDouble(Properties props, String key) {
        String raw = getString(props, key);
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw new ConfigException(
                    "Property '" + key + "' is not a valid number: " + raw, e);
        }
    }

    /**
     * Reads an optional property, returning a default value if the key is absent.
     *
     * @param props        the loaded properties object
     * @param key          the property key
     * @param defaultValue value to return when the key is missing
     * @return property value or {@code defaultValue}
     */
    public static String getStringOrDefault(Properties props, String key, String defaultValue) {
        String value = props.getProperty(key);
        // Повертаємо default якщо ключ відсутній або рядок порожній після trim()
        return (value != null && !value.trim().isEmpty()) ? value.trim() : defaultValue;
    }

    /**
     * Reads an optional {@code int} property, returning a default value if absent.
     *
     * @param props        the loaded properties object
     * @param key          the property key
     * @param defaultValue value to return when the key is missing or unparseable
     * @return parsed integer or {@code defaultValue}
     */
    public static int getIntOrDefault(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            // Некоректне значення → використовуємо default замість падіння
            return defaultValue;
        }
    }
}
