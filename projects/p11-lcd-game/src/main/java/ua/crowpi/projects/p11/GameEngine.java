package ua.crowpi.projects.p11;

import ua.crowpi.core.hardware.GpioFacade;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main game loop for the LCD Platform Game.
 *
 * <p>Ticks every 100 ms via a {@link ScheduledExecutorService}.  Each tick:</p>
 * <ol>
 *   <li>Applies gravity to the player.</li>
 *   <li>Moves all enemies.</li>
 *   <li>Checks coin collection (increments score).</li>
 *   <li>Checks enemy collision (decrements health).</li>
 *   <li>Transitions state to WIN or GAME_OVER if terminal conditions are met.</li>
 *   <li>Renders the updated world to the LCD.</li>
 * </ol>
 */
public class GameEngine {

    /** Engine tick interval in milliseconds. */
    public static final long TICK_MS = 100L;

    private final GameWorld   world;
    private final LcdRenderer renderer;
    private final GpioFacade  gpio;

    private volatile GameState state = GameState.MENU;

    private ScheduledExecutorService ticker;

    // Прапор для запобігання повторного відрахування пошкоджень в одному тіку
    private boolean hitThisTick = false;

    /**
     * Constructs a GameEngine with the given world, renderer, and GPIO facade.
     *
     * @param world    the game world containing all game objects
     * @param renderer the LCD renderer for visual output
     * @param gpio     the GPIO facade for sound effects
     */
    public GameEngine(GameWorld world, LcdRenderer renderer, GpioFacade gpio) {
        this.world    = world;
        this.renderer = renderer;
        this.gpio     = gpio;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Starts the game engine: shows the menu and initialises the tick scheduler.
     */
    public void start() {
        state = GameState.MENU;
        renderer.showMessage("LCD PLATFORM GAM", "Press JUMP start");

        // Daemon-потік — не блокує завершення JVM при виході
        ticker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "game-engine-tick");
            t.setDaemon(true);
            return t;
        });
        ticker.scheduleAtFixedRate(this::tick, TICK_MS, TICK_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the game engine and cancels the tick scheduler.
     */
    public void stop() {
        if (ticker != null) {
            ticker.shutdownNow();
        }
        state = GameState.MENU;
    }

    /**
     * Executes one engine tick — called every {@value #TICK_MS} ms by the scheduler.
     *
     * <p>This method is package-private to allow unit tests to drive the engine
     * without starting the scheduler thread.</p>
     */
    void tick() {
        if (state != GameState.PLAYING) return;

        hitThisTick = false;

        // Крок 1: застосовуємо гравітацію
        Physics.applyGravity(world.getPlayer(), world);

        // Крок 2: рухаємо всіх ворогів
        for (Enemy e : world.getEnemies()) {
            e.move(0, 15);
        }

        // Крок 3: збираємо монети
        Physics.collectCoins(world.getPlayer(), world);

        // Крок 4: перевіряємо зіткнення з ворогами
        if (!hitThisTick && Physics.checkEnemyCollision(world.getPlayer(), world)) {
            hitThisTick = true;
            Player p = world.getPlayer();
            p.setHealth(p.getHealth() - 1);
            SoundEffects.HIT.play(gpio);
            if (p.getHealth() <= 0) {
                p.setAlive(false);
                state = GameState.GAME_OVER;
                SoundEffects.GAME_OVER.play(gpio);
            }
        }

        // Крок 5: перевіряємо умову перемоги
        if (world.countRemainingCoins() == 0) {
            state = GameState.WIN;
            SoundEffects.WIN.play(gpio);
        }

        // Крок 6: рендеримо поточний стан
        renderer.render(world, world.getPlayer().getScore(),
                world.getPlayer().getHealth(), state);
    }

    // -------------------------------------------------------------------------
    // Button handlers
    // -------------------------------------------------------------------------

    /**
     * Moves the player one cell to the left, clamped to column 0.
     */
    public void onButtonLeft() {
        if (state == GameState.PLAYING) {
            world.getPlayer().setX(Math.max(0, world.getPlayer().getX() - 1));
        } else if (state == GameState.MENU) {
            startGame();
        }
    }

    /**
     * Moves the player one cell to the right, clamped to column 15.
     */
    public void onButtonRight() {
        if (state == GameState.PLAYING) {
            world.getPlayer().setX(Math.min(15, world.getPlayer().getX() + 1));
        }
    }

    /**
     * Makes the player jump (only effective when standing on the ground).
     */
    public void onButtonJump() {
        if (state == GameState.MENU) {
            startGame();
        } else if (state == GameState.PLAYING) {
            Physics.jump(world.getPlayer());
            SoundEffects.JUMP.play(gpio);
        } else if (state == GameState.GAME_OVER || state == GameState.WIN) {
            // Перезапуск після завершення гри
            world.reset();
            startGame();
        }
    }

    /**
     * Toggles between PLAYING and PAUSED states.
     */
    public void onButtonPause() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        } else if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** Returns the current game state. @return current {@link GameState} */
    public GameState getState() { return state; }

    /** Returns the player's current score. @return score */
    public int getScore() { return world.getPlayer().getScore(); }

    // -------------------------------------------------------------------------
    // Private
    // -------------------------------------------------------------------------

    private void startGame() {
        world.reset();
        state = GameState.PLAYING;
    }
}
