package ua.crowpi.projects.p08;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.crowpi.core.hardware.GpioFacade;
import ua.crowpi.core.hardware.I2cFacade;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for p08 RFID Door Lock.
 *
 * <p>CardDatabase and AccessController tests run without hardware.
 * AccessController tests use Mockito mocks for GPIO and I2C facades.</p>
 */
@ExtendWith(MockitoExtension.class)
class RfidTest {

    private CardDatabase db;

    @Mock
    private GpioFacade mockGpio;

    @Mock
    private I2cFacade mockLcd;

    @BeforeEach
    void setUp() {
        db = new CardDatabase();
    }

    // =========================================================================
    // Test 1: known active card → GRANTED
    // =========================================================================

    @Test
    void testCardDatabase_knownActiveCard() {
        AccessResult result = db.check("AA:BB:CC:DD");
        assertEquals(AccessResult.GRANTED, result, "Active card AA:BB:CC:DD must be GRANTED");
    }

    // =========================================================================
    // Test 2: known inactive card → DENIED
    // =========================================================================

    @Test
    void testCardDatabase_knownInactiveCard() {
        AccessResult result = db.check("FF:FF:FF:FF");
        assertEquals(AccessResult.DENIED, result, "Inactive card FF:FF:FF:FF must be DENIED");
    }

    // =========================================================================
    // Test 3: unknown card → UNKNOWN_CARD
    // =========================================================================

    @Test
    void testCardDatabase_unknownCard() {
        AccessResult result = db.check("99:88:77:66");
        assertEquals(AccessResult.UNKNOWN_CARD, result, "Unregistered UID must return UNKNOWN_CARD");
    }

    // =========================================================================
    // Test 4: enum values exist
    // =========================================================================

    @Test
    void testAccessResult_enumValues() {
        // Перевіряємо що всі три значення enum присутні
        assertNotNull(AccessResult.GRANTED);
        assertNotNull(AccessResult.DENIED);
        assertNotNull(AccessResult.UNKNOWN_CARD);
        assertEquals(3, AccessResult.values().length, "AccessResult must have exactly 3 values");
    }

    // =========================================================================
    // Test 5: case-insensitive lookup
    // =========================================================================

    @Test
    void testCardDatabase_caseInsensitiveLookup() {
        // UID вводиться по-різному — CardDatabase повинна знаходити незалежно від регістру
        Optional<KnownCard> lower = db.findByUid("aa:bb:cc:dd");
        Optional<KnownCard> upper = db.findByUid("AA:BB:CC:DD");
        assertTrue(lower.isPresent(), "Lowercase UID must find the card");
        assertTrue(upper.isPresent(), "Uppercase UID must find the card");
        assertEquals(lower.get().getOwnerName(), upper.get().getOwnerName());
    }

    // =========================================================================
    // Test 6: AccessController grants access for active card (Mockito)
    // =========================================================================

    @Test
    void testAccessController_grantedForActiveCard() {
        // Arrange: will mock lcd writeByte calls (void — no stub needed)
        RelayDoorLock lock = new RelayDoorLock(mockGpio);
        AccessAttemptLogger logger = new AccessAttemptLogger();
        AccessController ctrl = new AccessController(db, lock, mockGpio, mockLcd, logger);

        // Act
        AccessResult result = ctrl.processCard("AA:BB:CC:DD");

        // Assert: результат GRANTED і зелений RGB-LED увімкнений
        assertEquals(AccessResult.GRANTED, result);
        verify(mockGpio, atLeastOnce()).setOutput(eq(AccessController.RGB_G), eq(true));
    }

    // =========================================================================
    // Test 7: AccessController denies inactive card
    // =========================================================================

    @Test
    void testAccessController_deniedForInactiveCard() {
        RelayDoorLock lock = new RelayDoorLock(mockGpio);
        AccessAttemptLogger logger = new AccessAttemptLogger();
        AccessController ctrl = new AccessController(db, lock, mockGpio, mockLcd, logger);

        AccessResult result = ctrl.processCard("FF:FF:FF:FF");

        assertEquals(AccessResult.DENIED, result);
        // Жовтий = червоний + зелений увімкнені, синій вимкнений
        verify(mockGpio, atLeastOnce()).setOutput(eq(AccessController.RGB_R), eq(true));
    }

    // =========================================================================
    // Test 8: AccessController sounds buzzer for unknown card
    // =========================================================================

    @Test
    void testAccessController_unknownCardTriggersBuzzer() {
        RelayDoorLock lock = new RelayDoorLock(mockGpio);
        AccessAttemptLogger logger = new AccessAttemptLogger();
        AccessController ctrl = new AccessController(db, lock, mockGpio, mockLcd, logger);

        AccessResult result = ctrl.processCard("DE:AD:BE:EF");

        assertEquals(AccessResult.UNKNOWN_CARD, result);
        // Перевіряємо що PWM (зумер) був викликаний для попередження
        verify(mockGpio, atLeastOnce()).pwm(eq(AccessController.BUZZER_PIN), anyInt(), anyFloat());
    }
}
