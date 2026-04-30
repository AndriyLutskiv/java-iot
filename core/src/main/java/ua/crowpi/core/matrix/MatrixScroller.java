package ua.crowpi.core.matrix;

/**
 * Scrolls a text string horizontally across an 8×8 LED matrix in a dedicated
 * daemon thread.
 *
 * <h2>Font format</h2>
 * <p>Characters are rendered with the built-in 5×7 pixel font stored in
 * <em>Adafruit GFX column format</em>: each glyph occupies 5 bytes (columns),
 * where <strong>bit 0 = top row</strong> and <strong>bit 6 = bottom row</strong>.
 * Bit 7 is always 0 (unused). A 1-pixel blank column separates consecutive
 * characters. Characters not present in the table are replaced with a 4-wide
 * blank gap.</p>
 *
 * <h2>Thread safety</h2>
 * <p>{@link #start()} and {@link #stop()} may be called from any thread.
 * The {@code volatile boolean running} flag is the only cross-thread shared
 * state; the pixel buffer is built once before the thread starts and then
 * read-only.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * MatrixScroller scroller = new MatrixScroller(matrix, "F7   Computer Engineering");
 * scroller.start();   // non-blocking — starts daemon thread
 * // ...
 * scroller.stop();    // interrupts thread, waits up to 200 ms, then clears matrix
 * }</pre>
 */
public final class MatrixScroller {

    // -------------------------------------------------------------------------
    // 5×7 font (Adafruit GFX column format: bit 0 = top, bit 6 = bottom)
    // -------------------------------------------------------------------------

    /**
     * Font table indexed by ASCII code (32–126).
     * Each entry is a 5-byte array; {@code null} entries render as a 4-column blank.
     */
    private static final byte[][] FONT = new byte[127][];

    static {
        FONT[' '] = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00};

        // ── Digits ───────────────────────────────────────────────────────────
        FONT['0'] = new byte[]{0x3E, 0x51, 0x49, 0x45, 0x3E};
        FONT['1'] = new byte[]{0x00, 0x42, 0x7F, 0x40, 0x00};
        FONT['2'] = new byte[]{0x42, 0x61, 0x51, 0x49, 0x46};
        FONT['3'] = new byte[]{0x21, 0x41, 0x45, 0x4B, 0x31};
        FONT['4'] = new byte[]{0x18, 0x14, 0x12, 0x7F, 0x10};
        FONT['5'] = new byte[]{0x27, 0x45, 0x45, 0x45, 0x39};
        FONT['6'] = new byte[]{0x3C, 0x4A, 0x49, 0x49, 0x30};
        FONT['7'] = new byte[]{0x01, 0x71, 0x09, 0x05, 0x03};
        FONT['8'] = new byte[]{0x36, 0x49, 0x49, 0x49, 0x36};
        FONT['9'] = new byte[]{0x06, 0x49, 0x49, 0x29, 0x1E};

        // ── Uppercase ─────────────────────────────────────────────────────────
        FONT['A'] = new byte[]{0x7E, 0x09, 0x09, 0x09, 0x7E};
        FONT['B'] = new byte[]{0x7F, 0x49, 0x49, 0x49, 0x36};
        FONT['C'] = new byte[]{0x3E, 0x41, 0x41, 0x41, 0x22};
        FONT['D'] = new byte[]{0x7F, 0x41, 0x41, 0x22, 0x1C};
        FONT['E'] = new byte[]{0x7F, 0x49, 0x49, 0x49, 0x41};
        FONT['F'] = new byte[]{0x7F, 0x09, 0x09, 0x09, 0x01};
        FONT['G'] = new byte[]{0x3E, 0x41, 0x49, 0x49, 0x7A};
        FONT['H'] = new byte[]{0x7F, 0x08, 0x08, 0x08, 0x7F};
        FONT['I'] = new byte[]{0x00, 0x41, 0x7F, 0x41, 0x00};
        FONT['J'] = new byte[]{0x20, 0x40, 0x41, 0x3F, 0x01};
        FONT['K'] = new byte[]{0x7F, 0x08, 0x14, 0x22, 0x41};
        FONT['L'] = new byte[]{0x7F, 0x40, 0x40, 0x40, 0x40};
        FONT['M'] = new byte[]{0x7F, 0x02, 0x0C, 0x02, 0x7F};
        FONT['N'] = new byte[]{0x7F, 0x04, 0x08, 0x10, 0x7F};
        FONT['O'] = new byte[]{0x3E, 0x41, 0x41, 0x41, 0x3E};
        FONT['P'] = new byte[]{0x7F, 0x09, 0x09, 0x09, 0x06};
        FONT['Q'] = new byte[]{0x3E, 0x41, 0x51, 0x21, 0x5E};
        FONT['R'] = new byte[]{0x7F, 0x09, 0x19, 0x29, 0x46};
        FONT['S'] = new byte[]{0x46, 0x49, 0x49, 0x49, 0x31};
        FONT['T'] = new byte[]{0x01, 0x01, 0x7F, 0x01, 0x01};
        FONT['U'] = new byte[]{0x3F, 0x40, 0x40, 0x40, 0x3F};
        FONT['V'] = new byte[]{0x1F, 0x20, 0x40, 0x20, 0x1F};
        FONT['W'] = new byte[]{0x3F, 0x40, 0x38, 0x40, 0x3F};
        FONT['X'] = new byte[]{0x63, 0x14, 0x08, 0x14, 0x63};
        FONT['Y'] = new byte[]{0x07, 0x08, 0x70, 0x08, 0x07};
        FONT['Z'] = new byte[]{0x61, 0x51, 0x49, 0x45, 0x43};

