package ua.crowpi.projects.p05;

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
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * CrowPi educational project p05: <em>Ultrasonic Radar</em>.
 *
 * <p>A servo motor sweeps the HC-SR04 ultrasonic sensor through a 180° arc (0°→180°→0°)
 * in 5° steps. At each step the distance is measured, displayed on the LCD, visualised
 * via the RGB LED (green/yellow/blinking red based on zone) and logged to the console.
 * After each complete sweep the session data is exported to a JSON file and a new
 * session begins.</p>
 *
 * <p>Hardware wiring (BCM pin numbers):</p>
 * <ul>
 *   <li>HC-SR04 TRIGGER — BCM 23</li>
 *   <li>HC-SR04 ECHO    — BCM 24</li>
 *   <li>Servo PWM       — BCM 25</li>
 *   <li>LCD I²C         — address 0x27</li>
 *   <li>RGB LED R       — BCM 22</li>
 *   <li>RGB LED G       — BCM 27</li>
 *   <li>RGB LED B       — BCM 17</li>
 * </ul>
 *
 * <p>In mock mode ({@code --mock} flag) all GPIO and I²C calls are replaced by
 * {@link MockGpioFacade} / {@link MockI2cFacade} console logging. The ultrasonic
 * sensor is also simulated so the measurement loop runs without any real hardware.</p>
 */
public class RadarProject implements CrowPiProject {

    // -------------------------------------------------------------------------
    // Hardware constants
    // -------------------------------------------------------------------------

    /** I²C address of the LCD 16×2 with PCF8574 backpack. */
    static final int LCD_ADDR = 0x21;

    /** BCM pin for RGB LED red channel. */
    private static final int RGB_RED_PIN   = 22;

    /** BCM pin for RGB LED green channel. */
    private static final int RGB_GREEN_PIN = 27;

    /** BCM pin for RGB LED blue channel. */
    private static final int RGB_BLUE_PIN  = 17;

    /** Servo sweep step in degrees. */
    private static final int ANGLE_STEP = 5;

    /** Delay between successive servo steps in milliseconds. */
    private static final int STEP_DELAY_MS = 50;

    /** Pattern used for session IDs and scan timestamps. */
    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /** Simulated distance sequence used in mock mode — realistic demo values. */
    private static final double[] MOCK_DISTANCES =
            {15.0, 80.0, 35.0, 120.0, 25.0, 60.0, 42.0};

    // -------------------------------------------------------------------------
    // State fields
    // -------------------------------------------------------------------------

    /**
     * Controls whether the main sweep loop continues running.
     * Set to {@code false} by {@link #shutdown()} to terminate the loop cleanly.
     */
    private volatile boolean running = false;

    /** Current servo angle tracked across the sweep direction logic. */
    private int currentAngle = 0;

    /**
     * Direction of the current sweep: +1 for 0°→180°, -1 for 180°→0°.
     * Flipped when the sweep reaches either boundary.
     */
    private int angleDirection = 1;

    /** GPIO facade; injected in constructor or created in {@link #run(boolean)}. */
    private GpioFacade gpio;

    /** I²C facade; injected in constructor or created in {@link #run(boolean)}. */
    private I2cFacade lcd;

