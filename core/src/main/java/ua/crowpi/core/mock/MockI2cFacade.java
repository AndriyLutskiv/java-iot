package ua.crowpi.core.mock;

import ua.crowpi.core.hardware.I2cFacade;

/**
 * Mock implementation of {@link I2cFacade} for running on a laptop (no RPi required).
 *
 * <p>Write operations are logged to stdout in a format that mirrors a real I²C
 * transaction trace, helping students understand the bus protocol. Read operations
 * return predictable dummy bytes so higher-level logic can be tested without hardware.</p>
 *
 * <p>The mock LCD rendering (simulating the LCD 16×2 display output in the console)
 * is intentionally kept in this class through a simple internal character buffer,
 * so students see realistic output when running in {@code --mock} mode:</p>
 * <pre>
 *   ┌────────────────┐
 *   │TEMP: 23.4C     │
 *   │STATUS: COMFORT │
 *   └────────────────┘
 * </pre>
 */
public class MockI2cFacade implements I2cFacade {

    /** LCD line length for the CrowPi 16×2 display. */
    private static final int LCD_COLS = 16;

    // Внутрішній буфер двох рядків LCD — оновлюється при записі в адресу 0x27
    private final char[] lcdLine1 = new char[LCD_COLS];
    private final char[] lcdLine2 = new char[LCD_COLS];

    // Прапор: чи буде наступний запис у рядок 1 чи рядок 2 (спрощена модель HD44780)
    private boolean writingLine2 = false;
    private int cursorPos = 0;

    /**
     * Creates a MockI2cFacade and fills the LCD buffer with blank spaces.
     */
    public MockI2cFacade() {
        // Ініціалізуємо рядки пробілами — як фізичний LCD після reset
        java.util.Arrays.fill(lcdLine1, ' ');
        java.util.Arrays.fill(lcdLine2, ' ');
    }

    /**
     * {@inheritDoc}
     *
     * <p>In mock mode: if the target device is the LCD (address 0x27),
     * the byte is interpreted as a printable character and placed into the
     * internal line buffer; then the full display is reprinted to stdout.
     * For other devices the raw hex values are logged.</p>
     */
    @Override
    public void writeByte(int deviceAddr, int register, byte value) {
        if (deviceAddr == 0x21) {
            // Записуємо символ у відповідний рядок і оновлюємо відображення
            handleLcdByte(register, value);
        } else {
            System.out.printf("[MOCK I2C] write → addr=0x%02X reg=0x%02X val=0x%02X%n",
                    deviceAddr, register, value);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>In mock mode: returns 0x00 for any device/register combination and logs the call.</p>
     */
    @Override
    public byte readByte(int deviceAddr, int register) {
        System.out.printf("[MOCK I2C] read  ← addr=0x%02X reg=0x%02X → 0x00 (mock)%n",
                deviceAddr, register);
        // Повертаємо нуль — достатньо для тестів, де важлива лише поведінка логіки,
        // а не реальне значення регістру
        return 0x00;
    }

    /**
     * {@inheritDoc}
     *
     * <p>In mock mode: returns a zero-filled byte array and logs the call.</p>
     */
    @Override
    public byte[] readBytes(int deviceAddr, int register, int length) {
        System.out.printf("[MOCK I2C] readBytes ← addr=0x%02X reg=0x%02X len=%d → zeros%n",
                deviceAddr, register, length);
        // Масив нулів — project-код перевіряє хіба довжину, а не конкретні байти у mock-режимі
        return new byte[length];
    }

    // -------------------------------------------------------------------------
    // Внутрішня логіка LCD-симулятора
    // -------------------------------------------------------------------------

    /**
     * Interprets an LCD write and updates the in-memory display buffer.
     *
     * @param register 0x00 = command byte, 0x01 = data (character) byte
     * @param value    the command or character byte
     */
    private void handleLcdByte(int register, byte value) {
        if (register == 0x00) {
            // Команда HD44780: 0x01=clear, 0x80=cursor to row1, 0xC0=cursor to row2
            if (value == 0x01) {
                java.util.Arrays.fill(lcdLine1, ' ');
                java.util.Arrays.fill(lcdLine2, ' ');
                writingLine2 = false;
                cursorPos = 0;
            } else if ((value & 0xFF) == 0x80) {
                // Set cursor address 0x00 → рядок 1
                writingLine2 = false;
                cursorPos = 0;
            } else if ((value & 0xFF) == 0xC0) {
                // Set cursor address 0x40 → рядок 2
                writingLine2 = true;
                cursorPos = 0;
            }
        } else {
            // Символьний байт — записуємо у поточну позицію відповідного рядка
            char c = (char) (value & 0xFF);
            if (!writingLine2 && cursorPos < LCD_COLS) {
                lcdLine1[cursorPos++] = c;
            } else if (writingLine2 && cursorPos < LCD_COLS) {
                lcdLine2[cursorPos++] = c;
            }
            // Після запису оновлюємо відображення у консолі
            printLcd();
        }
    }

    /**
     * Prints the current LCD content to stdout in a box-drawing frame.
     */
    private void printLcd() {
        System.out.println("  \u250c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"
                + "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2510");
        System.out.printf("  \u2502%s\u2502%n", new String(lcdLine1));
        System.out.printf("  \u2502%s\u2502%n", new String(lcdLine2));
        System.out.println("  \u2514\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"
                + "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2518");
    }
}
