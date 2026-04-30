package ua.crowpi.projects.p09;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.hardware.GpioFacade;
import ua.crowpi.core.hardware.PinChangeListener;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Translates CrowPi keypad button presses into {@link NavigationInput.NavAction} events
 * and enqueues them for consumption by {@link MenuNavigator}.
 *
 * <p>CrowPi keypad mapping (4 navigation buttons):</p>
 * <ul>
 *   <li>Button A (GPIO 26) → {@link NavigationInput.NavAction#UP}</li>
 *   <li>Button B (GPIO 19) → {@link NavigationInput.NavAction#DOWN}</li>
 *   <li>Button C (GPIO 13) → {@link NavigationInput.NavAction#SELECT}</li>
 *   <li>Button D (GPIO 6)  → {@link NavigationInput.NavAction#BACK}</li>
 * </ul>
 *
 * <p>Buttons are active-LOW with internal pull-ups: a press brings the pin LOW.
 * The {@link PinChangeListener} is registered for both edges; only LOW transitions
 * (button pressed) are enqueued to avoid duplicate events on release.</p>
 */
public class InputRouter implements NavigationInput {

    private static final Logger LOG = LogManager.getLogger(InputRouter.class);

    // BCM GPIO pin numbers for CrowPi navigation buttons
    public static final int PIN_UP     = 26;
    public static final int PIN_DOWN   = 19;
    public static final int PIN_SELECT = 13;
    public static final int PIN_BACK   = 6;

    /** Bounded queue prevents memory growth from rapid button presses. */
    private final BlockingQueue<NavAction> queue = new LinkedBlockingQueue<>(32);

    private final GpioFacade gpio;

    /**
     * Creates an InputRouter and registers GPIO listeners for the four navigation buttons.
     *
     * @param gpio the GPIO facade to use for pin listener registration
     */
    public InputRouter(GpioFacade gpio) {
        this.gpio = gpio;
        // Реєструємо listener для кожної кнопки навігації
        gpio.addListener(PIN_UP,     makeListener(NavAction.UP));
        gpio.addListener(PIN_DOWN,   makeListener(NavAction.DOWN));
        gpio.addListener(PIN_SELECT, makeListener(NavAction.SELECT));
        gpio.addListener(PIN_BACK,   makeListener(NavAction.BACK));
        LOG.debug("InputRouter: GPIO listeners registered on pins {},{},{},{}",
                PIN_UP, PIN_DOWN, PIN_SELECT, PIN_BACK);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Blocks until an action is enqueued or the timeout elapses.</p>
     */
    @Override
    public NavAction poll(long timeoutMs) {
        try {
            // Блокуємо не більше timeoutMs мілісекунд
            return queue.poll(Math.max(timeoutMs, 0), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    // -------------------------------------------------------------------------

    /**
     * Creates a {@link PinChangeListener} that enqueues the given action when
     * the button is pressed (pin goes LOW = {@code high == false}).
     *
     * @param action the action to enqueue on button press
     * @return configured PinChangeListener
     */
    private PinChangeListener makeListener(NavAction action) {
        return (pin, high) -> {
            if (!high) {
                // Кнопка active-LOW: LOW (high=false) → натискання → ставимо у чергу
                boolean offered = queue.offer(action);
                if (!offered) {
                    // Черга повна — ігноруємо подію щоб не блокуватись у listener-потоці
                    LOG.warn("Input queue full — discarding action {} on pin={}", action, pin);
                }
            }
        };
    }
}
