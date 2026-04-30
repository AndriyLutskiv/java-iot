package ua.crowpi.projects.p11;

import ua.crowpi.core.hardware.I2cFacade;

/**
 * Renders the game world onto the 16×2 LCD using a double-buffered char array.
 *
 * <p>On each call to {@link #render(GameWorld, int, int, GameState)} a fresh
 * {@code char[2][16]} buffer is built in memory, then the entire buffer is sent
 * to the LCD in one sweep. This "double buffering" prevents visible tearing when
 * multiple objects move in the same tick.</p>
 *
 * <p>Character mapping (console proxies shown in parentheses):</p>
 * <ul>
 *   <li>{@link LcdCharset#PLAYER} — player sprite ({@code '@'})</li>
 *   <li>{@link LcdCharset#ENEMY}  — enemy sprite ({@code 'M'})</li>
 *   <li>{@link LcdCharset#COIN}   — coin ({@code 'o'})</li>
 *   <li>{@code '-'}               — platform cell</li>
 *   <li>{@code ' '}               — empty air</li>
 * </ul>
 */
public class LcdRenderer {

    /** I²C address of the PCF8574 LCD backpack. */
    public static final int LCD_ADDR = 0x21;

    private final I2cFacade lcd;

    /**
     * Constructs an LcdRenderer backed by the given I2C facade.
     *
     * @param lcd I2C facade used to write characters to the HD44780 LCD
     */
    public LcdRenderer(I2cFacade lcd) {
        this.lcd = lcd;
    }

    /**
     * Renders the current game world state to the LCD.
     *
     * <p>Builds a {@code char[2][16]} frame buffer, then flushes it to the LCD row by row.</p>
     *
     * @param world  the game world containing all renderable objects
     * @param score  current player score (shown in status area if space allows)
     * @param health current player health (1–3)
     * @param state  current game state (affects what is rendered)
     */
    public void render(GameWorld world, int score, int health, GameState state) {
        if (state == GameState.GAME_OVER) {
            showMessage("  GAME  OVER!   ", "Score: " + score + "   HP: " + health);
            return;
        }
        if (state == GameState.WIN) {
            showMessage("  YOU  WIN!     ", "Score: " + score + "            ");
            return;
        }
        if (state == GameState.PAUSED) {
            showMessage("  == PAUSED ==  ", "Score: " + score + "            ");
            return;
        }
        if (state == GameState.MENU) {
            showMessage("LCD PLATFORM GAM", "Press JUMP start");
            return;
        }

        // Будуємо кадровий буфер — двовимірний масив символів
        char[][] buf = new char[2][16];
        // Заповнюємо пробілами — базовий "фон" сцени
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 16; col++) {
                buf[row][col] = ' ';
            }
        }

        // Малюємо платформи горизонтальними дефісами
        for (Platform p : world.getPlatforms()) {
            for (int i = 0; i < p.getWidth(); i++) {
                int col = p.getX() + i;
                if (col >= 0 && col < 16) {
                    buf[p.getY()][col] = '-';
                }
            }
        }

        // Малюємо монети (не зібрані)
        for (Coin c : world.getCoins()) {
            if (!c.isCollected() && c.getX() >= 0 && c.getX() < 16) {
                buf[c.getY()][c.getX()] = LcdCharset.COIN;
            }
        }

        // Малюємо ворогів (живих)
        for (Enemy e : world.getEnemies()) {
            if (e.isAlive() && e.getX() >= 0 && e.getX() < 16) {
                buf[e.getY()][e.getX()] = LcdCharset.ENEMY;
            }
        }

        // Малюємо гравця — завжди поверх інших об'єктів
        Player p = world.getPlayer();
        if (p.getX() >= 0 && p.getX() < 16 && p.getY() >= 0 && p.getY() < 2) {
            buf[p.getY()][p.getX()] = LcdCharset.PLAYER;
        }

        // Відправляємо кадровий буфер на LCD рядок за рядком
        writeLine(0, buf[0]);
        writeLine(1, buf[1]);
    }

    /**
     * Displays an arbitrary two-line message on the LCD.
     *
     * @param line1 text for the top row (padded/truncated to 16 chars)
     * @param line2 text for the bottom row (padded/truncated to 16 chars)
     */
    public void showMessage(String line1, String line2) {
        writeLine(0, String.format("%-16.16s", line1 != null ? line1 : "").toCharArray());
        writeLine(1, String.format("%-16.16s", line2 != null ? line2 : "").toCharArray());
    }

    /**
     * Writes a 16-character row to the LCD via the I2C facade.
     *
     * @param row   0 = top row, 1 = bottom row
     * @param chars exactly 16 characters to display
     */
    private void writeLine(int row, char[] chars) {
        // Встановлюємо курсор: 0x80 = рядок 0, 0xC0 = рядок 1 (DDRAM offset 0x40)
        byte rowCmd = (row == 0) ? (byte) 0x80 : (byte) 0xC0;
        lcd.writeByte(LCD_ADDR, 0x00, rowCmd);

        // Записуємо кожен символ послідовно через I2C
        for (int i = 0; i < Math.min(chars.length, 16); i++) {
            lcd.writeByte(LCD_ADDR, 0x01, (byte) chars[i]);
        }
    }
}
