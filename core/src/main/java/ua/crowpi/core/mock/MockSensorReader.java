package ua.crowpi.core.mock;

import ua.crowpi.core.exception.HardwareException;
import ua.crowpi.core.hardware.SensorReader;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generic mock implementation of {@link SensorReader} for testing and laptop demos.
 *
 * <p>Takes a fixed list of values at construction time and cycles through them
 * indefinitely. This enables fully deterministic unit tests: the test controls
 * exactly which sequence of readings the project code receives.</p>
 *
 * <p>Example usage for a DHT11 mock that returns two temperature readings:</p>
 * <pre>{@code
 *   SensorReader<TemperatureReading> mock = new MockSensorReader<>(
 *       new TemperatureReading(22.5, 55.0, LocalDateTime.now()),
 *       new TemperatureReading(23.1, 57.0, LocalDateTime.now())
 *   );
 *   mock.read(); // → 22.5°C
 *   mock.read(); // → 23.1°C
 *   mock.read(); // → 22.5°C  (cycles)
 * }</pre>
 *
 * @param <T> the sensor reading type, e.g. {@code TemperatureReading}, {@code Boolean}, etc.
 */
public class MockSensorReader<T> implements SensorReader<T> {

    /** The fixed sequence of values returned in round-robin order. */
    private final List<T> values;

    // AtomicInteger використовуємо, бо read() може викликатись з різних потоків
    // (наприклад, ScheduledExecutorService у проекті + основний потік у тесті)
    private final AtomicInteger index = new AtomicInteger(0);

    /**
     * Creates a MockSensorReader that cycles through the given values.
     *
     * @param values one or more values to return in round-robin order; must not be empty
     * @throws IllegalArgumentException if {@code values} is empty
     */
    @SafeVarargs
    public MockSensorReader(T... values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("MockSensorReader requires at least one value");
        }
        // Arrays.asList повертає незмінний список — захист від зовнішньої модифікації
        this.values = Arrays.asList(values);
    }

    /**
     * Returns the next value from the cycle, wrapping around when the end is reached.
     *
     * <p>Thread-safe: uses {@link AtomicInteger#getAndUpdate} to advance the index
     * without a race condition between concurrent {@code read()} callers.</p>
     *
     * @return the next pre-configured sensor value
     * @throws HardwareException never thrown by this mock implementation
     */
    @Override
    public T read() throws HardwareException {
        // getAndUpdate атомарно повертає поточне значення і встановлює наступне
        int i = index.getAndUpdate(current -> (current + 1) % values.size());
        T value = values.get(i);
        System.out.printf("[MOCK SENSOR] read() → %s%n", value);
        return value;
    }

    /**
     * Returns the total number of values in the cycle.
     *
     * @return size of the value list
     */
    public int getCycleSize() {
        return values.size();
    }
}
