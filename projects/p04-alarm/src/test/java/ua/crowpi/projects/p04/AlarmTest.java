package ua.crowpi.projects.p04;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.crowpi.core.hardware.GpioFacade;
import ua.crowpi.core.hardware.I2cFacade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the p04 Alarm System module.
 *
 * <p>Tests cover the FSM transition table, PIN validation with SHA-256 hashing,
 * lockout logic, and the integration between AlarmProject and its collaborators
 * using Mockito mocks for hardware facades.</p>
 *
 * <p>No real GPIO hardware is required — all hardware calls are intercepted
 * by mock objects.</p>
 */
@ExtendWith(MockitoExtension.class)
class AlarmTest {

    // -------------------------------------------------------------------------
    // Shared fixtures
    // -------------------------------------------------------------------------

    /** SHA-256("1234") — used as the expected hash in PinValidator tests. */
    private static final String HASH_1234 =
            "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4";

    private AlarmFsm fsm;
    private PinValidator pinValidator;

    @Mock
    private GpioFacade mockGpio;

    @Mock
    private I2cFacade mockLcd;

    @BeforeEach
    void setUp() {
        // Свіжий FSM і валідатор перед кожним тестом — ізоляція стану
        fsm = new AlarmFsm();
        pinValidator = new PinValidator(HASH_1234);
    }

    // =========================================================================
    // FSM transition tests
    // =========================================================================

    /**
     * Verifies the DISARMED → ARMED transition on CORRECT_PIN.
     */
    @Test
    void testFsmTransition_disarmedToArmed() {
        // Arrange: початковий стан DISARMED
        assertEquals(AlarmState.DISARMED, fsm.getState(), "Initial state must be DISARMED");

        // Act: правильний PIN зі знятою охороною → ставимо на охорону
        AlarmState next = fsm.transition(AlarmEvent.CORRECT_PIN);

        // Assert: перехід до ARMED
        assertEquals(AlarmState.ARMED, next,
                "DISARMED + CORRECT_PIN must transition to ARMED");
        assertEquals(AlarmState.ARMED, fsm.getState(),
                "getState() must reflect the new state");
    }

    /**
     * Verifies the ARMED → TRIGGERED transition on PIR_TRIGGERED.
     */
    @Test
    void testFsmTransition_armedToTriggered() {
        // Arrange: переводимо FSM в ARMED
        fsm.transition(AlarmEvent.CORRECT_PIN);
        assertEquals(AlarmState.ARMED, fsm.getState(), "Precondition: state must be ARMED");

        // Act: PIR спрацьовує
        AlarmState next = fsm.transition(AlarmEvent.PIR_TRIGGERED);

        // Assert: перехід до TRIGGERED
        assertEquals(AlarmState.TRIGGERED, next,
                "ARMED + PIR_TRIGGERED must transition to TRIGGERED");
    }

    /**
     * Verifies the TRIGGERED → DISARMED transition on CORRECT_PIN.
     */
    @Test
    void testFsmTransition_triggeredToDisarmed() {
        // Arrange: ARMED → TRIGGERED
        fsm.transition(AlarmEvent.CORRECT_PIN);      // → ARMED
        fsm.transition(AlarmEvent.PIR_TRIGGERED);    // → TRIGGERED
        assertEquals(AlarmState.TRIGGERED, fsm.getState(), "Precondition: state must be TRIGGERED");

        // Act: правильний PIN при тривозі — знімаємо тривогу
        AlarmState next = fsm.transition(AlarmEvent.CORRECT_PIN);

        // Assert: повернення до DISARMED
        assertEquals(AlarmState.DISARMED, next,
                "TRIGGERED + CORRECT_PIN must transition to DISARMED");
    }

    /**
     * Verifies that an undefined (illegal) transition leaves the FSM state unchanged.
     * Specifically: DISARMED + PIR_TRIGGERED has no entry in the transition table
     * and must be silently ignored.
     */
    @Test
    void testFsmInvalidTransition_doesNotChange() {
        // Arrange: стан DISARMED — PIR не повинен нічого змінювати поза охороною
        assertEquals(AlarmState.DISARMED, fsm.getState());

        // Act: невизначений перехід
        AlarmState next = fsm.transition(AlarmEvent.PIR_TRIGGERED);

        // Assert: стан залишається DISARMED
        assertEquals(AlarmState.DISARMED, next,
                "Undefined transition DISARMED + PIR_TRIGGERED must be ignored");
        assertEquals(AlarmState.DISARMED, fsm.getState(),
                "getState() must still return DISARMED after invalid transition");
    }

    // =========================================================================
    // PinValidator tests
    // =========================================================================

    /**
     * Verifies that the correct PIN ("1234") passes validation and resets the fail counter.
     */
    @Test
    void testPinValidator_correctPin() {
        // Act: вводимо правильний PIN
        boolean result = pinValidator.validate("1234");

        // Assert: валідація пройшла, лічильник не зростав
        assertTrue(result, "validate('1234') must return true for the correct PIN");
        assertEquals(0, pinValidator.getFailCount(),
                "Fail count must be 0 after a correct PIN");
        assertFalse(pinValidator.isLockedOut(),
                "isLockedOut() must be false after a correct PIN");
    }

