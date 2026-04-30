package ua.crowpi.projects.p02;

import ua.crowpi.core.hardware.I2cFacade;

/**
 * Driver for the CrowPi 7-segment LED display connected via I²C.
 *
 * <p>The display is controlled by an <strong>HT16K33</strong> LED driver chip at
 * I²C address {@value #HT16K33_ADDR}.  Communication follows the HT16K33 protocol:</p>
 * <ol>
 *   <li>Enable the internal oscillator: send command {@code 0x21}</li>
 *   <li>Turn on display output (no blink): send command {@code 0x81}</li>
 *   <li>Set brightness to maximum: send command {@code 0xEF}</li>
 *   <li>Write segment byte to display RAM register {@code 0x00} (digit position 0)</li>
 * </ol>
 *
 * <p>Each command is sent as the I²C register byte via
 * {@link I2cFacade#writeByte(int, int, byte)}, which maps to
 * {@code I2CDevice.write(register, value)} in Pi4J v1.4 — the HT16K33 ignores the
 * trailing data byte for single-byte commands.</p>
 *
 * <p>Segment bit layout in the HT16K33 display RAM (low byte of each row):</p>
 * <pre>
 *   Bit 0 = A (top)
 *   Bit 1 = B (upper right)
 *   Bit 2 = C (lower right)
 *   Bit 3 = D (bottom)
 *   Bit 4 = E (lower left)
 *   Bit 5 = F (upper left)
 *   Bit 6 = G (middle)
 * </pre>
 *
 * <p>Segment layout (standard):</p>
 * <pre>
 *   _
 *  |_|   ← A = top, B = upper-right, C = lower-right,
 *  |_|     D = bottom, E = lower-left, F = upper-left, G = middle
 * </pre>
 */
public class SevenSegmentDisplay {

    /** I²C address of the HT16K33 7-segment display controller on the CrowPi. */
    public static final int HT16K33_ADDR = 0x70;

    /** HT16K33 command: enable internal oscillator (required before display operations). */
    private static final int CMD_OSCILLATOR_ON  = 0x21;

    /** HT16K33 command: turn on display output with no blinking. */
    private static final int CMD_DISPLAY_ON     = 0x81;

    /** HT16K33 command: set brightness to maximum (level 15 of 15). */
    private static final int CMD_BRIGHTNESS_MAX = 0xEF;

    /** HT16K33 display RAM register for digit position 0 (low byte of row 0). */
    private static final int REG_DIGIT0 = 0x00;

    /**
     * Segment encoding table for digits 0–9.
     *
     * <p>Each row is {@code [A, B, C, D, E, F, G]} where {@code 1} means the segment
     * is illuminated and {@code 0} means it is off.  The order matches the HT16K33
     * bit mapping: A→bit0, B→bit1, …, G→bit6.</p>
     *
     * <pre>
     * Digit | A  B  C  D  E  F  G
     * ------+----------------------
     *   0   | 1  1  1  1  1  1  0
     *   1   | 0  1  1  0  0  0  0
     *   2   | 1  1  0  1  1  0  1
     *   3   | 1  1  1  1  0  0  1
     *   4   | 0  1  1  0  0  1  1
     *   5   | 1  0  1  1  0  1  1
     *   6   | 1  0  1  1  1  1  1
     *   7   | 1  1  1  0  0  0  0
     *   8   | 1  1  1  1  1  1  1
     *   9   | 1  1  1  1  0  1  1
     * </pre>
     */
    public static final int[][] SEGMENTS = {
        // A  B  C  D  E  F  G
        {1, 1, 1, 1, 1, 1, 0},  // 0: всі сегменти крім G (середнього)
        {0, 1, 1, 0, 0, 0, 0},  // 1: лише B та C (права вертикаль)
        {1, 1, 0, 1, 1, 0, 1},  // 2: A,B,D,E,G
        {1, 1, 1, 1, 0, 0, 1},  // 3: A,B,C,D,G
        {0, 1, 1, 0, 0, 1, 1},  // 4: B,C,F,G
        {1, 0, 1, 1, 0, 1, 1},  // 5: A,C,D,F,G
        {1, 0, 1, 1, 1, 1, 1},  // 6: A,C,D,E,F,G
        {1, 1, 1, 0, 0, 0, 0},  // 7: A,B,C
        {1, 1, 1, 1, 1, 1, 1},  // 8: всі сегменти
        {1, 1, 1, 1, 0, 1, 1},  // 9: A,B,C,D,F,G
    };

    private final I2cFacade i2c;

    /**
     * Creates a new display driver, initialises the HT16K33, and clears the display.
     *
     * @param i2c the I²C facade used to communicate with the HT16K33 at {@value #HT16K33_ADDR}
     */
    public SevenSegmentDisplay(I2cFacade i2c) {
        this.i2c = i2c;
        // Вмикаємо осцилятор HT16K33 (необхідно перед будь-якими операціями з дисплеєм)
        i2c.writeByte(HT16K33_ADDR, CMD_OSCILLATOR_ON,  (byte) 0x00);
        // Вмикаємо вихід дисплею без блимання
        i2c.writeByte(HT16K33_ADDR, CMD_DISPLAY_ON,     (byte) 0x00);
        // Встановлюємо максимальну яскравість
        i2c.writeByte(HT16K33_ADDR, CMD_BRIGHTNESS_MAX, (byte) 0x00);
        // Гасимо всі позиції одразу після ініціалізації
        clear();
    }

    /**
     * Illuminates the segments that represent the given decimal digit.
     *
     * <p>The segment bitmask is derived from {@link #SEGMENTS} and packed into a single
     * byte (A=bit0 … G=bit6) which is written to the HT16K33 display RAM at
     * register {@value #REG_DIGIT0} (digit position 0).</p>
     *
     * @param digit decimal digit to display; must be in the range [0, 9]
     * @throws IllegalArgumentException if {@code digit} is outside [0, 9]
     */
    public void showDigit(int digit) {
        if (digit < 0 || digit > 9) {
            throw new IllegalArgumentException(
                    "Digit must be in range [0, 9], got: " + digit);
        }

        int[] mask = SEGMENTS[digit];
        // Пакуємо сегменти у байт: A=біт0, B=біт1, …, G=біт6
        byte seg = (byte) (
                mask[0]          |   // A → bit 0
                (mask[1] << 1)   |   // B → bit 1
                (mask[2] << 2)   |   // C → bit 2
                (mask[3] << 3)   |   // D → bit 3
                (mask[4] << 4)   |   // E → bit 4
                (mask[5] << 5)   |   // F → bit 5
                (mask[6] << 6)       // G → bit 6
        );
        // Записуємо у регістр позиції 0 оперативної пам'яті дисплею HT16K33
        i2c.writeByte(HT16K33_ADDR, REG_DIGIT0, seg);
    }

    /**
     * Turns off all digit positions, leaving the display blank.
     *
     * <p>Called during initialisation and when the project shuts down.</p>
     */
    public void clear() {
        // Стираємо всі 8 позицій дисплею (регістри 0x00–0x0E з кроком 2)
        for (int reg = 0x00; reg <= 0x0E; reg += 2) {
            i2c.writeByte(HT16K33_ADDR, reg, (byte) 0x00);
        }
    }
}
