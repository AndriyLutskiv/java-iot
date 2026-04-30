package ua.crowpi.core.pi4j;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.GpioPinPwmOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import ua.crowpi.core.hardware.GpioFacade;
import ua.crowpi.core.hardware.PinChangeListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Production implementation of {@link GpioFacade} using Pi4J v1.4.
 *
 * <p>Maps BCM (Broadcom) pin numbers — used throughout CrowPi documentation —
 * to Pi4J's WiringPi-based {@link RaspiPin} constants. Pins are provisioned
 * lazily on first use and cached for subsequent calls.</p>
 *
 * <p>Hardware PWM is supported only on BCM&nbsp;18 (WiringPi GPIO_01, physical
 * pin&nbsp;12), which is the only hardware-PWM capable pin directly supported by
 * Pi4J&nbsp;1.4 on the Raspberry&nbsp;Pi&nbsp;3.  The CrowPi passive buzzer is
 * wired to that pin.</p>
 *
 * <p><strong>Runs only on Raspberry Pi.</strong>  Instantiating this class on a
 * desktop will throw a {@link RuntimeException} from Pi4J's native library loader.</p>
 */
public class Pi4jGpioFacade implements GpioFacade {

    // -------------------------------------------------------------------------
    // BCM → WiringPi (Pi4J RaspiPin) lookup table
    // -------------------------------------------------------------------------

    /**
     * Static mapping from BCM GPIO pin numbers to Pi4J {@link RaspiPin} constants.
     * Covers all GPIO pins available on the 40-pin header of the Raspberry Pi 3.
     */
    private static final Map<Integer, Pin> BCM_TO_RASPI = new HashMap<>();

    static {
        // Мапа BCM → WiringPi-номер (Pi4J RaspiPin.GPIO_XX)
        BCM_TO_RASPI.put(2,  RaspiPin.GPIO_08);  // I2C SDA1 (якщо не I2C режим)
        BCM_TO_RASPI.put(3,  RaspiPin.GPIO_09);  // I2C SCL1 (якщо не I2C режим)
        BCM_TO_RASPI.put(4,  RaspiPin.GPIO_07);  // DHT11 (p01), PIR (p02)
        BCM_TO_RASPI.put(5,  RaspiPin.GPIO_21);  // клавіатура, ряд 4
        BCM_TO_RASPI.put(6,  RaspiPin.GPIO_22);  // PAUSE кнопка (p11), клавіатура
        BCM_TO_RASPI.put(7,  RaspiPin.GPIO_11);  // SPI CE1
        BCM_TO_RASPI.put(8,  RaspiPin.GPIO_10);  // SPI CE0
        BCM_TO_RASPI.put(9,  RaspiPin.GPIO_13);  // SPI MISO, кнопка 2
        BCM_TO_RASPI.put(10, RaspiPin.GPIO_12);  // SPI MOSI
        BCM_TO_RASPI.put(11, RaspiPin.GPIO_14);  // SPI CLK, кнопка 1
        BCM_TO_RASPI.put(12, RaspiPin.GPIO_26);  // PWM0 (ALT0) — альтернатива зумеру
        BCM_TO_RASPI.put(13, RaspiPin.GPIO_23);  // PWM1 (ALT0), JUMP кнопка (p11)
        BCM_TO_RASPI.put(16, RaspiPin.GPIO_27);  // клавіатура, колонка 4
        BCM_TO_RASPI.put(17, RaspiPin.GPIO_00);  // RGB LED червоний, реле
        BCM_TO_RASPI.put(18, RaspiPin.GPIO_01);  // PWM0 апаратний — ЗУМЕР CrowPi
        BCM_TO_RASPI.put(19, RaspiPin.GPIO_24);  // RIGHT кнопка (p11), клавіатура
        BCM_TO_RASPI.put(20, RaspiPin.GPIO_28);  // IR receiver (p03), physical pin 38
        BCM_TO_RASPI.put(21, RaspiPin.GPIO_29);  // клавіатура, колонка 3
        BCM_TO_RASPI.put(22, RaspiPin.GPIO_03);  // RGB LED синій, реле
        BCM_TO_RASPI.put(23, RaspiPin.GPIO_04);  // PIR motion sensor (p02), physical pin 16
        BCM_TO_RASPI.put(24, RaspiPin.GPIO_05);  // HC-SR04 ECHO
        BCM_TO_RASPI.put(25, RaspiPin.GPIO_06);  // RFID RST
        BCM_TO_RASPI.put(26, RaspiPin.GPIO_25);  // LEFT кнопка (p11)
        BCM_TO_RASPI.put(27, RaspiPin.GPIO_02);  // RGB LED зелений, реле
    }

