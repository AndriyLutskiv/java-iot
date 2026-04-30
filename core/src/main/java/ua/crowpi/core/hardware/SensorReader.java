package ua.crowpi.core.hardware;

import ua.crowpi.core.exception.HardwareException;

/**
 * Generic contract for reading a single typed value from any CrowPi sensor.
 *
 * <p>Parametrising the return type allows compile-time safety across all sensor types:</p>
 * <ul>
 *   <li>{@code SensorReader<TemperatureReading>} — DHT11</li>
 *   <li>{@code SensorReader<Integer>} — raw ADC value from MCP3008</li>
 *   <li>{@code SensorReader<String>} — UID string from RFID RC-522</li>
 *   <li>{@code SensorReader<Boolean>} — digital PIR or tilt sensor</li>
 * </ul>
 *
 * <p>In mock mode, {@link ua.crowpi.core.mock.MockSensorReader} returns pre-defined
 * values in a cyclic sequence, enabling deterministic unit tests and laptop demos.</p>
 *
 * @param <T> the type of reading produced by the sensor
 */
public interface SensorReader<T> {

    /**
     * Reads and returns one measurement from the sensor.
     *
     * <p>Implementations may block for a short time (e.g. DHT11 requires an 18 ms
     * LOW pulse before it responds). Callers should not invoke this from the
     * main UI thread if the blocking delay would cause visible jank.</p>
     *
     * @return the sensor reading; never {@code null}
     * @throws HardwareException if communication with the sensor fails
     *         (checksum error, timeout, bus fault, etc.)
     */
    T read() throws HardwareException;
}
