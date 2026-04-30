package ua.crowpi.projects.p11;

import java.util.ArrayList;
import java.util.List;

/**
 * Static utility class providing physics simulation for the LCD platform game.
 *
 * <p>Because the LCD is only 2 rows high, the physics model is simplified:</p>
 * <ul>
 *   <li>y=0 is the top row (air / high platforms)</li>
 *   <li>y=1 is the bottom row (floor / ground level)</li>
 *   <li>A jump raises the player to y=0 for {@code JUMP_TICKS} engine ticks, then falls back</li>
 * </ul>
 */
public final class Physics {

    /** Number of ticks the player stays at y=0 after jumping. */
    public static final int JUMP_TICKS = 3;

    // Utility class — no instances
    private Physics() { }

    /**
     * Applies gravity to the player for one engine tick.
     *
     * <p>If the player has jump ticks remaining, the counter is decremented.
     * Otherwise, if the player is at y=0 and not standing on a platform,
     * they fall to y=1.</p>
     *
     * @param player the player to update
     * @param world  the game world (used to check platforms)
     */
    public static void applyGravity(Player player, GameWorld world) {
        if (player.getJumpTicksRemaining() > 0) {
            // Гравець у стрибку — зменшуємо лічильник тіків у повітрі
            player.setJumpTicksRemaining(player.getJumpTicksRemaining() - 1);
            return;
        }

        // Якщо гравець на верхньому рядку і не стоїть на платформі — падає вниз
        if (player.getY() == 0 && !isOnPlatform(player, world)) {
            player.setY(1);
            player.setVelocityY(0);
        }
    }

    /**
     * Initiates a jump: moves the player to y=0 and sets the jump tick counter.
     *
     * <p>Only works if the player is currently on the ground (y=1).</p>
     *
     * @param player the player to jump
     */
    public static void jump(Player player) {
        // Стрибок можливий лише з підлоги — не можна стрибнути в повітрі
        if (player.getY() == 1) {
            player.setY(0);
            player.setJumpTicksRemaining(JUMP_TICKS);
            player.setVelocityY(-1);
        }
    }

    /**
     * Returns {@code true} if the player is currently standing on any platform.
     *
     * @param player the player to check
     * @param world  the game world containing platforms
     * @return {@code true} if the player's position is covered by a platform
     */
    public static boolean isOnPlatform(Player player, GameWorld world) {
        for (Platform p : world.getPlatforms()) {
            if (p.contains(player.getX(), player.getY())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the player is occupying the same cell as any alive enemy.
     *
     * @param player the player to check
     * @param world  the game world containing enemies
     * @return {@code true} if a collision with an enemy is detected
     */
    public static boolean checkEnemyCollision(Player player, GameWorld world) {
        for (Enemy e : world.getEnemies()) {
            if (e.isAlive() && e.getX() == player.getX() && e.getY() == player.getY()) {
                // Гравець зіткнувся з ворогом — втрачає здоров'я
                return true;
            }
        }
        return false;
    }

    /**
     * Collects all uncollected coins at the player's current position.
     *
     * <p>Each collected coin increments the player's score by 1.</p>
     *
     * @param player the player to check for coin collection
     * @param world  the game world containing coins
     * @return list of coins that were collected this tick (may be empty)
     */
    public static List<Coin> collectCoins(Player player, GameWorld world) {
        List<Coin> collected = new ArrayList<>();
        for (Coin c : world.getCoins()) {
            if (!c.isCollected() && c.getX() == player.getX() && c.getY() == player.getY()) {
                c.collect();
                // Збільшуємо рахунок гравця за кожну зібрану монету
                player.setScore(player.getScore() + 1);
                collected.add(c);
            }
        }
        return collected;
    }
}