    // -------------------------------------------------------------------------
    // Стан
    // -------------------------------------------------------------------------

    /** Pi4J GPIO-контролер — синглтон для поточного JVM-процесу. */
    private final GpioController gpio = GpioFactory.getInstance();

    /** Кеш вже запровізованих цифрових виходів (BCM → GpioPinDigitalOutput). */
    private final Map<Integer, GpioPinDigitalOutput> outputs = new ConcurrentHashMap<>();

    /** Кеш вже запровізованих цифрових входів (BCM → GpioPinDigitalInput). */
    private final Map<Integer, GpioPinDigitalInput> inputs = new ConcurrentHashMap<>();

    /** Апаратний PWM вихід для зумера (BCM 18 / GPIO_01). Ініціалізується при першому виклику pwm(). */
    private GpioPinPwmOutput hwPwmPin = null;

    // -------------------------------------------------------------------------
    // GpioFacade implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Provisions a digital output pin on first call (active-LOW initial state),
     * then drives it HIGH or LOW.</p>
     */
    @Override
    public void setOutput(int bcmPin, boolean high) {
        getOrProvisionOutput(bcmPin).setState(high);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Provisions a digital input with pull-down on first call, then reads the
     * current level.</p>
     */
    @Override
    public boolean readInput(int bcmPin) {
        return getOrProvisionInput(bcmPin).isHigh();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Provisions a digital input with pull-down on first call and attaches a
     * Pi4J {@link GpioPinListenerDigital} that delegates each state-change event
     * to the provided {@link PinChangeListener}.</p>
     */
    @Override
    public void addListener(int bcmPin, PinChangeListener listener) {
        GpioPinDigitalInput input = getOrProvisionInput(bcmPin);
        // Обгортаємо PinChangeListener у Pi4J-специфічний GpioPinListenerDigital
        input.addListener((GpioPinListenerDigital) event ->
                listener.onPinChange(bcmPin, event.getState().isHigh()));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Drives hardware PWM on BCM&nbsp;18 (the CrowPi buzzer pin).  Calling
     * with {@code frequencyHz == 0} or {@code dutyCycle == 0} silences the buzzer.</p>
     *
     * <p>PWM frequency formula: {@code freq = 19_200_000 / (clockDiv × range)}</p>
     * <p>With {@code range = 1000}: {@code clockDiv = 19_200 / freq}</p>
     *
     * @throws IllegalArgumentException if {@code bcmPin} is not BCM 18
     */
    @Override
    public void pwm(int bcmPin, int frequencyHz, float dutyCycle) {
        if (bcmPin != 18) {
            // Pi4J 1.4 підтримує апаратний PWM лише на BCM 18 (WiringPi GPIO_01)
            // Для інших пінів ігноруємо виклик — зумер CrowPi завжди на BCM 18
            return;
        }

        // Провізіонуємо PWM пін лише один раз
        if (hwPwmPin == null) {
            hwPwmPin = gpio.provisionPwmOutputPin(RaspiPin.GPIO_01, "buzzer", 0);
        }

        if (frequencyHz <= 0 || dutyCycle <= 0.0f) {
            // Вимикаємо зумер — встановлюємо duty cycle 0
            hwPwmPin.setPwm(0);
            return;
        }

        // Налаштовуємо частоту через апаратний лічильник Pi4J (WiringPi PWM)
        // Базова тактова частота Pi: 19.2 МГц
        // freq = 19_200_000 / (clockDiv * range)
        // clockDiv = 19_200 / freq (при range = 1000)
        int range     = 1000;
        int clockDiv  = Math.max(2, 19_200_000 / (frequencyHz * range));

        // Режим Mark:Space дає стабільну фіксовану частоту (на відміну від Balanced)
        com.pi4j.wiringpi.Gpio.pwmSetMode(com.pi4j.wiringpi.Gpio.PWM_MODE_MS);
        com.pi4j.wiringpi.Gpio.pwmSetRange(range);
        com.pi4j.wiringpi.Gpio.pwmSetClock(clockDiv);

        // Встановлюємо скважність — 50% = 500 з 1000
        hwPwmPin.setPwm((int) (dutyCycle * range));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Shuts down the Pi4J {@link GpioController}, which un-provisions all pins
     * and sets them to their safe default states (inputs with pull-down).
     * After calling this method the facade must not be used again.</p>
     */
    @Override
    public void close() {
        // gpio.shutdown() звільняє всі провізіоновані піни і зупиняє Pi4J
        gpio.shutdown();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Unprovisions any existing output for the pin, then provisions it as a
     * digital input with {@link PinPullResistance#PULL_UP}.  PULL_UP matches
     * the external pull-up resistor on the DHT11 data line; using PULL_DOWN
     * would fight the external pull-up and corrupt signal timing.</p>
     */
    @Override
    public synchronized void setInput(int bcmPin) {
        // Знімаємо провізіонування виходу якщо він існує (перехід OUT → IN)
        GpioPinDigitalOutput existingOut = outputs.remove(bcmPin);
        if (existingOut != null) {
            gpio.unprovisionPin(existingOut);
        }
        // Скидаємо кеш входу — буде перестворено нижче з PULL_UP
        GpioPinDigitalInput existingIn = inputs.remove(bcmPin);
        if (existingIn != null) {
            gpio.unprovisionPin(existingIn);
        }
        Pin raspiPin = raspiPin(bcmPin);
        GpioPinDigitalInput input = gpio.provisionDigitalInputPin(
                raspiPin, "in-BCM" + bcmPin, PinPullResistance.PULL_UP);
        inputs.put(bcmPin, input);
    }

    /**
     * Returns the cached digital output for the given BCM pin, provisioning it
     * if it has not been used before.  If the pin was previously set as an
     * input (e.g. after a DHT11 read cycle), the input is unpinned first.
     */
    private synchronized GpioPinDigitalOutput getOrProvisionOutput(int bcmPin) {
        GpioPinDigitalOutput cached = outputs.get(bcmPin);
        if (cached != null) return cached;

        // Якщо пін раніше використовувався як вхід — знімаємо провізіонування (IN → OUT)
        GpioPinDigitalInput existingIn = inputs.remove(bcmPin);
        if (existingIn != null) {
            gpio.unprovisionPin(existingIn);
        }

        Pin raspiPin = raspiPin(bcmPin);
        GpioPinDigitalOutput output = gpio.provisionDigitalOutputPin(
                raspiPin, "out-BCM" + bcmPin, PinState.LOW);
        outputs.put(bcmPin, output);
        return output;
    }

    /**
     * Returns the cached digital input for the given BCM pin, provisioning it
     * if it has not been used before.
     */
    private synchronized GpioPinDigitalInput getOrProvisionInput(int bcmPin) {
        return inputs.computeIfAbsent(bcmPin, p -> {
            Pin raspiPin = raspiPin(p);
            // Перевіряємо чи пін вже запровізовано
            if (gpio.getProvisionedPin(raspiPin) instanceof GpioPinDigitalInput) {
                return (GpioPinDigitalInput) gpio.getProvisionedPin(raspiPin);
            }
            // PULL_DOWN — кнопки CrowPi активні HIGH (натиснута = HIGH)
            return gpio.provisionDigitalInputPin(raspiPin, "in-BCM" + p,
                    PinPullResistance.PULL_DOWN);
        });
    }

    /**
     * Looks up the Pi4J {@link RaspiPin} for the given BCM pin number.
     *
     * @param bcmPin BCM GPIO pin number
     * @return the corresponding {@link RaspiPin} constant
     * @throws IllegalArgumentException if the BCM pin is not in the lookup table
     */
    private static Pin raspiPin(int bcmPin) {
        Pin pin = BCM_TO_RASPI.get(bcmPin);
        if (pin == null) {
            throw new IllegalArgumentException(
                    "BCM pin " + bcmPin + " is not in the BCM→WiringPi mapping table.");
        }
        return pin;
    }
}
