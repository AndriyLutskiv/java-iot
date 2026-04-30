package ua.crowpi.projects.p07;

import ua.crowpi.core.exception.HardwareException;
import ua.crowpi.core.hardware.GpioFacade;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reads soil moisture level from an MCP3008 10-bit SPI ADC connected to the CrowPi.
 *
 * <p><strong>Real hardware wiring:</strong> MCP3008 channel 0 is connected to
 * a capacitive or resistive soil moisture probe. The ADC produces a 10-bit
 * value (0–1023) where:</p>
 * <ul>
 *   <li>Low ADC value (near 0) = wet soil (high conductivity)</li>
 *   <li>High ADC value (near 1023) = dry soil (low conductivity)</li>
 * </ul>
 *
 * <p>The conversion formula is: {@code percent = (int)(100.0 - (rawValue / 1023.0 * 100.0))}</p>
 *
 * <p><strong>Mock mode:</strong> Because {@code SpiFacade} is not available
 * in this module, a cycling sequence of pre-defined moisture percentages is
 * returned: 75, 55, 35, 20, 45. This sequence intentionally crosses the
 * default threshold of 40% to exercise both pump-on and pump-off decisions
 * during a mock demo.</p>
 */
public class SoilMoistureSensor {

    /**
     * Pre-defined cycling values for mock mode.
     * Sequence deliberately crosses the default 40% threshold to show pump activation.
     * Values: 75% (wet), 55% (ok), 35% (dry→pump), 20% (very dry→pump), 45% (ok).
     */
    private static final int[] MOCK_CYCLE = {75, 55, 35, 20, 45};

    // AtomicInteger забезпечує потокобезпечний лічильник для cycling у mock-режимі
    private final AtomicInteger mockIndex = new AtomicInteger(0);

    /** GPIO facade — kept for future real SPI integration via bit-banging if needed. */
    private final GpioFacade gpio;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a SoilMoistureSensor backed by the given GPIO facade.
     *
     * <p>In the current implementation the GPIO facade is reserved for future
     * bit-bang SPI or chip-select control; all reads return mock cycling values.</p>
     *
     * @param gpio GPIO facade (real or mock)
     */
    public SoilMoistureSensor(GpioFacade gpio) {
        this.gpio = gpio;
    }

    // -------------------------------------------------------------------------
    // Sensor read
    // -------------------------------------------------------------------------

    /**
     * Returns the current soil moisture level as a percentage (0–100).
     *
     * <p><strong>Real hardware path (not yet implemented):</strong>
     * Would perform a full-duplex SPI transaction with the MCP3008 to read
     * channel 0. The 10-bit raw value would be converted using:
     * {@code percent = (int)(100.0 - (rawValue / 1023.0 * 100.0))}</p>
     *
     * <p><strong>Mock mode:</strong> Cycles through the sequence
     * {75, 55, 35, 20, 45} in round-robin order, wrapping back to 75
     * after 45.</p>
     *
     * @return soil moisture percentage in range 0–100
     * @throws HardwareException if an SPI communication error occurs
     *         (not thrown by mock implementation)
     */
    public int readPercent() throws HardwareException {
        // Повертаємо симульоване значення з циклічного масиву
        // У реальній версії тут був би SPI-запит до MCP3008 по протоколу:
        //   byte[] tx = {0x01, (byte)0x80, 0x00};
        //   byte[] rx = spi.transfer(tx);
        //   int raw = ((rx[1] & 0x03) << 8) | (rx[2] & 0xFF);
        //   return (int)(100.0 - (raw / 1023.0 * 100.0));
        int idx = mockIndex.getAndUpdate(current -> (current + 1) % MOCK_CYCLE.length);
        int percent = MOCK_CYCLE[idx];
        System.out.printf("[SOIL SENSOR] readPercent() → %d%% (mock cycle idx=%d)%n", percent, idx);
        return percent;
    }

    /**
     * Converts a raw 10-bit MCP3008 ADC value to a soil moisture percentage.
     *
     * <p>Formula: {@code percent = (int)(100.0 - (rawValue / 1023.0 * 100.0))}</p>
     * <p>Lower ADC value = wetter soil = higher percentage.</p>
     *
     * @param rawValue the 10-bit ADC reading (0–1023)
     * @return soil moisture percentage (0=completely dry, 100=fully saturated)
     */
    public static int rawToPercent(int rawValue) {
        // Інвертуємо шкалу: менший ADC = вища вологість
        return (int) (100.0 - (rawValue / 1023.0 * 100.0));
    }
}
