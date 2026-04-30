package ua.crowpi.core.pi4j;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import ua.crowpi.core.hardware.I2cFacade;

import java.io.IOException;

/**
 * Production {@link I2cFacade} implementation that drives a standard HD44780
 * 16×2 LCD connected via the <strong>Adafruit I²C/SPI LCD Backpack</strong>
 * (MCP23008 GPIO expander) as used on the CrowPi board.
 *
 * <p>The MCP23008 is an 8-bit I²C GPIO expander accessed via SMBus register
 * writes.  Unlike PCF8574-based backpacks, each I²C transaction addresses a
 * specific MCP23008 register (IODIR or GPIO).</p>
 *
 * <p>MCP23008 GP pin assignments (CrowPi wiring):</p>
 * <pre>
 *   GP0 — unused
 *   GP1 — RS  (Register Select: 0=instruction, 1=data)
 *   GP2 — EN  (Enable: pulse HIGH→LOW to latch nibble)
 *   GP3 — D4  (HD44780 data bit 4)
 *   GP4 — D5  (HD44780 data bit 5)
 *   GP5 — D6  (HD44780 data bit 6)
 *   GP6 — D7  (HD44780 data bit 7)
 *   GP7 — BL  (Backlight: 1=ON)
 * </pre>
 *
 * <p>Usage in a project's {@code run()} method:</p>
 * <pre>
 *   I2cFacade lcd = new Pi4jLcdFacade(I2CBus.BUS_1, 0x21);
 * </pre>
 *
 * <h3>Calling convention for project LCD-write code</h3>
 * <table border="1">
 *   <tr><th>{@code register} argument</th><th>Meaning</th></tr>
 *   <tr><td>{@code 0x00}</td><td>HD44780 <em>instruction</em> (RS=0)</td></tr>
 *   <tr><td>{@code 0x01}</td><td>HD44780 <em>character data</em> (RS=1)</td></tr>
 * </table>
 */
public class Pi4jLcdFacade implements I2cFacade, AutoCloseable {

    // -------------------------------------------------------------------------
    // MCP23008 registers
    // -------------------------------------------------------------------------

    /** MCP23008 I/O direction register — write 0x00 to make all GP pins outputs. */
    private static final int IODIR = 0x00;

    /** MCP23008 GPIO port register — read/write GP pin states. */
    private static final int GPIO  = 0x09;

    // -------------------------------------------------------------------------
    // MCP23008 GP bit masks (CrowPi LCD backpack wiring)
    // -------------------------------------------------------------------------

    /** GP1 = RS: Register Select (0=instruction, 1=data). */
    private static final int RS = 0x02;

    /** GP2 = EN: Enable — pulse HIGH→LOW to latch nibble into HD44780. */
    private static final int EN = 0x04;

    /** GP7 = BL: Backlight control (1=ON). */
    private static final int BL = 0x80;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** Physical I²C bus (bus 1 = /dev/i2c-1 on RPi 3). */
    private final I2CBus    bus;

    /** MCP23008 device object for the LCD backpack. */
    private final I2CDevice device;

