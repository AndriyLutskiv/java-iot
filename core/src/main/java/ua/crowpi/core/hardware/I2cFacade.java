package ua.crowpi.core.hardware;

/**
 * Abstraction layer over the Raspberry Pi I²C bus.
 *
 * <p>Used by projects that communicate with I²C peripherals such as:</p>
 * <ul>
 *   <li>LCD 16×2 with PCF8574 backpack (address 0x27)</li>
 *   <li>7-segment display controllers</li>
 * </ul>
 *
 * <p>All communication is byte-level; higher-level protocol details (e.g. the
 * HD44780 LCD command set) are handled in the project-specific helper classes.</p>
 */
public interface I2cFacade {

    /**
     * Writes a single byte to the specified register of an I²C device.
     *
     * @param deviceAddr 7-bit I²C device address (e.g. 0x27 for the CrowPi LCD)
     * @param register   register address on the target device
     * @param value      byte value to write
     */
    void writeByte(int deviceAddr, int register, byte value);

    /**
     * Reads a single byte from the specified register of an I²C device.
     *
     * @param deviceAddr 7-bit I²C device address
     * @param register   register address to read from
     * @return the byte value returned by the device
     */
    byte readByte(int deviceAddr, int register);

    /**
     * Reads a block of bytes starting at the specified register.
     *
     * <p>Used for burst reads, e.g. reading temperature + humidity from DHT11
     * via an I²C bridge, or reading FIFO data from sensor chips.</p>
     *
     * @param deviceAddr 7-bit I²C device address
     * @param register   starting register address
     * @param length     number of bytes to read
     * @return byte array of exactly {@code length} bytes
     */
    byte[] readBytes(int deviceAddr, int register, int length);
}
