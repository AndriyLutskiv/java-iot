package ua.crowpi.projects.p06;

import ua.crowpi.core.hardware.GpioFacade;
import ua.crowpi.core.hardware.I2cFacade;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Core game controller that drives the full 5-round Reaction Speed Trainer loop.
 *
 * <p>The engine coordinates all hardware components and game objects:</p>
 * <ul>
 *   <li>Displays round status on the LCD via {@link I2cFacade}.</li>
 *   <li>Fires the stimulus through {@link StimulusGenerator}.</li>
 *   <li>Listens for player button presses via GPIO interrupt callbacks stored
 *       atomically using {@link AtomicLong}.</li>
 *   <li>Creates {@link GameRound} objects, applies scores through
 *       {@link GameSession#addRound(GameRound)}, and tracks results.</li>
 * </ul>
 *
 * <p>Button assignments (BCM pin numbers):</p>
 * <ul>
 *   <li>Player 1 button → BCM 26</li>
 *   <li>Player 2 button → BCM 19</li>
 * </ul>
 */
public class ReactionEngine {

    /** BCM GPIO pin number for Player 1's push button. */
    public static final int BUTTON_P1 = 26;

    /** BCM GPIO pin number for Player 2's push button. */
    public static final int BUTTON_P2 = 19;

    /** Total number of rounds played per game session. */
    public static final int ROUNDS = 5;

    /**
     * How long (ms) the engine waits for button presses after the stimulus fires.
     * During this window both AtomicLong values are polled on the button interrupt.
     */
    private static final long RESPONSE_WINDOW_MS = 3000L;

    /** I²C address of the CrowPi 16×2 LCD with PCF8574 backpack. */
    private static final int LCD_ADDR = 0x21;

    /** LCD HD44780 command register address. */
    private static final int LCD_CMD = 0x00;

    /** LCD HD44780 data (character) register address. */
    private static final int LCD_DATA = 0x01;

    /** HD44780 command: clear display and return cursor home. */
    private static final byte LCD_CLEAR = 0x01;

    /** HD44780 command: set DDRAM address to line 1 column 0 (0x80). */
    private static final byte LCD_LINE1 = (byte) 0x80;

    /** HD44780 command: set DDRAM address to line 2 column 0 (0xC0). */
    private static final byte LCD_LINE2 = (byte) 0xC0;

    /** GPIO facade for button input and potentially LED control. */
    private final GpioFacade gpio;

    /** I²C facade for LCD output. */
    private final I2cFacade lcd;

    /** Stimulus generator that flashes the RGB LED at a random moment. */
    private final StimulusGenerator stimGen;

    /** Player 1 object accumulating scores and reaction times across rounds. */
    Player p1;

    /** Player 2 object accumulating scores and reaction times across rounds. */
    Player p2;

    /** The current game session tracking all rounds and overall winner. */
    GameSession session;

    // -------------------------------------------------------------------------
    // Конструктор
    // -------------------------------------------------------------------------

    /**
     * Creates a {@code ReactionEngine} wired to the given hardware abstractions.
     *
     * <p>Players and session are initialised lazily inside {@link #run()}.</p>
     *
     * @param gpio     GPIO facade for button listeners
     * @param lcd      I²C facade for the 16×2 LCD display
     * @param stimGen  pre-constructed stimulus generator
     */
    public ReactionEngine(GpioFacade gpio, I2cFacade lcd, StimulusGenerator stimGen) {
        this.gpio    = gpio;
        this.lcd     = lcd;
        this.stimGen = stimGen;
    }

    // -------------------------------------------------------------------------
    // Публічний API
    // -------------------------------------------------------------------------

    /**
     * Runs the complete 5-round game session and returns the resulting session object.
     *
     * <p>Steps per round:</p>
     * <ol>
     *   <li>Write "READY... GET SET!" to LCD row 0.</li>
     *   <li>Call {@link StimulusGenerator#generate()} — blocks until stimulus fires.</li>
     *   <li>Register GPIO listeners that atomically capture press timestamps.</li>
     *   <li>Sleep {@value #RESPONSE_WINDOW_MS} ms for the response window.</li>
     *   <li>Build a {@link GameRound}, add it to the session.</li>
     *   <li>Update LCD with round result.</li>
     * </ol>
     *
     * <p>After all rounds: display final winner on LCD and return the session.</p>
     *
     * @return the completed {@link GameSession} with all round data and scores applied
     */
    public GameSession run() {
        // Ініціалізуємо гравців та сесію на початку кожної нової гри
        p1 = new Player("PLAYER 1");
        p2 = new Player("PLAYER 2");
        session = new GameSession(p1, p2);

        for (int round = 1; round <= ROUNDS; round++) {
            // Показуємо стан "готуйся" перед очікуванням стимулу
            lcdWriteLine(0, "READY... GET SET!");
            lcdWriteLine(1, "Round " + round + " of " + ROUNDS);

            // Генерація стимулу блокує потік на випадковий час 1–4 секунди,
            // потім вмикає RGB LED і повертає точний час вмикання
            long stimulusTime = stimGen.generate();

            // AtomicLong зберігає час натискання кнопки з будь-якого потоку безпечно
            AtomicLong p1Press = new AtomicLong(0L);
            AtomicLong p2Press = new AtomicLong(0L);

            // Реєструємо слухачів кнопок — вони спрацюють з GPIO-потоку
            gpio.addListener(BUTTON_P1, (pin, high) -> {
                if (high) {
                    // compareAndSet(0, ...) — записуємо час лише першого натискання
                    p1Press.compareAndSet(0L, System.currentTimeMillis());
                }
            });
            gpio.addListener(BUTTON_P2, (pin, high) -> {
                if (high) {
                    // Аналогічно для P2 — запам'ятовуємо лише перше натискання
                    p2Press.compareAndSet(0L, System.currentTimeMillis());
                }
            });

            // Чекаємо вікно відповіді — 3 секунди для реакції обох гравців
            try {
                Thread.sleep(RESPONSE_WINDOW_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Гасимо LED після закінчення вікна відповіді
            stimGen.stopFlash();

            // Збираємо результати раунду та застосовуємо рахунки
            GameRound gameRound = new GameRound(stimulusTime, p1Press.get(), p2Press.get());
            session.addRound(gameRound);

            // Відображаємо результат раунду на LCD
            Player roundWinner = gameRound.getWinner();
            if (roundWinner != null) {
                lcdWriteLine(1, roundWinner.getName() + " WINS!");
            } else {
                lcdWriteLine(1, "NO WINNER");
            }

            // Невелика пауза щоб гравці встигли прочитати результат раунду
            try {
                Thread.sleep(1500L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Показуємо підсумковий результат після всіх раундів
        Player winner = session.getWinner();
        if (winner != null) {
            lcdWriteLine(0, "WINNER:");
            lcdWriteLine(1, winner.getName() + "!");
        } else {
            lcdWriteLine(0, "IT'S A DRAW!");
            lcdWriteLine(1, p1.getScore() + " vs " + p2.getScore());
        }

        return session;
    }

    /**
     * Creates and evaluates a single-round {@link GameSession} using the provided
     * exact timestamps.
     *
     * <p>This method is intended for unit testing: it bypasses all hardware I/O
     * and timer logic, allowing precise control over every timing parameter.</p>
     *
     * @param stimulusTime wall-clock ms when the stimulus would have fired
     * @param p1PressTime  wall-clock ms when P1 pressed; {@code 0} = did not press
     * @param p2PressTime  wall-clock ms when P2 pressed; {@code 0} = did not press
     * @return a completed {@link GameSession} with one round and scores applied
     */
    public GameSession runOneRound(long stimulusTime, long p1PressTime, long p2PressTime) {
        // Ініціалізуємо гравців та сесію для ізольованого тестового раунду
        p1 = new Player("PLAYER 1");
        p2 = new Player("PLAYER 2");
        session = new GameSession(p1, p2);

        // Створюємо раунд із наданими часами — жодного апаратного вводу/виводу
        GameRound round = new GameRound(stimulusTime, p1PressTime, p2PressTime);
        session.addRound(round);

        return session;
    }

    // -------------------------------------------------------------------------
    // Геттери (package-private для тестів + public для зовнішнього доступу)
    // -------------------------------------------------------------------------

    /**
     * Returns Player 1 of the most recently started game.
     *
     * @return P1 {@link Player} object, or {@code null} if no game has been run yet
     */
    public Player getP1() {
        return p1;
    }

    /**
     * Returns Player 2 of the most recently started game.
     *
     * @return P2 {@link Player} object, or {@code null} if no game has been run yet
     */
    public Player getP2() {
        return p2;
    }

    // -------------------------------------------------------------------------
    // Внутрішні утиліти LCD
    // -------------------------------------------------------------------------

    /**
     * Writes a text string to the specified LCD row (0 = top, 1 = bottom).
     *
     * <p>The text is padded or truncated to exactly 16 characters so the LCD
     * row is always fully overwritten with no leftover characters from a
     * previous message.</p>
     *
     * @param row  0 for the first (top) row; 1 for the second (bottom) row
     * @param text the message to display; longer strings are truncated to 16 chars
     */
    private void lcdWriteLine(int row, String text) {
        // Вирівнюємо рядок до 16 символів — інакше залишки попереднього повідомлення видно
        String padded = String.format("%-16s", text).substring(0, 16);

        // Виставляємо курсор на початок потрібного рядка LCD
        byte cursorCmd = (row == 0) ? LCD_LINE1 : LCD_LINE2;
        lcd.writeByte(LCD_ADDR, LCD_CMD, cursorCmd);

        // Посимвольно пишемо рядок у DDRAM LCD
        for (char c : padded.toCharArray()) {
            lcd.writeByte(LCD_ADDR, LCD_DATA, (byte) c);
        }
    }
}
