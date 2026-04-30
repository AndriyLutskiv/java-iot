package ua.crowpi.projects.p07;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for p07 Automatic Greenhouse Controller.
 *
 * <p>All tests verify pure business logic (rules engine, manual override,
 * actuator state) without real hardware or sensors.</p>
 */
class GreenhouseTest {

    private ThresholdConfig config;
    private ManualOverride  override;
    private GreenhouseController controller;

    @BeforeEach
    void setUp() {
        config     = new ThresholdConfig();
        override   = new ManualOverride();
        controller = new GreenhouseController(config, override);
    }

    // =========================================================================
    // Test 1: dry soil → pump ON
    // =========================================================================

    @Test
    void testGreenhouseController_pumpOnWhenDrySoil() {
        // Вологість ґрунту нижча за мінімальний поріг → насос увімкнути
        GreenhouseReading dry = new GreenhouseReading(22.0, 55.0, 20, ts());
        ActuatorState state = controller.evaluate(dry);
        assertTrue(state.isPumpOn(), "Pump must be ON when soil is too dry");
    }

    // =========================================================================
    // Test 2: high temperature → fan ON
    // =========================================================================

    @Test
    void testGreenhouseController_fanOnWhenHot() {
        // Температура вища за максимальний поріг → вентилятор увімкнути
        GreenhouseReading hot = new GreenhouseReading(35.0, 55.0, 60, ts());
        ActuatorState state = controller.evaluate(hot);
        assertTrue(state.isFanOn(), "Fan must be ON when temperature exceeds threshold");
    }

    // =========================================================================
    // Test 3: both dry and hot → both ON
    // =========================================================================

    @Test
    void testGreenhouseController_bothOnWhenDryAndHot() {
        GreenhouseReading both = new GreenhouseReading(35.0, 55.0, 20, ts());
        ActuatorState state = controller.evaluate(both);
        assertTrue(state.isPumpOn(), "Pump must be ON");
        assertTrue(state.isFanOn(),  "Fan must be ON");
        assertEquals(ActuatorState.BOTH_ON, state);
    }

    // =========================================================================
    // Test 4: normal conditions → all OFF
    // =========================================================================

    @Test
    void testGreenhouseController_allOffWhenNormal() {
        // Нормальні умови: ґрунт достатньо вологий, температура в нормі
        GreenhouseReading normal = new GreenhouseReading(22.0, 55.0, 60, ts());
        ActuatorState state = controller.evaluate(normal);
        assertFalse(state.isPumpOn(), "Pump must be OFF under normal conditions");
        assertFalse(state.isFanOn(),  "Fan must be OFF under normal conditions");
        assertEquals(ActuatorState.ALL_OFF, state);
    }

    // =========================================================================
    // Test 5: manual override forces pump regardless of soil moisture
    // =========================================================================

    @Test
    void testManualOverride_overridesAutoLogic() {
        // Нормальні умови але ручне перевизначення → насос все одно увімкнений
        override.togglePump(); // forceOn
        GreenhouseReading normal = new GreenhouseReading(22.0, 55.0, 60, ts());
        ActuatorState state = controller.evaluate(normal);
        assertTrue(state.isPumpOn(), "Manual override must force pump ON");
    }

    // =========================================================================
    // Test 6: ActuatorState.of() factory
    // =========================================================================

    @Test
    void testActuatorState_of() {
        assertEquals(ActuatorState.PUMP_ON,  ActuatorState.of(true,  false));
        assertEquals(ActuatorState.FAN_ON,   ActuatorState.of(false, true));
        assertEquals(ActuatorState.BOTH_ON,  ActuatorState.of(true,  true));
        assertEquals(ActuatorState.ALL_OFF,  ActuatorState.of(false, false));
    }

    // =========================================================================
    // Test 7: ActuatorState.withPump()
    // =========================================================================

    @Test
    void testActuatorState_withPump() {
        assertEquals(ActuatorState.PUMP_ON, ActuatorState.ALL_OFF.withPump(true));
        assertEquals(ActuatorState.ALL_OFF, ActuatorState.PUMP_ON.withPump(false));
        assertEquals(ActuatorState.BOTH_ON, ActuatorState.FAN_ON.withPump(true));
    }

    // -------------------------------------------------------------------------

    private static String ts() {
        return "2024-01-15T14:00:00";
    }
}