    private MatrixScrollerComponent matrixScroller;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Creates a RadarProject with no pre-wired hardware.
     * {@link #run(boolean)} will create the appropriate facades based on {@code mockMode}.
     */
    public RadarProject() {
        // Порожній конструктор — фасади створюються в run() залежно від режиму
    }

    /**
     * Creates a RadarProject with pre-injected hardware facades.
     *
     * <p>Intended for testing: pass Mockito mocks or {@link MockGpioFacade} /
     * {@link MockI2cFacade} instances to decouple tests from the run-time
     * facade-creation logic inside {@link #run(boolean)}.</p>
     *
     * @param gpio pre-configured GPIO facade
     * @param lcd  pre-configured I²C LCD facade
     */
    public RadarProject(GpioFacade gpio, I2cFacade lcd) {
        this.gpio = gpio;
        this.lcd  = lcd;
    }

    // -------------------------------------------------------------------------
    // CrowPiProject contract
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @return {@code "Ultrasonic Radar"}
     */
    @Override
    public String getName() {
        return "Ultrasonic Radar";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "p05"}
     */
    @Override
    public String getProjectId() {
        return "p05";
    }

    /**
     * {@inheritDoc}
     *
     * @return a one-sentence description of this project
     */
    @Override
    public String getDescription() {
        return "Servo-mounted HC-SR04 sweeps 0-180° and maps distances to coloured zones; "
               + "results exported as JSON after each sweep.";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Initialises hardware facades, then enters the main sweep loop.
     * Each iteration of the outer loop is one complete 0°→180°→0° sweep followed
     * by a JSON export. The inner loops advance the servo by {@value #ANGLE_STEP}°
     * per step with a {@value #STEP_DELAY_MS} ms pause.</p>
     *
     * <p>In mock mode the HC-SR04 measurement is replaced by {@link #mockMeasure(int)}
     * to avoid blocking on GPIO echo wait.</p>
     *
     * @param mockMode {@code true} to use mock facades and simulated distances
     * @throws HardwareException if a real hardware communication error occurs
     */
    @Override
    public void run(boolean mockMode) throws HardwareException {
        // Вибираємо між реальним і симульованим апаратним забезпеченням
        boolean matrixMock = (gpio != null) || mockMode;
        if (gpio == null) {
            if (mockMode) {
                gpio = new MockGpioFacade();
                lcd  = new MockI2cFacade();
            } else {
                // На реальному RPi 3 фасади мають бути ін'єктовані через конструктор або фабрику
                gpio = new Pi4jGpioFacade();
                lcd  = new Pi4jLcdFacade(I2CBus.BUS_1, 0x21);
            }
        }

        // ── LED matrix scroller ─────────────────────────────────────────────────
        matrixScroller = new MatrixScrollerComponent(matrixMock);
        matrixScroller.start();

        // Ініціалізуємо допоміжні компоненти
        UltrasonicSensor  sensor   = new UltrasonicSensor(gpio);
        ServoController   servo    = new ServoController(gpio);
        RadarDataExporter exporter = new RadarDataExporter();
        ConsoleRadarRenderer renderer = new ConsoleRadarRenderer();

        running      = true;
        currentAngle = 0;
        angleDirection = 1;

        System.out.println("=== ULTRASONIC RADAR — p05 ===");
        System.out.printf("Servo BCM%d | Trigger BCM%d | Echo BCM%d%n",
                ServoController.SERVO_PIN, UltrasonicSensor.TRIGGER_PIN, UltrasonicSensor.ECHO_PIN);
        System.out.println("Zones: CLEAR >50cm | WARNING 20-50cm | DANGER <20cm");
        System.out.println("-------------------------------");

        // Основний цикл — продовжується до виклику shutdown()
        while (running) {
            String sessionId = LocalDateTime.now().format(TS_FORMAT);
            ScanResult session = new ScanResult(sessionId);

            // Прямий прохід 0° → 180°
            runSweepDirection(session, sensor, servo, renderer, mockMode, 0, 180, ANGLE_STEP);
            if (!running) break;

            // Зворотний прохід 180° → 0°
            runSweepDirection(session, sensor, servo, renderer, mockMode, 180, 0, -ANGLE_STEP);
            if (!running) break;

            // Після повного циклу — відображаємо ASCII-радар та зберігаємо JSON
            renderer.render(session);
            exportSession(exporter, session);
        }

        System.out.println("[p05] Radar stopped.");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Signals the main loop to stop and releases the GPIO facade.</p>
     */
    @Override
    public void shutdown() {
        // Сигналізуємо головному циклу що треба зупинитись
        running = false;

        if (matrixScroller != null) {
            matrixScroller.stop();
            matrixScroller = null;
        }

        if (gpio != null) {
            gpio.close();
            gpio = null;
        }
        lcd = null;
    }

    // -------------------------------------------------------------------------
    // Внутрішня логіка
    // -------------------------------------------------------------------------

    /**
     * Performs one directional sweep between {@code fromAngle} and {@code toAngle}.
     *
     * <p>The sweep steps through angles by {@code step} (positive or negative) and
     * at each position: moves the servo, waits {@value #STEP_DELAY_MS} ms, measures
     * distance (real or simulated), records the scan, updates the LCD, and sets the
     * RGB LED colour.</p>
     *
     * @param session  the current scan session to collect measurements into
     * @param sensor   the ultrasonic sensor driver
     * @param servo    the servo motor driver
     * @param renderer the console renderer used for per-scan line output
     * @param mockMode {@code true} to use simulated distances
     * @param fromAngle starting angle (inclusive), degrees
     * @param toAngle   ending angle (inclusive), degrees
     * @param step      signed step size in degrees; positive = forward, negative = reverse
     * @throws HardwareException if a real sensor measurement fails
     */
    private void runSweepDirection(ScanResult session, UltrasonicSensor sensor,
                                   ServoController servo, ConsoleRadarRenderer renderer,
                                   boolean mockMode, int fromAngle, int toAngle, int step)
            throws HardwareException {

        int angle = fromAngle;
        // Цикл завершується коли кут виходить за межу toAngle (враховуємо напрям step)
        while (running && ((step > 0 && angle <= toAngle) || (step < 0 && angle >= toAngle))) {
            // Переміщаємо сервопривід на заданий кут
            servo.setAngle(angle);
            currentAngle = angle;

            // Витримуємо паузу між кроками для механічної стабілізації сервоприводу
            sleepMs(STEP_DELAY_MS);

            // Вимірюємо відстань: у mock-режимі симулюємо, на реальному hardware — вимірюємо
            double distanceCm;
            if (mockMode) {
                distanceCm = mockMeasure(angle);
            } else {
                distanceCm = sensor.measure();
            }

            // Формуємо часову мітку і будуємо об'єкт вимірювання
            String timestamp = LocalDateTime.now().format(TS_FORMAT);
            RadarScan scan = new RadarScan(angle, distanceCm, timestamp);
            session.addScan(scan);

            // Виводимо рядок CSV в консоль для моніторингу в реальному часі
            renderer.printScanLine(scan);

            // Оновлюємо LCD — перший рядок: кут і відстань
            updateLcd(angle, distanceCm);

            // Встановлюємо колір RGB LED відповідно до зони відстані
            DistanceZone zone = DistanceZone.forDistance(distanceCm);
            setRgbLed(zone);

            // Рухаємось до наступного кута кроком 5°
            angle += step;
        }
    }

    /**
     * Returns a simulated distance for the given angle using a fixed cyclic table.
     *
     * <p>The simulation cycles through {@link #MOCK_DISTANCES} using the angle index
     * ({@code angleDeg / 5 % length}) so the same angle always maps to the same value
     * within a single sweep, producing a realistic-looking radar pattern.</p>
     *
     * @param angleDeg the current servo angle
     * @return simulated distance in centimetres
     */
    private double mockMeasure(int angleDeg) {
        // Симулюємо різні відстані залежно від кута для реалістичної демонстрації
        return MOCK_DISTANCES[(angleDeg / ANGLE_STEP) % MOCK_DISTANCES.length];
    }

    /**
     * Updates the LCD 16×2 display with the current angle and distance reading.
     *
     * <p>Line 1 shows {@code "ANG:045 DIST:32cm"} (16 characters, space-padded).
     * The LCD is driven by sending individual character bytes to I²C register 0x01
     * after a cursor-positioning command.</p>
     *
     * @param angleDeg   current servo angle
     * @param distanceCm current measured distance
     */
    private void updateLcd(int angleDeg, double distanceCm) {
        // Формуємо рядок у форматі "ANG:045 DIST:32cm" — максимум 16 символів LCD
        String line = String.format("ANG:%03d DIST:%3.0fcm", angleDeg, distanceCm);
        // Обрізаємо до 16 символів щоб не виходити за межі LCD
        if (line.length() > 16) {
            line = line.substring(0, 16);
        }
        // Команда 0x80 — встановити курсор на початок першого рядка (HD44780)
        lcd.writeByte(LCD_ADDR, 0x00, (byte) 0x80);
        // Записуємо кожен символ рядка як байт даних
        for (char c : line.toCharArray()) {
            lcd.writeByte(LCD_ADDR, 0x01, (byte) c);
        }
    }

    /**
     * Sets the RGB LED to the colour corresponding to the given distance zone.
     *
     * <ul>
     *   <li>CLEAR   → green (R=LOW, G=HIGH, B=LOW)</li>
     *   <li>WARNING → yellow (R=HIGH, G=HIGH, B=LOW)</li>
     *   <li>DANGER  → red, blinks twice (R=HIGH, G=LOW, B=LOW, repeated)</li>
     * </ul>
     *
     * @param zone the distance zone determined by the latest measurement
     */
    private void setRgbLed(DistanceZone zone) {
        switch (zone) {
            case CLEAR:
                // Зелений — шлях вільний, небезпеки немає
                gpio.setOutput(RGB_RED_PIN,   false);
                gpio.setOutput(RGB_GREEN_PIN, true);
                gpio.setOutput(RGB_BLUE_PIN,  false);
                break;

            case WARNING:
                // Жовтий = червоний + зелений — попередження про наближення об'єкта
                gpio.setOutput(RGB_RED_PIN,   true);
                gpio.setOutput(RGB_GREEN_PIN, true);
                gpio.setOutput(RGB_BLUE_PIN,  false);
                break;

            case DANGER:
                // Червоне миготіння — об'єкт дуже близько, потрібна негайна реакція
                for (int i = 0; i < 2; i++) {
                    gpio.setOutput(RGB_RED_PIN,   true);
                    gpio.setOutput(RGB_GREEN_PIN, false);
                    gpio.setOutput(RGB_BLUE_PIN,  false);
                    sleepMs(100);
                    gpio.setOutput(RGB_RED_PIN,   false);
                    sleepMs(100);
                }
                // Повертаємо стабільний червоний після моргань
                gpio.setOutput(RGB_RED_PIN, true);
                break;

            default:
                // На випадок невідомої зони — вимикаємо LED
                gpio.setOutput(RGB_RED_PIN,   false);
                gpio.setOutput(RGB_GREEN_PIN, false);
                gpio.setOutput(RGB_BLUE_PIN,  false);
                break;
        }
    }

    /**
     * Exports the completed scan session to a JSON file in the current directory.
     *
     * <p>The filename follows the pattern {@code scan_yyyyMMdd_HHmmss.json}.
     * Any IO errors are printed to {@code System.err} and do not terminate the
     * radar loop — losing one export is less critical than stopping the sweep.</p>
     *
     * @param exporter the exporter to use
     * @param session  the completed scan session
     */
    private void exportSession(RadarDataExporter exporter, ScanResult session) {
        String filename = "scan_" + session.getSessionId() + ".json";
        try {
            exporter.exportToFile(session, filename);
            System.out.println("[p05] Exported " + session.getScanCount()
                    + " scans to " + filename);
        } catch (IOException e) {
            // Невдала експортація не є критичною — продовжуємо сканування
            System.err.println("[p05] Export failed for " + filename + ": " + e.getMessage());
        }
    }

    /**
     * Pauses the current thread for the specified number of milliseconds.
     * Re-sets the interrupt flag if interrupted.
     *
     * @param ms milliseconds to sleep; silently no-ops if {@code ms <= 0}
     */
    private static void sleepMs(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // Відновлюємо прапорець переривання — не ковтаємо InterruptedException
            Thread.currentThread().interrupt();
        }
    }
}
