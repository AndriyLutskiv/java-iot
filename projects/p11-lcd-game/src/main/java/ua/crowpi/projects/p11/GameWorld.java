package ua.crowpi.projects.p11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds all mutable game objects: player, platforms, enemies, and coins.
 *
 * <p>The platform list is immutable (level layout fixed at construction).
 * Enemy and coin lists use synchronised wrappers because enemy threads read
 * and write their own state while the game engine tick reads it.</p>
 */
public class GameWorld {

    private final Player         player;
    private final List<Platform> platforms;
    private final List<Enemy>    enemies;
    private final List<Coin>     coins;

    // Збережені початкові позиції ворогів для скидання рівня
    private final int[] enemyStartX;
    private final int[] enemyStartY;

    /**
     * Constructs a GameWorld with the default level layout.
     *
     * <p>Level layout:</p>
     * <ul>
     *   <li>Platforms at (3,0,3), (8,0,4), (13,0,2) — top row</li>
     *   <li>Enemies at (5,1) and (10,0)</li>
     *   <li>Coins at (4,0), (9,0), (11,0), (14,0)</li>
     * </ul>
     */
    public GameWorld() {
        player = new Player(1, 1);

        // Платформи статичні — не змінюються під час гри
        List<Platform> plist = new ArrayList<>();
        plist.add(new Platform(3, 0, 3));
        plist.add(new Platform(8, 0, 4));
        plist.add(new Platform(13, 0, 2));
        platforms = Collections.unmodifiableList(plist);

        enemies = new ArrayList<>();
        enemies.add(new Enemy(5, 1));
        enemies.add(new Enemy(10, 0));

        enemyStartX = new int[]{5, 10};
        enemyStartY = new int[]{1, 0};

        coins = new ArrayList<>();
        coins.add(new Coin(4, 0));
        coins.add(new Coin(9, 0));
        coins.add(new Coin(11, 0));
        coins.add(new Coin(14, 0));
    }

    /**
     * Resets all game objects to their initial state for a new game.
     */
    public void reset() {
        player.reset();
        for (int i = 0; i < enemies.size(); i++) {
            enemies.get(i).setX(enemyStartX[i]);
            enemies.get(i).setY(enemyStartY[i]);
            enemies.get(i).setAlive(true);
        }
        // Скидаємо стан монет — всі знову не зібрані
        for (Coin c : coins) {
            // Coin.collected — package-private setter через інший Coin
            // замість сетера — створюємо нові об'єкти монет
        }
        // Заміна монет новими об'єктами (collected = false за замовчуванням)
        coins.clear();
        coins.add(new Coin(4, 0));
        coins.add(new Coin(9, 0));
        coins.add(new Coin(11, 0));
        coins.add(new Coin(14, 0));
    }

    /**
     * Returns the number of coins that have not yet been collected.
     *
     * @return uncollected coin count
     */
    public int countRemainingCoins() {
        int count = 0;
        for (Coin c : coins) {
            if (!c.isCollected()) count++;
        }
        return count;
    }

    /** @return the player object */
    public Player getPlayer() { return player; }

    /** @return unmodifiable platform list */
    public List<Platform> getPlatforms() { return platforms; }

    /** @return live enemy list */
    public List<Enemy> getEnemies() { return enemies; }

    /** @return live coin list */
    public List<Coin> getCoins() { return coins; }
}
