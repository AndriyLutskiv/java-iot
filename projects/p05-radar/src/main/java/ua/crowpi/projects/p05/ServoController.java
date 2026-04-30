package ua.crowpi.projects.p05;

import ua.crowpi.core.hardware.GpioFacade;

/**
 * Controls a standard hobby servo motor connected to BCM GPIO 25 via PWM.
 *
 * <p>Standard hobby servos use a 50 Hz PWM signal where the pulse width encodes
 * the target angle:</p>
 * <ul>
 *   <li>0°   → 1.0 ms pulse (5.0% duty at 50 Hz, period = 20 ms)</li>
 *   <li>90°  → 1.5 ms pulse (7.5% duty)</li>
 *   <li>180° → 2.0 ms pulse (10.0% duty)</li>
 * </ul>
 *
 * <p>The duty cycle formula used is:<br>
 * {@code duty = (1.0f + (angleDeg / 180.0f)) / 20.0f}<br>
 * which maps linearly from 5% (0°) to 10% (180°) within the 20 ms period.</p>
 *
 * <p>Pin assignment (BCM numbering):</p>
 * <ul>
 *   <li>BCM 25 — PWM signal to servo signal wire (orange/yellow)</li>
 * </ul>
 */
public class ServoController {

    /** BCM GPIO pin number connected to the servo's PWM signal line. */
    public static final int SERVO_PIN = 25;

    /** Standard 50 Hz PWM frequency used by hobby servos. */
    private static final int SERVO_FREQ_HZ = 50;

    /** GPIO facade used to issue PWM commands. */
    private final GpioFacade gpio;

    /**
     * Last angle commanded to the servo; -1 before the first {@link #setAngle} call.
     * Used by {@link #getCurrentAngle()} to report the last known position.
     */
    private int currentAngle;

    /**
     * Creates a new ServoController that drives the servo on {@link #SERVO_PIN}.
     *
     * @param gpio GPIO facade used to send PWM signals;
     *             use {@link ua.crowpi.core.mock.MockGpioFacade} for desktop testing
     */
    public ServoController(GpioFacade gpio) {
        this.gpio = gpio;
        // Початкове значення -1 означає, що позиція ще не задана
        this.currentAngle = -1;
    }

    /**
     * Commands the servo to move to the specified angle.
     *
     * <p>The angle is converted to a PWM duty cycle using the formula:<br>
     * {@code duty = (1.0f + (angleDeg / 180.0f)) / 20.0f}<br>
     * and sent at {@value #SERVO_FREQ_HZ} Hz to {@link #SERVO_PIN}.</p>
     *
     * @param angleDeg target angle in degrees; must be in range [0, 180]
     * @throws IllegalArgumentException if {@code angleDeg} is outside [0, 180]
     */
    public void setAngle(int angleDeg) {
        if (angleDeg < 0 || angleDeg > 180) {
            // Захист від виходу за межі фізичного діапазону сервоприводу
            throw new IllegalArgumentException(
                    "Servo angle must be in range [0, 180], got: " + angleDeg);
        }

        // Розраховуємо коефіцієнт заповнення ШІМ для заданого кута
        // Формула: (1 мс + (кут/180) × 1 мс) / 20 мс = (1 + ratio) / 20
        float duty = (1.0f + (angleDeg / 180.0f)) / 20.0f;

        // Надсилаємо PWM-команду на піні сервоприводу
        gpio.pwm(SERVO_PIN, SERVO_FREQ_HZ, duty);

        // Зберігаємо поточний кут для подальшого читання
        this.currentAngle = angleDeg;
    }

    /**
     * Returns the last angle that was successfully commanded via {@link #setAngle}.
     *
     * @return last commanded angle in degrees [0, 180], or -1 if {@code setAngle}
     *         has never been called on this instance
     */
    public int getCurrentAngle() {
        return currentAngle;
    }
}
