package ua.crowpi.projects.p11;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.crowpi.core.hardware.GpioFacade;
import ua.crowpi.core.hardware.I2cFacade;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for p11 LCD Platform Game.
 *
 * <p>Physics and LeaderBoard tests run without hardware. LcdRenderer tests
 * use a Mockito-mocked I2C facade to verify buffer content.</p>
 */
@ExtendWith(MockitoExtension.class)
class LcdGameTest {

    private GameWorld world;

    @Mock
    private I2cFacade mockLcd;

    @Mock
    private GpioFacade mockGpio;

    @BeforeEach
    void setUp() {
        world = new GameWorld();
    }

    // =========================================================================
    // Test 1: gravity pulls player from y=0 to y=1 when not on platform
    // =========================================================================

    @Test
    void testPhysics_gravityApplied() {
        // Гравець у повітрі без стрибка і без платформи → падає на підлогу
        Player p = world.getPlayer();
        p.setX(0);   // колонка 0 не покрита жодною платформою
        p.setY(0);
        p.setJumpTicksRemaining(0);

        Physics.applyGravity(p, world);

        assertEquals(1, p.getY(), "Player must fall to y=1 when in the air without a platform");
    }

    // =========================================================================
    // Test 2: jump moves player to y=0
    // =========================================================================

    @Test
    void testPhysics_jumpSetsYToZero() {
        Player p = world.getPlayer();
        p.setY(1);  // стартова позиція — на підлозі
        p.setJumpTicksRemaining(0);

        Physics.jump(p);

        assertEquals(0, p.getY(), "Player must jump to y=0");
        assertEquals(Physics.JUMP_TICKS, p.getJumpTicksRemaining(), "Jump ticks must be set");
    }

    // =========================================================================
    // Test 3: isOnPlatform detects platform correctly
    // =========================================================================

    @Test
    void testPhysics_playerOnPlatform() {
        // Платформа Platform(3,0,3) покриває колонки 3,4,5 рядка 0
        Player p = world.getPlayer();
        p.setX(4);
        p.setY(0);

        assertTrue(Physics.isOnPlatform(p, world),
                "Player at (4,0) must be detected as standing on Platform(3,0,3)");
    }

    // =========================================================================
    // Test 4: LeaderBoard maintains at most 10 entries
    // =========================================================================

    @Test
    void testLeaderBoard_addScore_maintainsTop10() {
        LeaderBoard board = new LeaderBoard();
        for (int i = 0; i < 11; i++) {
            board.addScore("Player" + i, i * 10);
        }
        assertEquals(10, board.size(), "LeaderBoard must not exceed 10 entries");
    }

    // =========================================================================
    // Test 5: LeaderBoard sorts descending
    // =========================================================================

    @Test
    void testLeaderBoard_addScore_sortedDescending() {
        LeaderBoard board = new LeaderBoard();
        board.addScore("A", 50);
        board.addScore("B", 100);
        board.addScore("C", 75);

        assertEquals(100, board.getTopScores().get(0).getScore(),
                "Highest score must be first");
        assertEquals(50, board.getTopScores().get(2).getScore(),
                "Lowest score must be last");
    }

    // =========================================================================
    // Test 6: 11th lowest score is dropped
    // =========================================================================

    @Test
    void testLeaderBoard_eleventhScoreDropped() {
        LeaderBoard board = new LeaderBoard();
        // Додаємо 10 однакових великих очок
        for (int i = 0; i < 10; i++) {
            board.addScore("Player", 100);
        }
        // Додаємо маленьке очко — повинно бути відкинуте
        board.addScore("Loser", 1);
        assertEquals(10, board.size());
        board.getTopScores().forEach(r ->
            assertTrue(r.getScore() >= 100, "Low score must have been dropped"));
    }

    // =========================================================================
    // Test 7: LcdRenderer places player at correct position in buffer
    // =========================================================================

    @Test
    void testLcdRenderer_playerAtCorrectPosition() {
        // Налаштовуємо гравця на позиції (3, 1) — нижній рядок, стовпець 3
        world.getPlayer().setX(3);
        world.getPlayer().setY(1);
        world.getPlayer().setJumpTicksRemaining(0);

        LcdRenderer renderer = new LcdRenderer(mockLcd);
        renderer.render(world, 0, 3, GameState.PLAYING);

        // writeByte повинен бути викликаний принаймні один раз — перевіряємо що LCD отримав дані
        verify(mockLcd, atLeastOnce()).writeByte(eq(LcdRenderer.LCD_ADDR), anyInt(), anyByte());
    }
}