    /**
     * Verifies that a wrong PIN fails validation and increments the fail counter.
     */
    @Test
    void testPinValidator_wrongPin() {
        // Act: вводимо неправильний PIN
        boolean result = pinValidator.validate("0000");

        // Assert: валідація провалилась, лічильник збільшився
        assertFalse(result, "validate('0000') must return false for a wrong PIN");
        assertEquals(1, pinValidator.getFailCount(),
                "Fail count must be 1 after one wrong attempt");
        assertFalse(pinValidator.isLockedOut(),
                "isLockedOut() must be false after only 1 wrong attempt");
    }

    /**
     * Verifies that isLockedOut() returns true after exactly three consecutive wrong attempts.
     */
    @Test
    void testPinValidator_lockoutAfterThreeAttempts() {
        // Act: три послідовні невірні PIN
        pinValidator.validate("0000");
        pinValidator.validate("1111");
        pinValidator.validate("2222");

        // Assert: блокування активоване
        assertEquals(3, pinValidator.getFailCount(),
                "Fail count must be 3 after three wrong attempts");
        assertTrue(pinValidator.isLockedOut(),
                "isLockedOut() must return true after 3 consecutive wrong PINs");
    }

    /**
     * Verifies that PinValidator.sha256("1234") produces the expected SHA-256 digest.
     *
     * <p>The expected value is the standard SHA-256 hash of the UTF-8 byte sequence
     * {@code "1234"} (four ASCII digits, no trailing newline).</p>
     */
    @Test
    void testPinValidator_sha256Hash() {
        // Act: обчислюємо SHA-256 від "1234"
        String actual = PinValidator.sha256("1234");

        // Assert: результат відповідає еталонному хешу (Java MessageDigest SHA-256)
        assertNotNull(actual, "sha256() must not return null");
        assertEquals(64, actual.length(),
                "SHA-256 hex digest must be exactly 64 characters");
        assertEquals(HASH_1234, actual,
                "SHA-256('1234') must equal the well-known digest");
    }

    // =========================================================================
    // AlarmProject integration test (Mockito)
    // =========================================================================

    /**
     * Verifies that when the PIR listener fires a HIGH event on the configured PIR pin
     * and the FSM is in ARMED state, the FSM transitions to TRIGGERED.
     *
     * <p>The test injects a mock {@link GpioFacade} and captures the registered
     * {@link ua.crowpi.core.hardware.PinChangeListener} via Mockito argument capture.
     * It then invokes the listener directly to simulate a PIR signal without needing
     * a real background thread or GPIO hardware.</p>
     */
    @Test
    void testAlarmProject_pirTriggerCallsFsmTransition() {
        // Arrange: налаштовуємо mock LCD щоб writeByte не кидав виняток
        // (Mockito за замовчуванням — void методи нічого не роблять)
        AlarmFsm testFsm = new AlarmFsm();
        // Переводимо FSM в ARMED напряму — тест перевіряє лише PIR → TRIGGERED
        testFsm.transition(AlarmEvent.CORRECT_PIN);
        assertEquals(AlarmState.ARMED, testFsm.getState(), "Precondition: FSM must be ARMED");

        // Ін'єктуємо тестовий FSM у AlarmProject через тестовий конструктор
        AlarmProject project = new AlarmProject(testFsm, mockGpio, mockLcd);

        // Захоплюємо зареєстрований PinChangeListener через Mockito ArgumentCaptor
        org.mockito.ArgumentCaptor<ua.crowpi.core.hardware.PinChangeListener> listenerCaptor =
                org.mockito.ArgumentCaptor.forClass(ua.crowpi.core.hardware.PinChangeListener.class);

        // Stub: setOutput нічого не робить (вже є поведінка за замовчуванням)
        // Stub: writeByte нічого не робить (void)
        // Stub: readInput повертає false (клавіші не натиснуті)
        when(mockGpio.readInput(anyInt())).thenReturn(false);

        // Запускаємо run() у окремому потоці — він блокується в main loop
        Thread runThread = new Thread(() -> {
            try {
                project.run(false); // mockMode=false — gpio вже ін'єктований
            } catch (Exception e) {
                // Очікуємо HardwareException або переривання — ігноруємо в тесті
            }
        }, "test-alarm-run");
        runThread.setDaemon(true);
        runThread.start();

        // Чекаємо щоб run() встиг зареєструвати listener
        sleepMs(300);

        // Перевіряємо що addListener був викликаний і захоплюємо listener
        verify(mockGpio, atLeastOnce()).addListener(anyInt(), listenerCaptor.capture());

        // Act: симулюємо HIGH сигнал PIR безпосередньо через listener
        // (перший захоплений listener — це PIR listener на піні 26)
        if (!listenerCaptor.getAllValues().isEmpty()) {
            listenerCaptor.getAllValues().get(0).onPinChange(26, true);
        }

        // Даємо головному циклу час обробити pirTriggered прапорець
        sleepMs(300);

        // Assert: FSM перейшов до TRIGGERED
        assertEquals(AlarmState.TRIGGERED, testFsm.getState(),
                "FSM must transition to TRIGGERED after PIR HIGH event in ARMED state");

        // Зупиняємо AlarmProject
        project.shutdown();
    }

    // =========================================================================
    // Helper
    // =========================================================================

    /** Sleeps the test thread, re-setting the interrupt flag if interrupted. */
    private static void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
