package ua.crowpi.projects.p04;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Alarm System — CrowPi educational project p04.
 *
 * <p>Demonstrates:</p>
 * <ul>
 *   <li>Finite State Machine (FSM) pattern — {@link AlarmFsm}</li>
 *   <li>SHA-256 PIN hashing — {@link PinValidator}</li>
 *   <li>GPIO polling (PIR sensor, 4×4 keypad) — {@link KeypadReader}</li>
 *   <li>Hardware PWM siren in a daemon thread — {@link SirenPlayer}</li>
 *   <li>GPIO relay control — {@link RelayController}</li>
 * </ul>
 *
 * <p><strong>System states and colours:</strong></p>
 * <ul>
 *   <li>DISARMED — RGB green, relay off, siren off</li>
 *   <li>ARMED    — RGB yellow, relay off, siren off</li>
 *   <li>TRIGGERED — RGB red, relay on, siren playing</li>
 *   <li>LOCKED   — RGB purple, relay off, siren off</li>
 * </ul>
 *
 * <p><strong>Startup sequence:</strong></p>
 * <ol>
 *   <li>Load {@link AlarmConfig} from {@code alarm.properties}.</li>
 *   <li>Initialise hardware facades (real or mock).</li>
 *   <li>Register PIR listener on GPIO 26.</li>
 *   <li>Enter main loop: PIN entry → validate → FSM event → state action.</li>
 * </ol>
 *
 * <p><strong>Lockout logic:</strong> After three consecutive wrong PINs,
 * {@link PinValidator#isLockedOut()} returns {@code true}. The project then
 * forces the FSM into LOCKED via {@link AlarmFsm#setState(AlarmState)} and
 * schedules a one-shot timer (via {@link ScheduledExecutorService}) to inject
 * the {@link AlarmEvent#LOCK_EXPIRED} event after the configured duration.</p>
 */
public class AlarmProject implements CrowPiProject {

    private static final Logger LOG = LogManager.getLogger(AlarmProject.class);

    /** BCM GPIO pin number of the HC-SR501 PIR motion sensor. */
    private static final int PIR_PIN = 26;

    /** RGB LED BCM GPIO pins (active-HIGH, common-cathode on CrowPi). */
    private static final int RGB_RED_PIN   = 6;
    private static final int RGB_GREEN_PIN = 13;
    private static final int RGB_BLUE_PIN  = 19;

    /** I²C address of the PCF8574 LCD backpack. */
    private static final int LCD_ADDR = 0x21;

    // -------------------------------------------------------------------------
    // Runtime components — ініціалізуються в run(), звільняються в shutdown()
    // -------------------------------------------------------------------------

    private final AlarmFsm fsm;
    private GpioFacade gpio;
    private I2cFacade lcd;

    private PinValidator pinValidator;
    private RelayController relay;
    private SirenPlayer siren;
    private KeypadReader keypad;
    private AlarmLogger alarmLogger;
    private AlarmConfig config;

    // Планувальник для одноразового таймера розблокування
    private final ScheduledExecutorService lockoutScheduler;

    // volatile — прапорець читається і головним потоком, і обробником PIR (інший потік)
    private volatile boolean pirTriggered = false;

    // volatile — безпечне завершення головного циклу з shutdown()
    private volatile boolean running = false;

    private MatrixScrollerComponent matrixScroller;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Creates an AlarmProject for production use.
     * Hardware facades and FSM are created lazily in {@link #run(boolean)}.
     */
    public AlarmProject() {
        this.fsm = new AlarmFsm();
        this.lockoutScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "alarm-lockout-timer");
            // Daemon — не блокує завершення JVM
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Creates an AlarmProject with injected collaborators — intended for unit testing.
     *
     * <p>The provided FSM, GPIO facade and I2C facade are used directly in {@link #run(boolean)}
     * without creating new instances, allowing full dependency injection in tests.</p>
     *
     * @param fsm  pre-configured FSM instance
     * @param gpio GPIO facade (may be a mock)
     * @param lcd  I2C facade for the LCD (may be a mock)
     */
    public AlarmProject(AlarmFsm fsm, GpioFacade gpio, I2cFacade lcd) {
        this.fsm = fsm;
        this.gpio = gpio;
        this.lcd  = lcd;
        this.lockoutScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "alarm-lockout-timer");
            t.setDaemon(true);
            return t;
        });
    }

    // -------------------------------------------------------------------------
    // CrowPiProject contract
    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "Alarm System";
    }

    /** {@inheritDoc} */
    @Override
    public String getProjectId() {
        return "p04";
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "PIR-triggered alarm with SHA-256 PIN code, keypad entry, siren and relay.";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Initialises hardware facades, loads configuration, registers the PIR listener,
     * then enters the main control loop until the user presses 'D' (EXIT).</p>
     *
     * @param mockMode {@code true} to run with {@link MockGpioFacade} and {@link MockI2cFacade}
     * @throws HardwareException if real hardware is requested but not yet implemented
     */
    @Override
    public void run(boolean mockMode) throws HardwareException {
        running = true;
        LOG.info("AlarmProject starting (mockMode={})", mockMode);

        // ----------------------------------------------------------------
        // 1. Завантажуємо конфігурацію з classpath
        // ----------------------------------------------------------------
        config = new AlarmConfig();
        alarmLogger = new AlarmLogger(AlarmProject.class.getName());

        // ----------------------------------------------------------------
        // 2. Ініціалізуємо hardware-фасади
        // ----------------------------------------------------------------
        boolean matrixMock = (gpio != null) || mockMode;
        if (gpio == null) {
            // gpio не був ін'єктований — створюємо відповідно до режиму
            if (mockMode) {
                gpio = new MockGpioFacade();
                lcd  = new MockI2cFacade();
            } else {
                // Реальне GPIO Pi4J — студент реалізує пізніше
                gpio = new Pi4jGpioFacade();
                lcd  = new Pi4jLcdFacade(I2CBus.BUS_1, 0x21);
            }
        }

        // ── LED matrix scroller ─────────────────────────────────────────────────
        matrixScroller = new MatrixScrollerComponent(matrixMock);
        matrixScroller.start();

        // ----------------------------------------------------------------
        // 3. Створюємо компоненти
        // ----------------------------------------------------------------
        pinValidator = new PinValidator(config.getPinHash());
        relay        = new RelayController(gpio);
        siren        = new SirenPlayer(gpio);
        keypad       = new KeypadReader(gpio);

        // ----------------------------------------------------------------
        // 4. Реєструємо PIR listener — спрацьовує в окремому потоці
        // ----------------------------------------------------------------
        gpio.addListener(PIR_PIN, (pin, high) -> {
            // PIR переходить HIGH при детекції руху; ігноруємо LOW
            if (high && fsm.getState() == AlarmState.ARMED) {
                // Встановлюємо прапорець — головний цикл обробить його безпечно
                pirTriggered = true;
                LOG.debug("PIR HIGH detected on pin {}", pin);
            }
        });

        // ----------------------------------------------------------------
        // 5. Ініціалізуємо LCD і показуємо стартовий екран
        // ----------------------------------------------------------------
        lcdInit();
        updateDisplay("", "");

        // ----------------------------------------------------------------
        // 6. Головний цикл управління
        // ----------------------------------------------------------------
        LOG.info("Alarm main loop started");
        while (running) {

            // --- 6a. Перевіряємо PIR (атомарно читаємо і скидаємо прапорець) ---
            if (pirTriggered) {
                pirTriggered = false;
                if (fsm.getState() == AlarmState.ARMED) {
                    AlarmState prev = fsm.getState();
                    AlarmState next = fsm.transition(AlarmEvent.PIR_TRIGGERED);
                    alarmLogger.logEvent(prev, AlarmEvent.PIR_TRIGGERED, next);
                    LOG.info("PIR triggered! State: {} → {}", prev, next);
                    applyStateActions();
                }
            }

            // --- 6b. Якщо система заблокована — не приймаємо PIN ---
            if (fsm.getState() == AlarmState.LOCKED) {
                updateDisplay("", "LOCKED OUT");
                sleepMs(200);
                continue;
            }

            // --- 6c. Відображаємо поточний стан на LCD ---
            updateDisplay("", "");

            // --- 6d. Зчитуємо PIN з клавіатури ---
            String pin = readPinWithDisplay(4);

            // Якщо PIN порожній (наприклад, через переривання) — продовжуємо цикл
            if (pin.isEmpty()) {
                continue;
            }

            // --- 6e. Валідуємо PIN ---
            boolean correct = pinValidator.validate(pin);
            alarmLogger.logPinAttempt(correct, pinValidator.getFailCount());

            if (correct) {
                AlarmState prev = fsm.getState();
                AlarmState next = fsm.transition(AlarmEvent.CORRECT_PIN);
                alarmLogger.logEvent(prev, AlarmEvent.CORRECT_PIN, next);
                LOG.info("Correct PIN. State: {} → {}", prev, next);
                applyStateActions();
            } else {
                AlarmState prev = fsm.getState();
                // Надсилаємо WRONG_PIN як інформацію (FSM не має переходу по ньому)
                fsm.transition(AlarmEvent.WRONG_PIN);
                alarmLogger.logEvent(prev, AlarmEvent.WRONG_PIN, fsm.getState());
                LOG.warn("Wrong PIN attempt #{}", pinValidator.getFailCount());

                // Перевіряємо чи не вичерпано ліміт спроб
                if (pinValidator.isLockedOut()) {
                    activateLockout();
                }
            }

            sleepMs(200);
        }

        LOG.info("AlarmProject main loop exited");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stops the siren, deactivates the relay, closes the GPIO facade,
     * and shuts down the lockout timer. Idempotent.</p>
     */
    @Override
    public void shutdown() {
        if (!running) {
            return; // ідемпотентність — безпечний повторний виклик
        }
        running = false;

        if (matrixScroller != null) {
            matrixScroller.stop();
            matrixScroller = null;
        }

        if (siren != null) {
            siren.stop();
        }
        if (relay != null) {
            relay.deactivate();
        }
        if (gpio != null) {
            gpio.close();
        }

        // Зупиняємо планувальник блокування
        lockoutScheduler.shutdownNow();

        LOG.info("AlarmProject shutdown complete");
    }

    // -------------------------------------------------------------------------
    // Внутрішня логіка
    // -------------------------------------------------------------------------

    /**
     * Applies hardware actions appropriate for the current FSM state:
     * starts/stops the siren, activates/deactivates the relay, and
     * updates the LCD and RGB LED colours.
     */
    private void applyStateActions() {
        AlarmState state = fsm.getState();

        switch (state) {
            case DISARMED:
                // Знімаємо тривогу — зупиняємо сирену та реле
                siren.stop();
                relay.deactivate();
                setRgbColor(false, true, false);  // зелений
                break;

            case ARMED:
                // На охороні — тихо чекаємо, зелений → жовтий
                siren.stop();
                relay.deactivate();
                setRgbColor(true, true, false);   // жовтий = червоний + зелений
                break;

            case TRIGGERED:
                // Тривога! Сирена і реле увімкнено — червоний колір
                siren.start();
                relay.activate();
                setRgbColor(true, false, false);  // червоний
                break;

            case LOCKED:
                // Заблоковано — фіолетовий колір
                siren.stop();
                relay.deactivate();
                setRgbColor(true, false, true);   // фіолетовий = червоний + синій
                break;

            default:
                break;
        }

        updateDisplay("", "");
    }

    /**
     * Forces the FSM into LOCKED state and schedules a one-shot timer to inject
     * {@link AlarmEvent#LOCK_EXPIRED} and reset the PIN validator after the
     * configured lockout duration.
     */
    private void activateLockout() {
        LOG.warn("Activating lockout for {} seconds", config.getLockoutDurationSeconds());

        // Примусово встановлюємо LOCKED — PinValidator.isLockedOut() є зовнішньою умовою,
        // яка не може бути виражена як FSM-перехід без зберігання лічильника всередині FSM
        fsm.setState(AlarmState.LOCKED);
        applyStateActions();

        // Одноразовий таймер — після закінчення блокування скидаємо стан
        lockoutScheduler.schedule(() -> {
            LOG.info("Lockout expired — transitioning to DISARMED");
            pinValidator.reset();
            AlarmState prev = fsm.getState();
            AlarmState next = fsm.transition(AlarmEvent.LOCK_EXPIRED);
            alarmLogger.logEvent(prev, AlarmEvent.LOCK_EXPIRED, next);
            applyStateActions();
        }, config.getLockoutDurationSeconds(), TimeUnit.SECONDS);
    }

    /**
     * Reads {@code length} key presses from the keypad while displaying a masked PIN
     * (asterisks) on the LCD second row in real time.
     *
     * @param length the number of digits to read
     * @return the collected PIN string; may be shorter than {@code length} if interrupted
     */
    private String readPinWithDisplay(int length) {
        StringBuilder pin = new StringBuilder(length);
        char lastKey = '\0';

        while (pin.length() < length && running && fsm.getState() != AlarmState.LOCKED) {
            // Якщо PIR спрацював під час введення PIN — перериваємо негайно,
            // щоб головний цикл міг обробити подію (ARMED → TRIGGERED)
            if (pirTriggered) {
                break;
            }
            char key = keypad.readKey();

            if (key != '\0' && key != lastKey) {
                // Ігноруємо спеціальні клавіші (A–D, *, #) при введенні PIN
                if (Character.isDigit(key)) {
                    pin.append(key);
                    lastKey = key;
                    // Оновлюємо маску PIN на LCD: відображаємо * для кожного введеного символу
                    String mask = repeat('*', pin.length());
                    updateDisplay("", mask);
                }
            } else if (key == '\0') {
                lastKey = '\0';
            }

            sleepMs(50);
        }

        return pin.toString();
    }

    /**
     * Updates both rows of the 16×2 LCD with current state information.
     *
     * <p>Row 1 shows {@code "STATUS: <state>"}.
     * Row 2 shows {@code "PIN: <suffix>"} where suffix is the masked PIN or a status message.</p>
     *
     * @param pinSuffix extra text for the PIN row (e.g. masked input, "OK", "FAIL")
     * @param override  if non-empty, replaces the full second row content
     */
    private void updateDisplay(String pinSuffix, String override) {
        String line1 = "STATUS: " + fsm.getState();
        String line2 = override.isEmpty() ? "PIN: " + pinSuffix : override;
        lcdWriteLine(0, line1);
        lcdWriteLine(1, line2);
    }

    /**
     * Sends a formatted line to the LCD via the I2C facade.
     * The text is padded/truncated to exactly 16 characters.
     *
     * @param row  0 for the top row, 1 for the bottom row
     * @param text text content (will be padded/truncated to 16 chars)
     */
    private void lcdWriteLine(int row, String text) {
        // Команда встановлення курсора: 0x80 = рядок 0, 0xC0 = рядок 1
        byte rowCmd = (row == 0) ? (byte) 0x80 : (byte) 0xC0;
        lcd.writeByte(LCD_ADDR, 0x00, rowCmd);

        // Нормалізуємо до рівно 16 символів
        String padded = String.format("%-16.16s", text != null ? text : "");
        for (char c : padded.toCharArray()) {
            lcd.writeByte(LCD_ADDR, 0x01, (byte) c);
        }
    }

    /**
     * Sends the LCD initialisation command sequence (clear + entry mode).
     */
    private void lcdInit() {
        // Команди ініціалізації HD44780 у 4-bit режимі через PCF8574
        lcd.writeByte(LCD_ADDR, 0x00, (byte) 0x01); // Clear display
        sleepMs(5);
        lcd.writeByte(LCD_ADDR, 0x00, (byte) 0x0C); // Display ON, cursor OFF
    }

    /**
     * Sets the RGB LED colour by driving the three colour pins.
     *
     * @param red   {@code true} to turn the red component on
     * @param green {@code true} to turn the green component on
     * @param blue  {@code true} to turn the blue component on
     */
    private void setRgbColor(boolean red, boolean green, boolean blue) {
        gpio.setOutput(RGB_RED_PIN,   red);
        gpio.setOutput(RGB_GREEN_PIN, green);
        gpio.setOutput(RGB_BLUE_PIN,  blue);
    }

    /**
     * Returns a string consisting of {@code count} repetitions of {@code ch}.
     *
     * @param ch    the character to repeat
     * @param count number of repetitions; if negative returns an empty string
     * @return the repeated-character string
     */
    private static String repeat(char ch, int count) {
        // Java 11 не має String.repeat(int) для char — будуємо вручну
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }

    /**
     * Sleeps the current thread for the given number of milliseconds,
     * restoring the interrupt flag if interrupted.
     *
     * @param ms milliseconds to sleep
     */
    private static void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // Відновлюємо прапорець переривання — не гасимо його мовчки
            Thread.currentThread().interrupt();
        }
    }
}