    /** 7-bit I²C address of the MCP23008 (0x21 on the CrowPi). */
    private final int lcdAddress;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Opens the I²C bus, configures the MCP23008 as all-output, and runs the
     * HD44780 4-bit initialisation sequence.
     *
     * @param busNumber  Pi4J bus constant (use {@link I2CBus#BUS_1} for RPi 3)
     * @param lcdAddress 7-bit I²C address of the MCP23008 (e.g. {@code 0x21})
     * @throws RuntimeException if the bus cannot be opened or initialisation fails
     */
    public Pi4jLcdFacade(int busNumber, int lcdAddress) {
        this.lcdAddress = lcdAddress;
        try {
            this.bus    = I2CFactory.getInstance(busNumber);
            this.device = bus.getDevice(lcdAddress);
        } catch (I2CFactory.UnsupportedBusNumberException | IOException e) {
            throw new RuntimeException(
                    "Cannot open I2C bus " + busNumber + " / device 0x"
                    + Integer.toHexString(lcdAddress) + ": " + e.getMessage(), e);
        }
        initLcd();
    }

    // -------------------------------------------------------------------------
    // I2cFacade implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Interprets the {@code register} argument as the HD44780 register selector:</p>
     * <ul>
     *   <li>{@code 0x00} → sends {@code value} as an HD44780 <em>instruction</em> (RS=0).</li>
     *   <li>{@code 0x01} → sends {@code value} as HD44780 <em>character data</em> (RS=1).</li>
     *   <li>Any other register → silently ignored.</li>
     * </ul>
     */
    @Override
    public void writeByte(int deviceAddr, int register, byte value) {
        if (register == 0x00) {
            sendByte(value & 0xFF, 0);
        } else if (register == 0x01) {
            sendByte(value & 0xFF, RS);
        }
    }

    /** Not used for LCD-only facade — returns {@code 0}. */
    @Override
    public byte readByte(int deviceAddr, int register) { return 0; }

    /** Not used for LCD-only facade — returns a zero-filled array. */
    @Override
    public byte[] readBytes(int deviceAddr, int register, int length) { return new byte[length]; }

    // -------------------------------------------------------------------------
    // AutoCloseable
    // -------------------------------------------------------------------------

    /**
     * Closes the I²C bus and releases the file descriptor.
     *
     * @throws IOException if the bus cannot be closed
     */
    @Override
    public void close() throws IOException {
        bus.close();
    }

    // -------------------------------------------------------------------------
    // MCP23008 / HD44780 low-level protocol
    // -------------------------------------------------------------------------

    /**
     * Sends one full byte to the HD44780 as two 4-bit nibbles via the MCP23008.
     *
     * <p>Bit mapping — HD44780 data bits to MCP23008 GP pins:</p>
     * <pre>
     *   Upper nibble: HD44780 bits 7-4 → GP6-GP3  formula: (value &amp; 0xF0) &gt;&gt; 1
     *   Lower nibble: HD44780 bits 3-0 → GP6-GP3  formula: (value &amp; 0x0F) &lt;&lt; 3
     * </pre>
     *
     * @param value  the 8-bit value to send (unsigned, 0–255)
     * @param rsBit  {@link #RS} for data register; {@code 0} for instruction
     */
    private void sendByte(int value, int rsBit) {
        // Upper nibble: HD44780 D7-D4 → MCP23008 GP6-GP3 (shift right by 1)
        int hi = (value & 0xF0) >> 1;
        // Lower nibble: HD44780 D3-D0 → MCP23008 GP6-GP3 (shift left by 3)
        int lo = (value & 0x0F) << 3;
        sendNibble(hi | BL | rsBit);
        sendNibble(lo | BL | rsBit);
    }

    /**
     * Sends one 4-bit nibble to the MCP23008 GPIO register, pulsing EN HIGH→LOW
     * so the HD44780 latches the nibble on the falling edge.
     *
     * @param nibbleWithFlags MCP23008 GP byte with data bits and BL/RS already set
     */
    private void sendNibble(int nibbleWithFlags) {
        mcpWrite(nibbleWithFlags | EN);   // EN=1 — HD44780 latches on falling edge
        delayUs(1);                        // EN pulse width ≥ 450 ns
        mcpWrite(nibbleWithFlags & ~EN);  // EN=0 — HD44780 reads the nibble
        delayUs(50);                       // HD44780 execution time ≤ 37 µs
    }

    /**
     * Writes one byte to the MCP23008 GPIO register via an SMBus byte-write
     * transaction (START + addr + register + data + STOP).
     *
     * @param data the 8-bit value to place on GP0–GP7
     */
    private void mcpWrite(int data) {
        try {
            // device.write(register, value) — SMBus byte write, correct for MCP23008
            device.write(GPIO, (byte) (data & 0xFF));
        } catch (IOException e) {
            throw new RuntimeException(
                    "MCP23008 write failed (addr=0x" + Integer.toHexString(lcdAddress)
                    + ", data=0x" + Integer.toHexString(data & 0xFF) + "): "
                    + e.getMessage(), e);
        }
    }

    /**
     * Configures MCP23008 and initialises the HD44780 in 4-bit mode.
     *
     * <p>Sequence: set all GP pins as outputs, wait for power stabilisation,
     * then follow the HD44780 datasheet 4-bit init flow (3× 8-bit poke →
     * switch to 4-bit → Function Set → Display Off → Clear → Entry Mode → Display On).</p>
     */
    private void initLcd() {
        try {
            // MCP23008 IODIR = 0x00 → всі GP піни як виходи
            device.write(IODIR, (byte) 0x00);
        } catch (IOException e) {
            throw new RuntimeException("MCP23008 IODIR init failed: " + e.getMessage(), e);
        }

        delayMs(50);  // чекаємо стабілізації живлення LCD

        // Кроки 1-3: три ініціалізаційні поштовхи у 8-бітному режимі
        // HD44780 DB7-DB4 = 0011 → MCP23008 GP6-GP3 = 0001 1 → 0x18
        sendNibble(0x18 | BL); delayMs(5);
        sendNibble(0x18 | BL); delayUs(150);
        sendNibble(0x18 | BL); delayUs(150);

        // Крок 4: перехід у 4-бітний режим (DB7-DB4 = 0010 → 0x10)
        sendNibble(0x10 | BL); delayUs(150);

        // Крок 5: Function Set — 4-bit, 2 рядки, символи 5×8
        sendByte(0x28, 0); delayUs(50);

        // Крок 6: Display OFF
        sendByte(0x08, 0); delayUs(50);

        // Крок 7: Clear Display (вимагає ≥ 1.52 мс)
        sendByte(0x01, 0); delayMs(2);

        // Крок 8: Entry Mode Set — автоінкремент, без зсуву
        sendByte(0x06, 0); delayUs(50);

        // Крок 9: Display ON — без курсора і мигання
        sendByte(0x0C, 0); delayUs(50);
    }

    // -------------------------------------------------------------------------
    // Timing helpers
    // -------------------------------------------------------------------------

    /**
     * Busy-waits for approximately {@code us} microseconds using {@link System#nanoTime()}.
     * Used for sub-millisecond delays where {@link Thread#sleep} granularity (~100 µs) is
     * insufficient.
     */
    private static void delayUs(int us) {
        long end = System.nanoTime() + (long) us * 1_000L;
        while (System.nanoTime() < end) { /* spin */ }
    }

    /** Sleeps for {@code ms} milliseconds. */
    private static void delayMs(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
