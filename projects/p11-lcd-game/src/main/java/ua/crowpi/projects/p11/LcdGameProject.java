package ua.crowpi.projects.p11;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.CrowPiProject;
import ua.crowpi.core.exception.HardwareException;
import ua.crowpi.core.hardware.GpioFacade;
import ua.crowpi.core.hardware.I2cFacade;
import ua.crowpi.core.matrix.MatrixScrollerComponent;
import ua.crowpi.core.mock.MockGpioFacade;
import ua.crowpi.core.mock.MockI2cFacade;
import com.pi4j.io.i2c.I2CBus;
import ua.crowpi.core.pi4j.Pi4jGpioFacade;
import ua.crowpi.core.pi4j.Pi4jLcdFacade;

/**
 * LCD Platform Game — CrowPi educational project p11.
 *
 * <p>Demonstrates:</p>
 * <ul>
 *   <li>Game loop with fixed-rate {@link java.util.concurrent.ScheduledExecutorService} ticks</li>
 *   <li>Double-buffered LCD rendering ({@link LcdRenderer})</li>
 *   <li>Simplified 2-row physics ({@link Physics})</li>
 *   <li>Concurrent enemy movement</li>
 *   <li>JSON leaderboard persistence via Jackson ({@link LeaderBoard})</li>
 * </ul>
 */
public class LcdGameProject implements CrowPiProject {

    private static final Logger LOG = LogManager.getLogger(LcdGameProject.class);

    /** BCM GPIO pin for the left-movement button. */
    public static final int LEFT_PIN  = 26;
    /** BCM GPIO pin for the right-movement button. */
    public static final int RIGHT_PIN = 19;
    /** BCM GPIO pin for the jump button. */
    public static final int JUMP_PIN  = 13;
    /** BCM GPIO pin for the pause button. */
    public static final int PAUSE_PIN = 6;

    // -------------------------------------------------------------------------
    // Runtime state
    // -------------------------------------------------------------------------

    private volatile boolean running = false;

    private GpioFacade gpio;
    private I2cFacade  lcd;
    private GameEngine engine;

    private MatrixScrollerComponent matrixScroller;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** Creates an LcdGameProject for production use. */
    public LcdGameProject() { }

    /**
     * Creates an LcdGameProject with injected facades — for unit testing.
     *
     * @param gpio GPIO facade
     * @param lcd  I2C LCD facade
     */
    public LcdGameProject(GpioFacade gpio, I2cFacade lcd) {
        this.gpio = gpio;
        this.lcd  = lcd;
    }

    // -------------------------------------------------------------------------
    // CrowPiProject contract
    // -------------------------------------------------------------------------

    @Override public String getName()        { return "LCD Platform Game"; }
    @Override public String getProjectId()   { return "p11"; }
    @Override public String getDescription() {
        return "2-row LCD platform game with physics, enemies, coins, and JSON leaderboard.";
    }

    @Override
    public void run(boolean mockMode) throws HardwareException {
        running = true;
        LOG.info("LcdGameProject starting (mockMode={})", mockMode);

        // Ініціалізуємо hardware-фасади
        boolean matrixMock = (gpio != null) || mockMode;
        if (gpio == null) {
            if (mockMode) {
                gpio = new MockGpioFacade();
                lcd  = new MockI2cFacade();
            } else {
                gpio = new Pi4jGpioFacade();
                lcd  = new Pi4jLcdFacade(I2CBus.BUS_1, 0x21);
            }
        }

        // ── LED matrix scroller ─────────────────────────────────────────────────
        matrixScroller = new MatrixScrollerComponent(matrixMock);
        matrixScroller.start();

        GameWorld   world    = new GameWorld();
        LcdRenderer renderer = new LcdRenderer(lcd);
        LeaderBoard board    = new LeaderBoard();
        engine = new GameEngine(world, renderer, gpio);

        // Реєструємо GPIO-слухачів для кнопок керування
        gpio.addListener(LEFT_PIN,  (pin, high) -> { if (high) engine.onButtonLeft();  });
        gpio.addListener(RIGHT_PIN, (pin, high) -> { if (high) engine.onButtonRight(); });
        gpio.addListener(JUMP_PIN,  (pin, high) -> { if (high) engine.onButtonJump();  });
        gpio.addListener(PAUSE_PIN, (pin, high) -> { if (high) engine.onButtonPause(); });

        engine.start();
        LOG.info("Game engine started. Press JUMP to begin.");

        // Головний цикл — чекаємо до завершення гри або shutdown()
        while (running) {
            sleepMs(200);

            // Перевіряємо стан гри для збереження рейтингу
            GameState gs = engine.getState();
            if ((gs == GameState.GAME_OVER || gs == GameState.WIN) && running) {
                int score = engine.getScore();
                LOG.info("Game ended: state={} score={}", gs, score);

                if (board.isTopScore(score)) {
                    // В реальному режимі — введення імені через кнопки
                    // У mock — генеруємо ім'я автоматично
                    String playerName = mockMode ? "PLAYER" : "PLAYER";
                    board.addScore(playerName, score);
                    try {
                        board.save();
                        LOG.info("Leaderboard saved with score {}", score);
                    } catch (Exception e) {
                        LOG.warn("Could not save leaderboard: {}", e.getMessage());
                    }
                }

                // Чекаємо натискання JUMP для перезапуску (engine обробляє)
                sleepMs(3_000);
            }
        }
    }

    @Override
    public void shutdown() {
        if (!running) return;
        running = false;
        if (engine != null) engine.stop();
        if (gpio != null) gpio.close();
        LOG.info("LcdGameProject shutdown complete");
    }

    private static void sleepMs(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
