package ua.crowpi.projects.p09;

import ua.crowpi.core.hardware.I2cFacade;

/**
 * Utility class for writing text to a 16×2 HD44780-compatible LCD
 * via an I²C PCF8574 backpack (address 0x27).
 *
 * <p>The PCF8574 backpack drives the LCD in 4-bit mode. The protocol requires
 * sending each byte as two nibbles, with specific enable-pulse timing. This helper
 * encodes that protocol and exposes two simple write-line methods.</p>
 *
 * <p>LCD memory layout (DDRAM):</p>
 * <ul>
 *   <li>Row 0 starts at DDRAM address 0x00</li>
 *   <li>Row 1 starts at DDRAM address 0x40</li>
 * </ul>
 */
public final class LcdHelper {

    /** I²C address of the PCF8574 backpack on the CrowPi LCD. */
    public static final int LCD_ADDR = 0x21;

    // PCF8574 pin mapping bits (4-bit mode):
    // D4=bit0, D5=bit1, D6=bit2, D7=bit3, BL=bit4(backlight), EN=bit5, RW=bit6, RS=bit7
    private static final byte RS_BIT  = 0x01;  // Register Select: 0=command, 1=data
    private static final byte EN_BIT  = 0x04;  // Enable pulse
    private static final byte BL_BIT  = 0x08;  // Backlight (keep ON)

    // DDRAM address commands
    private static final byte CMD_ROW0 = (byte) 0x80; // Set cursor to row 0, col 0
    private static final byte CMD_ROW1 = (byte) 0xC0; // Set cursor to row 1, col 0

    // Утилітний клас — конструктор заблокований
    private LcdHelper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Writes a string to the specified LCD row.
     *
     * <p>The text is padded with spaces to exactly 16 characters so that previous
     * content on the row is always overwritten cleanly.</p>
     *
     * @param lcd  I2C facade connected to the LCD backpack
     * @param row  0 for top row, 1 for bottom row
     * @param text text to display; truncated/padded to 16 characters
     */
    public static void writeLine(I2cFacade lcd, int row, String text) {
        // Встановлюємо позицію курсора відповідно до рядка
        sendCommand(lcd, row == 0 ? CMD_ROW0 : CMD_ROW1);

        // Нормалізуємо рядок до рівно 16 символів
        String padded = String.format("%-16.16s", text != null ? text : "");
        for (char c : padded.toCharArray()) {
            sendData(lcd, (byte) c);
        }
    }

    /**
     * Sends an initialisation sequence to the LCD at startup.
     *
     * <p>Must be called once before any {@link #writeLine} calls.
     * Sets 4-bit mode, 2-line display, cursor off, display on, clear screen.</p>
     *
     * @param lcd I2C facade connected to the LCD backpack
     */
    public static void init(I2cFacade lcd) {
        // Послідовність ініціалізації за даташитом HD44780
        sendCommand(lcd, (byte) 0x33); // Function set (8-bit)
        sendCommand(lcd, (byte) 0x32); // Function set (4-bit)
        sendCommand(lcd, (byte) 0x28); // 4-bit, 2 lines, 5×8 dots
        sendCommand(lcd, (byte) 0x0C); // Display ON, cursor OFF
        sendCommand(lcd, (byte) 0x06); // Entry mode: increment, no shift
        sendCommand(lcd, (byte) 0x01); // Clear display
        sleepMs(5); // Clear display needs ≥ 1.52 ms to complete
    }

    // -------------------------------------------------------------------------
    // Низькорівневий протокол PCF8574 → HD44780
    // -------------------------------------------------------------------------

    /**
     * Sends a command byte (RS=0) to the LCD.
     *
     * @param lcd  I2C facade
     * @param cmd  HD44780 command byte
     */
    private static void sendCommand(I2cFacade lcd, byte cmd) {
        send(lcd, cmd, (byte) 0); // RS=0 → команда
    }

    /**
     * Sends a data byte (RS=1, i.e. a character) to the LCD.
     *
     * @param lcd   I2C facade
     * @param data  character byte to display
     */
    private static void sendData(I2cFacade lcd, byte data) {
        send(lcd, data, RS_BIT); // RS=1 → дані
    }

    /**
     * Transmits one byte to the LCD as two 4-bit nibbles with enable pulses.
     *
     * @param lcd    I2C facade
     * @param value  byte to transmit
     * @param mode   RS bit (0 = command, {@link #RS_BIT} = data)
     */
    private static void send(I2cFacade lcd, byte value, byte mode) {
        // Старший ніббл
        byte highNibble = (byte) ((value & 0xF0) | BL_BIT | mode);
        pulse(lcd, highNibble);
        // Молодший ніббл
        byte lowNibble = (byte) (((value << 4) & 0xF0) | BL_BIT | mode);
        pulse(lcd, lowNibble);
    }

    /**
     * Sends a single nibble with an EN enable pulse (HIGH → LOW).
     *
     * @param lcd    I2C facade
     * @param nibble nibble byte with control bits already set
     */
    private static void pulse(I2cFacade lcd, byte nibble) {
        // Записуємо дані з EN=1 (HIGH)
        lcd.writeByte(LCD_ADDR, 0, (byte) (nibble | EN_BIT));
        // Записуємо дані з EN=0 (LOW) — falling edge защіпує дані в LCD
        lcd.writeByte(LCD_ADDR, 0, (byte) (nibble & ~EN_BIT));
    }

    /** Sleeps for the specified milliseconds, ignoring interruptions. */
    private static void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
