package ua.crowpi.projects.p06;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ua.crowpi.core.hardware.GpioFacade;
import ua.crowpi.core.hardware.I2cFacade;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the p06-reaction game logic.
 *
 * <p>All tests operate purely on plain Java objects (POJOs) or use Mockito mocks
 * for hardware facades, so no Raspberry Pi hardware is required.</p>
 */
@ExtendWith(MockitoExtension.class)
class ReactionTest {

    // Mockito ін'єктує фейкові реалізації для апаратних залежностей
    @Mock
    private GpioFacade mockGpio;

    @Mock
    private I2cFacade mockLcd;

    @Mock
    private StimulusGenerator mockStimGen;

    // -------------------------------------------------------------------------
    // PlayerStats tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link PlayerStats#getAverage()} returns the correct
     * arithmetic mean for a three-element list.
     */
    @Test
    void testPlayerStats_average() {
        List<Long> times = Arrays.asList(200L, 300L, 400L);
        PlayerStats stats = new PlayerStats(times);

        // (200 + 300 + 400) / 3 = 300.0
        assertEquals(300.0, stats.getAverage(), 0.001,
                "Average of [200, 300, 400] should be 300.0 ms");
    }

    /**
     * Verifies that {@link PlayerStats#getMin()} returns the smallest value
     * from the reaction times list.
     */
    @Test
    void testPlayerStats_min() {
        List<Long> times = Arrays.asList(200L, 300L, 400L);
        PlayerStats stats = new PlayerStats(times);

        assertEquals(200L, stats.getMin(),
                "Minimum of [200, 300, 400] should be 200 ms");
    }

    // -------------------------------------------------------------------------
    // GameSession winner tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link GameSession#getWinner()} returns Player 1 when P1
     * wins more rounds than P2 within the same session.
     */
    @Test
    void testGameSession_getWinner_player1Wins() {
        Player p1 = new Player("PLAYER 1");
        Player p2 = new Player("PLAYER 2");
        GameSession session = new GameSession(p1, p2);

        // Стимул=1000, P1 натиснув раніше в обох раундах → P1 має +2
        long stimTime = 1000L;
        session.addRound(new GameRound(stimTime, 1200L, 1500L)); // P1 wins
        session.addRound(new GameRound(stimTime, 1100L, 1800L)); // P1 wins

        Player winner = session.getWinner();
        // P1 має 2 бали, P2 — 0 → переможець P1
        assertEquals(p1, winner, "Player 1 should win with 2 rounds won");
    }

    /**
     * Verifies that {@link GameSession#getWinner()} returns {@code null} when
     * both players have equal scores (a draw).
     */
    @Test
    void testGameSession_getWinner_draw() {
        Player p1 = new Player("PLAYER 1");
        Player p2 = new Player("PLAYER 2");
        GameSession session = new GameSession(p1, p2);

        long stimTime = 1000L;
        // P1 виграє перший раунд, P2 — другий → нічия 1:1
        session.addRound(new GameRound(stimTime, 1100L, 1500L)); // P1 wins
        session.addRound(new GameRound(stimTime, 1800L, 1200L)); // P2 wins

        Player winner = session.getWinner();
        // Однаковий рахунок — переможця немає
        assertNull(winner, "Should return null on a draw");
    }

    // -------------------------------------------------------------------------
    // False-start tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that a player who presses before the stimulus receives a -1 penalty.
     */
    @Test
    void testGameRound_falseStartPenalty() {
        Player p1 = new Player("PLAYER 1");
        Player p2 = new Player("PLAYER 2");

        // P1 натискає на 500 мс, стимул на 1000 мс → фальстарт
        GameRound round = new GameRound(1000L, 500L, 1500L);
        round.applyScores(p1, p2);

        // P1 має отримати штраф -1 за фальстарт
        assertEquals(-1, p1.getScore(), "P1 should receive -1 for a false start");
        // P2 натиснув після стимулу і є єдиним валідним — отримує +1
        assertEquals(1, p2.getScore(), "P2 should receive +1 as valid presser");
    }

    /**
     * Verifies that when P1 presses before the stimulus (false start) and P2 presses
     * after, the engine correctly awards P1 = -1 and P2 = +1.
     *
     * <p>Uses {@link ReactionEngine#runOneRound(long, long, long)} to bypass all
     * real hardware and timer logic.</p>
     */
    @Test
    void testReactionEngine_falseStartDetected() {
        // StimulusGenerator замокований — runOneRound не викликає generate() взагалі
        ReactionEngine engine = new ReactionEngine(mockGpio, mockLcd, mockStimGen);

        // stimulusTime=1000, P1 натиснув на 500 (ДО стимулу) → фальстарт
        // P2 натиснув на 1500 (ПІСЛЯ стимулу) → виграш
        GameSession session = engine.runOneRound(1000L, 500L, 1500L);

        Player p1 = engine.getP1();
        Player p2 = engine.getP2();

        // Перевіряємо штраф за фальстарт
        assertEquals(-1, p1.getScore(),
                "P1 pressed at 500 < stimulusTime 1000 — must receive -1 for false start");
        // Перевіряємо нагороду за перемогу
        assertEquals(1, p2.getScore(),
                "P2 pressed at 1500 > stimulusTime 1000 — must receive +1 for winning");
    }
}
