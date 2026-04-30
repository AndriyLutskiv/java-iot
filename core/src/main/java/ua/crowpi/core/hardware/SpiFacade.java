package ua.crowpi.core.hardware;

/**
 * Abstraction layer over the Raspberry Pi SPI bus.
 *
 * <p>Used by projects that communicate with SPI peripherals:</p>
 * <ul>
 *   <li>RFID RC-522 reader (p08) — CE0, SPI bus 0</li>
 *   <li>MCP3008 ADC for soil moisture sensor (p07) — CE1, SPI bus 0</li>
 * </ul>
 *
 * <p>SPI is full-duplex: every byte sent simultaneously returns a byte from the device.
 * The {@link #transfer(byte[])} method therefore always returns an array of the
 * same length as the input.</p>
 */
public interface SpiFacade {

    /**
     * Performs a full-duplex SPI transfer (simultaneous send + receive).
     *
     * <p>The transfer occupies the SPI bus for the duration of the call.
     * Callers must not share the facade across threads without external
     * synchronisation.</p>
     *
     * @param data bytes to transmit to the device (MOSI line)
     * @return bytes received from the device (MISO line); same length as {@code data}
     */
    byte[] transfer(byte[] data);

    /**
     * Releases the SPI bus and closes the underlying device handle.
     *
     * <p>Must be called from {@link ua.crowpi.core.CrowPiProject#shutdown()}.</p>
     */
    void close();
}
