package ua.crowpi.projects.p11;

/**
 * Constants for LCD custom characters and display symbols used in the game.
 *
 * <p>The HD44780 LCD controller supports up to 8 user-defined custom characters
 * (CGRAM addresses 0–7, referenced via control codes {@code \001}–{@code \010}).
 * Each character is a 5×8 pixel bitmap stored as 8 bytes.</p>
 *
 * <p>In mock mode these codes map to printable ASCII proxies for console rendering.</p>
 */
public final class LcdCharset {

    /** CGRAM character 1 — player sprite (rendered as {@code '@'} in console). */
    public static final char PLAYER = '\001';

    /** CGRAM character 2 — enemy sprite (rendered as {@code 'M'} in console). */
    public static final char ENEMY  = '\002';

    /** CGRAM character 3 — coin sprite (rendered as {@code 'o'} in console). */
    public static final char COIN   = '\003';

    /** CGRAM character 4 — heart / life icon (rendered as {@code '*'} in console). */
    public static final char HEART  = '\004';

    // -------------------------------------------------------------------------
    // 5×8 bitmap data for each custom character (sent to LCD CGRAM at startup)
    // -------------------------------------------------------------------------

    /**
     * Pixel bitmap for the PLAYER character (stick figure, 5×8).
     * Each byte is a row; bit 4 = leftmost pixel.
     */
    public static final byte[] PLAYER_BITMAP = {
        0b00100,  // head
        0b01110,  // shoulders
        0b00100,  // body
        0b01110,  // arms
        0b00100,  // waist
        0b01010,  // legs
        0b01010,  // legs lower
        0b00000
    };

    /**
     * Pixel bitmap for the ENEMY character (ghost/spider shape, 5×8).
     */
    public static final byte[] ENEMY_BITMAP = {
        0b01110,
        0b11111,
        0b10101,
        0b11111,
        0b11111,
        0b10101,
        0b10001,
        0b00000
    };

    /**
     * Pixel bitmap for the COIN character (circle, 5×8).
     */
    public static final byte[] COIN_BITMAP = {
        0b00000,
        0b01110,
        0b11111,
        0b11111,
        0b11111,
        0b01110,
        0b00000,
        0b00000
    };

    /**
     * Pixel bitmap for the HEART character (5×8).
     */
    public static final byte[] HEART_BITMAP = {
        0b00000,
        0b01010,
        0b11111,
        0b11111,
        0b01110,
        0b00100,
        0b00000,
        0b00000
    };

    // Utility class — private constructor prevents instantiation
    private LcdCharset() { }
}