        // ── Lowercase ─────────────────────────────────────────────────────────
        FONT['a'] = new byte[]{0x20, 0x54, 0x54, 0x54, 0x78};
        FONT['b'] = new byte[]{0x7F, 0x48, 0x44, 0x44, 0x38};
        FONT['c'] = new byte[]{0x38, 0x44, 0x44, 0x44, 0x20};
        FONT['d'] = new byte[]{0x38, 0x44, 0x44, 0x48, 0x7F};
        FONT['e'] = new byte[]{0x38, 0x54, 0x54, 0x54, 0x18};
        FONT['f'] = new byte[]{0x08, 0x7E, 0x09, 0x01, 0x02};
        FONT['g'] = new byte[]{0x0C, 0x52, 0x52, 0x52, 0x3E};
        FONT['h'] = new byte[]{0x7F, 0x08, 0x04, 0x04, 0x78};
        FONT['i'] = new byte[]{0x00, 0x44, 0x7D, 0x40, 0x00};
        FONT['j'] = new byte[]{0x20, 0x40, 0x44, 0x3D, 0x00};
        FONT['k'] = new byte[]{0x7F, 0x10, 0x28, 0x44, 0x00};
        FONT['l'] = new byte[]{0x00, 0x41, 0x7F, 0x40, 0x00};
        FONT['m'] = new byte[]{0x7C, 0x04, 0x18, 0x04, 0x78};
        FONT['n'] = new byte[]{0x7C, 0x08, 0x04, 0x04, 0x78};
        FONT['o'] = new byte[]{0x38, 0x44, 0x44, 0x44, 0x38};
        FONT['p'] = new byte[]{0x7C, 0x14, 0x14, 0x14, 0x08};
        FONT['q'] = new byte[]{0x08, 0x14, 0x14, 0x18, 0x7C};
        FONT['r'] = new byte[]{0x7C, 0x08, 0x04, 0x04, 0x08};
        FONT['s'] = new byte[]{0x48, 0x54, 0x54, 0x54, 0x20};
        FONT['t'] = new byte[]{0x04, 0x3F, 0x44, 0x40, 0x20};
        FONT['u'] = new byte[]{0x3C, 0x40, 0x40, 0x20, 0x7C};
        FONT['v'] = new byte[]{0x1C, 0x20, 0x40, 0x20, 0x1C};
        FONT['w'] = new byte[]{0x3C, 0x40, 0x30, 0x40, 0x3C};
        FONT['x'] = new byte[]{0x44, 0x28, 0x10, 0x28, 0x44};
        FONT['y'] = new byte[]{0x0C, 0x50, 0x50, 0x50, 0x3C};
        FONT['z'] = new byte[]{0x44, 0x64, 0x54, 0x4C, 0x44};
    }

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    /** Delay between each pixel-column scroll step (ms). Lower = faster scroll. */
    static final int SCROLL_DELAY_MS = 75;

    /** Blank columns prepended so text enters from the right edge. */
    private static final int LEAD_BLANK_COLS = 8;

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final Max7219Matrix matrix;
    private final byte[] pixelBuffer;   // повний буфер пікселів для скролінгу

    /**
     * Signals the scroll thread to exit cleanly after the current step.
     * Written by {@link #stop()} (main thread), read by the scroll thread.
     */
    private volatile boolean running = false;

    private Thread scrollThread;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a scroller for the given text.
     *
     * <p>The pixel buffer is built once in the constructor; {@link #start()} then
     * launches the daemon thread that cycles through it indefinitely.</p>
     *
     * @param matrix the LED matrix to render on
     * @param text   ASCII text to scroll (spaces OK; unknown chars → small gap)
     */
    public MatrixScroller(Max7219Matrix matrix, String text) {
        this.matrix      = matrix;
        this.pixelBuffer = buildPixelBuffer(text);
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Starts the scrolling daemon thread (non-blocking).
     * Has no effect if already running.
     */
    public void start() {
        if (running) return;
        running      = true;
        scrollThread = new Thread(this::scrollLoop, "matrix-scroller");
        // Daemon: не заважає JVM завершитися після виходу з головного потоку
        scrollThread.setDaemon(true);
        scrollThread.start();
    }

    /**
     * Signals the scroll thread to stop, waits for it to finish, then clears the matrix.
     * Interrupts the thread's sleep so it exits within one step rather than up to
     * {@value #SCROLL_DELAY_MS} ms later.
     */
    public void stop() {
        running = false;
        if (scrollThread != null) {
            scrollThread.interrupt();
            try {
                scrollThread.join(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        matrix.clear();
    }

    // -------------------------------------------------------------------------
    // Scroll loop
    // -------------------------------------------------------------------------

    /**
     * Main scroll loop: repeatedly advances one pixel column at a time across
     * the full pixel buffer, wrapping back to the start after the last column.
     */
    private void scrollLoop() {
        int offset = 0;
        while (running) {
            renderFrame(offset);
            offset = (offset + 1) % pixelBuffer.length;
            try {
                Thread.sleep(SCROLL_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Extracts 8 consecutive pixel columns starting at {@code offset} (with
     * wrap-around) and pushes them to the matrix via {@link Max7219Matrix#setFrame}.
     */
    private void renderFrame(int offset) {
        byte[] frame = new byte[Max7219Matrix.SIZE];
        int len = pixelBuffer.length;
        for (int i = 0; i < Max7219Matrix.SIZE; i++) {
            frame[i] = pixelBuffer[(offset + i) % len];
        }
        matrix.setFrame(frame);
    }

    // -------------------------------------------------------------------------
    // Pixel buffer construction
    // -------------------------------------------------------------------------

    /**
     * Converts a string to a flat pixel-column buffer.
     *
     * <p>Layout:</p>
     * <pre>
     *   [LEAD_BLANK_COLS blank columns]
     *   [char 0: 5 cols + 1 gap col]
     *   [char 1: 5 cols + 1 gap col]
     *   ...
     *   [LEAD_BLANK_COLS blank columns]  ← seamless loop point
     * </pre>
     *
     * @param text text to render
     * @return pixel column buffer (each byte = one column in Adafruit GFX format)
     */
    public static byte[] buildPixelBuffer(String text) {
        // Кожен символ займає 5 стовпців + 1 розділовий → 6 стовпців
        int bodyLen = text.length() * 6;
        // Буфер = порожній відступ + тіло + порожній відступ для плавного циклу
        byte[] buffer = new byte[LEAD_BLANK_COLS + bodyLen + LEAD_BLANK_COLS];

        int pos = LEAD_BLANK_COLS;
        for (int ci = 0; ci < text.length(); ci++) {
            int code = text.charAt(ci);
            byte[] glyph = (code >= 0 && code < FONT.length) ? FONT[code] : null;

            if (glyph != null) {
                // 5 стовпців символу
                for (int col = 0; col < 5; col++) {
                    buffer[pos++] = glyph[col];
                }
            } else {
                // Невідомий символ → 4 порожніх стовпця (вузьке місце)
                pos += 4;
            }
            // 1 порожній стовпець-розділювач між символами
            buffer[pos++] = 0x00;
        }
        // Хвостові порожні стовпці (= плавний перехід при зациклюванні)
        return buffer;
    }
}
