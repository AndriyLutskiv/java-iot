package ua.crowpi.core.pi4j;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import ua.crowpi.core.exception.HardwareException;
import ua.crowpi.core.hardware.I2cFacade;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Production implementation of {@link I2cFacade} that wraps Pi4J v1.4 I²C API.
 *
 * <p>Opens the specified I²C bus on construction and keeps a cache of
 * {@link I2CDevice} instances, one per device address.  This avoids the
 * overhead of calling {@link I2CBus#getDevice(int)} on every read/write.</p>
 *
 * <p>Typical usage on the CrowPi with an LCD at address 0x27:</p>
 * <pre>
 *   I2cFacade lcd = new Pi4jI2cFacade(I2CBus.BUS_1);
 * </pre>
 *
 * <p>Call {@link #close()} when the hardware is no longer needed to release
 * the underlying I²C bus file descriptor.</p>
 *
 * <p><strong>Runs only on Raspberry Pi.</strong>  Instantiating this class on
 * a desktop machine will throw {@link HardwareException} because the Pi4J
 * native library is unavailable.</p>
 */
public class Pi4jI2cFacade implements I2cFacade, AutoCloseable {

    // Шина I²C — на CrowPi це BUS_1 (/dev/i2c-1)
    private final I2CBus bus;

    // Кеш пристроїв: адреса → об'єкт I2CDevice (щоб не відкривати щоразу)
    private final Map<Integer, I2CDevice> devices = new HashMap<>();

    // -------------------------------------------------------------------------
    // Конструктор
    // -------------------------------------------------------------------------

    /**
     * Opens the I²C bus with the specified bus number.
     *
     * @param busNumber Pi4J bus constant, e.g. {@link I2CBus#BUS_1}
     * @throws HardwareException if the bus cannot be opened (e.g. I²C not enabled,
     *                           not running on Raspberry Pi, or missing permissions)
     */
    public Pi4jI2cFacade(int busNumber) throws HardwareException {
        try {
            // Відкриваємо I²C шину — може кинути IOException або UnsupportedBusNumberException
            this.bus = I2CFactory.getInstance(busNumber);
        } catch (I2CFactory.UnsupportedBusNumberException e) {
            throw new HardwareException(
                    "Unsupported I2C bus number: " + busNumber
                    + ". Verify that I2C is enabled via raspi-config.", e);
        } catch (IOException e) {
            throw new HardwareException(
                    "Failed to open I2C bus " + busNumber
                    + ": " + e.getMessage()
                    + ". Ensure I2C is enabled (raspi-config → Interface Options → I2C).", e);
        }
    }

    // -------------------------------------------------------------------------
    // I2cFacade implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link I2CDevice#write(int, byte)} where the {@code register}
     * parameter maps to Pi4J's {@code address} (register offset on the device).</p>
     *
     * @throws RuntimeException wrapping {@link IOException} if the write fails
     */
    @Override
    public void writeByte(int deviceAddr, int register, byte value) {
        try {
            // Отримуємо або створюємо об'єкт пристрою з кешу
            getDevice(deviceAddr).write(register, value);
        } catch (IOException e) {
            // Перетворюємо на unchecked — I2cFacade не декларує checked виключення
            throw new RuntimeException(
                    "I2C write failed (addr=0x" + Integer.toHexString(deviceAddr)
                    + " reg=0x" + Integer.toHexString(register) + "): " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads one byte from the device register into a single-element buffer
     * via {@link I2CDevice#read(int, byte[], int, int)}.</p>
     *
     * @throws RuntimeException wrapping {@link IOException} if the read fails
     */
    @Override
    public byte readByte(int deviceAddr, int register) {
        try {
            byte[] buf = new byte[1];
            // Зчитуємо один байт зі вказаного регістру пристрою
            getDevice(deviceAddr).read(register, buf, 0, 1);
            return buf[0];
        } catch (IOException e) {
            throw new RuntimeException(
                    "I2C read failed (addr=0x" + Integer.toHexString(deviceAddr)
                    + " reg=0x" + Integer.toHexString(register) + "): " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads {@code length} bytes starting at {@code register} via
     * {@link I2CDevice#read(int, byte[], int, int)}.</p>
     *
     * @throws RuntimeException wrapping {@link IOException} if the read fails
     */
    @Override
    public byte[] readBytes(int deviceAddr, int register, int length) {
        try {
            byte[] buf = new byte[length];
            // Блоковий зчит із вказаного регістру — корисно для датчиків з FIFO/пакетами
            getDevice(deviceAddr).read(register, buf, 0, length);
            return buf;
        } catch (IOException e) {
            throw new RuntimeException(
                    "I2C readBytes failed (addr=0x" + Integer.toHexString(deviceAddr)
                    + " reg=0x" + Integer.toHexString(register)
                    + " len=" + length + "): " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // AutoCloseable
    // -------------------------------------------------------------------------

    /**
     * Closes the underlying I²C bus and releases the file descriptor.
     *
     * <p>Should be called in the project's {@code shutdown()} method.</p>
     *
     * @throws IOException if the bus cannot be closed
     */
    @Override
    public void close() throws IOException {
        // Закриваємо шину — звільняємо файловий дескриптор /dev/i2c-N
        bus.close();
        devices.clear();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the cached {@link I2CDevice} for the given address, creating one
     * if it does not exist yet.
     *
     * @param deviceAddr 7-bit I²C device address
     * @return the device instance
     * @throws IOException if Pi4J cannot obtain the device
     */
    private I2CDevice getDevice(int deviceAddr) throws IOException {
        // computeIfAbsent не підходить тут, бо getDevice кидає IOException
        I2CDevice dev = devices.get(deviceAddr);
        if (dev == null) {
            // Відкриваємо пристрій вперше — зберігаємо у кеш для наступних операцій
            dev = bus.getDevice(deviceAddr);
            devices.put(deviceAddr, dev);
        }
        return dev;
    }
}
