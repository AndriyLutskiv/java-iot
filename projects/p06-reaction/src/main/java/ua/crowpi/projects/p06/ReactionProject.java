package ua.crowpi.projects.p06;

import ua.crowpi.core.CrowPiProject;
import ua.crowpi.core.exception.HardwareException;
import ua.crowpi.core.hardware.GpioFacade;
import ua.crowpi.core.hardware.I2cFacade;
import ua.crowpi.core.mock.MockGpioFacade;
import ua.crowpi.core.mock.MockI2cFacade;
import com.pi4j.io.i2c.I2CBus;
import ua.crowpi.core.pi4j.Pi4jGpioFacade;
import ua.crowpi.core.pi4j.Pi4jLcdFacade;
import ua.crowpi.core.matrix.MatrixScrollerComponent;

/**
 * CrowPi educational project p06: <em>Reaction Speed Trainer</em>.
 *
 * <p>Two players compete to press their buttons as quickly as possible after
 * an RGB LED flashes white. The game runs 5 rounds; the player who wins the
 * most rounds is declared the session champion. False starts (pressing before
 * the LED) incur a -1 penalty. Final statistics (average, min, max reaction
 * times) are displayed in the console after the game.</p>
 *
 * <p>Hardware used:</p>
 * <ul>
 *   <li>RGB LED on BCM 9 / 10 / 11 (stimulus)</li>
 *   <li>Push button P1 on BCM 26</li>
 *   <li>Push button P2 on BCM 19</li>
 *   <li>Passive buzzer on BCM 18 (victory jingle)</li>
 *   <li>I²C LCD 16×2 (address 0x27) for status messages</li>
 * </ul>
 *
 * <p>In mock mode all GPIO/I²C interactions are replaced with console log
 * output via {@link MockGpioFacade} and {@link MockI2cFacade}.</p>
 */
public class ReactionProject implements CrowPiProject {

    /** GPIO facade instance; set during {@link #run(boolean)}. */
    private GpioFacade gpio;

    /** I²C facade instance; set during {@link #run(boolean)}. */
    private I2cFacade lcd;

    private MatrixScrollerComponent matrixScroller;

    // -------------------------------------------------------------------------
    // CrowPiProject contract
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @return {@code "Reaction Speed Trainer"}
     */
    @Override
    public String getName() {
        return "Reaction Speed Trainer";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "p06"}
     */
    @Override
    public String getProjectId() {
        return "p06";
    }

    /**
     * {@inheritDoc}
     *
     * @return a one-sentence description of this project
     */
    @Override
    public String getDescription() {
        return "Two-player reaction-speed game: hit your button first after the LED flashes "
               + "to win rounds — false starts are penalised.";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Initialises hardware facades, creates players, runs the full 5-round game
     * via {@link ReactionEngine#run()}, plays the victory jingle, and prints final
     * statistics to stdout.</p>
     *
     * @param mockMode {@code true} to use mock facades (no real RPi GPIO required)
     * @throws HardwareException if real hardware cannot be initialised (non-mock mode)
     */
    @Override
    public void run(boolean mockMode) throws HardwareException {
        // Вибираємо між реальним та симульованим апаратним забезпеченням
        if (mockMode) {
            gpio = new MockGpioFacade();
            lcd  = new MockI2cFacade();
        } else {
            // На реальному RPi 3 Pi4J-реалізації передаються ззовні через фабрику,
            // але для самостійного запуску проекту кидаємо виняток з підказкою
            gpio = new Pi4jGpioFacade();
            lcd  = new Pi4jLcdFacade(I2CBus.BUS_1, 0x21);
        }

        // ── LED matrix scroller ─────────────────────────────────────────────────
        matrixScroller = new MatrixScrollerComponent(mockMode);
        matrixScroller.start();

        // Будуємо граф залежностей: стимул → рушій → джингл
        StimulusGenerator stimGen  = new StimulusGenerator(gpio);
        VictoryJingle     jingle   = new VictoryJingle(gpio);
        ReactionEngine    engine   = new ReactionEngine(gpio, lcd, stimGen);

        System.out.println("=== REACTION SPEED TRAINER — p06 ===");
        System.out.println("Rounds: " + ReactionEngine.ROUNDS);
        System.out.println("P1 button: BCM " + ReactionEngine.BUTTON_P1
                           + "  |  P2 button: BCM " + ReactionEngine.BUTTON_P2);
        System.out.println("-------------------------------------");

        // Запускаємо гру — метод блокується до завершення всіх раундів
        GameSession session = engine.run();

        // Відтворюємо переможний джингл якщо є переможець
        Player winner = session.getWinner();
        if (winner != null) {
            jingle.play(winner.getName());
        }

        // Виводимо підсумкову статистику реакції обох гравців
        printResults(session);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Releases all GPIO resources if the facade was initialised.
     * Safe to call multiple times.</p>
     */
    @Override
    public void shutdown() {
        if (gpio != null) {
            // Вивільняємо GPIO-ресурси щоб не залишати піни у невизначеному стані
            gpio.close();
            gpio = null;
        }
        // lcd не потребує явного закриття — немає фонових потоків
        lcd = null;
    }

    // -------------------------------------------------------------------------
    // Внутрішні методи
    // -------------------------------------------------------------------------

    /**
     * Prints a formatted results table to stdout with per-player scores and
     * reaction-time statistics derived from the completed session.
     *
     * @param session the completed game session
     */
    private void printResults(GameSession session) {
        System.out.println("\n=== FINAL RESULTS ===");

        // Виводимо статистику для кожного гравця через PlayerStats
        printPlayerResult(session.getP1());
        printPlayerResult(session.getP2());

        Player winner = session.getWinner();
        if (winner != null) {
            System.out.println("\n*** WINNER: " + winner.getName()
                               + " (score " + winner.getScore() + ") ***");
        } else {
            System.out.println("\n*** IT'S A DRAW! ***");
        }
    }

    /**
     * Prints a single player's score and reaction-time statistics.
     *
     * @param player the player whose stats to print
     */
    private void printPlayerResult(Player player) {
        PlayerStats stats = new PlayerStats(player.getReactionTimesMs());
        System.out.printf(
            "  %s — Score: %2d  |  Reactions: %d  |  Avg: %.0f ms  |  Min: %s ms  |  Max: %d ms%n",
            player.getName(),
            player.getScore(),
            stats.getCount(),
            stats.getAverage(),
            // Якщо min == MAX_VALUE — даних немає, показуємо прочерк
            stats.getMin() == Long.MAX_VALUE ? "--" : Long.toString(stats.getMin()),
            stats.getMax()
        );
    }
}
