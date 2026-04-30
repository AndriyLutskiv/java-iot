package ua.crowpi.core.matrix;

import ua.crowpi.core.hardware.SpiFacade;

/**
 * Driver for an 8×8 LED matrix module based on the MAX7219 display controller.
 *
 * <p>The MAX7219 communicates over SPI using 16-bit frames (register byte +
 * data byte). This class handles initialisation and frame rendering; higher-level
 * effects (scrolling text, animations) are implemented in {@link MatrixScroller}.</p>
 *
 * <h2>Register map used</h2>
 * <pre>
 *  0x09  Decode mode   → 0x00 (raw bitmap, no BCD decode)
 *  0x0A  Intensity     → 0x04 (0–15; medium brightness)
 *  0x0B  Scan limit    → 0x07 (scan all 8 rows)
 *  0x0C  Shutdown      → 0x01 (normal operation)
 *  0x0F  Display test  → 0x00 (off)
 *  0x01–0x08  Row 1–8  (row data)
 * </pre>
 *
 * <h2>Column bit order</h2>
 * <p>The MAX7219 row register encodes each row as a bitmask where
 * <strong>bit 7 = leftmost column</strong> (column 0) and
 * <strong>bit 0 = rightmost column</strong> (column 7).
 * Adjust {@link #setFrame} if your matrix is wired with the opposite orientation.</p>
 *
 * <h2>Font format</h2>
 * <p>{@link #setFrame(byte[])} accepts 8 column bytes in
 * <em>Adafruit GFX 5×7 format</em>: <strong>bit 0 = top row</strong> (row 1),
 * bit 6 = bottom row (row 7), bit 7 = unused. The method transposes this
 * column-major input into the row-major data expected by the MAX7219.</p>
 */
public final class Max7219Matrix {

    // -------------------------------------------------------------------------
    // MAX7219 register addresses
    // -------------------------------------------------------------------------

    private static final int REG_DECODE      = 0x09;
    private static final int REG_INTENSITY   = 0x0A;
    private static final int REG_SCAN_LIMIT  = 0x0B;
    private static final int REG_SHUTDOWN    = 0x0C;
    private static final int REG_DISPLAY_TEST = 0x0F;

    /** Number of rows (and columns) on the 8×8 matrix. */
    public static final int SIZE = 8;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final SpiFacade spi;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a Max7219Matrix backed by the given SPI facade and immediately
     * initialises the MAX7219 chip into a known display-ready state.
     *
     * @param spi SPI facade connected to the MAX7219 (real or mock)
     */
    public Max7219Matrix(SpiFacade spi) {
        this.spi = spi;
        init();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Turns off all LEDs on the matrix without changing the MAX7219 settings.
     */
    public void clear() {
        for (int row = 1; row <= SIZE; row++) {
            send(row, 0x00);
        }
    }

    /**
     * Renders a frame onto the matrix.
     *
     * <p>The {@code columns} array must contain exactly {@value #SIZE} bytes,
     * one per display column from left (index 0) to right (index 7).
     * Each byte uses <em>Adafruit GFX format</em>: bit 0 = top row, bit 6 = bottom row.</p>
     *
     * <p>The method transposes the column-major input into the row-major registers
     * of the MAX7219:
     * <pre>
     *   MAX7219 row r register bit (7 − col) = font column byte col, bit (r − 1)
     * </pre>
     * </p>
     *
     * @param columns 8-byte column array (Adafruit GFX format)
     * @throws IllegalArgumentException if {@code columns.length != 8}
     */
    public void setFrame(byte[] columns) {
        if (columns.length != SIZE) {
            throw new IllegalArgumentException(
                    "columns must have exactly " + SIZE + " bytes, got " + columns.length);
        }

        // Транспозиція: стовпці (шрифт) → рядки (MAX7219 регістри)
        for (int row = 1; row <= SIZE; row++) {
            int rowBit  = row - 1;   // bit 0 = row 1 (top) у форматі Adafruit
            byte rowData = 0;
            for (int col = 0; col < SIZE; col++) {
                int pixelOn = (columns[col] >> rowBit) & 1;
                // bit 7 = ліва колонка (col=0), bit 0 = права (col=7)
                rowData |= (byte) (pixelOn << (SIZE - 1 - col));
            }
            send(row, rowData & 0xFF);
        }
    }

    /**
     * Powers down the MAX7219 (clears the display and enters shutdown mode).
     * Call from project {@code shutdown()} to ensure the display goes dark
     * when switching between projects.
     */
    public void shutdown() {
        clear();
        send(REG_SHUTDOWN, 0x00);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Initialises the MAX7219 into a known display-ready state.
     * Called once from the constructor.
     */
    private void init() {
        send(REG_DISPLAY_TEST, 0x00);   // вимикаємо тест-режим
        send(REG_SCAN_LIMIT,   0x07);   // відображаємо всі 8 рядків
        send(REG_DECODE,       0x00);   // raw-режим (без BCD-декодування)
        send(REG_INTENSITY,    0x04);   // середня яскравість (0–15)
        send(REG_SHUTDOWN,     0x01);   // нормальна робота (не sleep)
        clear();
    }

    /**
     * Sends a single MAX7219 register write as a 2-byte SPI frame.
     *
     * @param register MAX7219 register address (0x00–0x0F)
     * @param data     value to write (0x00–0xFF)
     */
    private void send(int register, int data) {
        spi.transfer(new byte[]{(byte) register, (byte) data});
    }
}
