package ua.crowpi.projects.p04;

import ua.crowpi.core.hardware.GpioFacade;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Reads key presses from a 4×4 matrix keypad connected to GPIO pins.
 *
 * <p>The keypad is scanned by driving each row pin HIGH in turn and reading
 * the four column pins. When a key is pressed it connects its row and column,
 * so the corresponding column reads HIGH while that row is driven HIGH.</p>
 *
 * <p>Physical key layout:</p>
 * <pre>
 *   1  2  3  A
 *   4  5  6  B
 *   7  8  9  C
 *   *  0  #  D
 * </pre>
 *
 * <p>BCM GPIO assignments on the CrowPi:</p>
 * <ul>
 *   <li>Row pins (outputs): 17, 27, 22, 5</li>
 *   <li>Column pins (inputs): 16, 25, 24, 23</li>
 * </ul>
 *
 * <p>In mock mode the instance consumes characters from an internal queue
 * (populated via {@link #offerMockKey(char)}) instead of scanning GPIO pins.
 * This allows deterministic unit testing without real hardware.</p>
 */
public class KeypadReader {

    /** BCM GPIO pins connected to the four keypad row conductors (output). */
    public static final int[] ROW_PINS = {17, 27, 22, 5};

    /** BCM GPIO pins connected to the four keypad column conductors (input). */
    public static final int[] COL_PINS = {16, 25, 24, 23};

    /**
     * Character lookup table: {@code KEYS[row][col]} gives the character for
     * the key at the intersection of that row and column conductor.
     */
    private static final char[][] KEYS = {
            {'1', '2', '3', 'A'},
            {'4', '5', '6', 'B'},
            {'7', '8', '9', 'C'},
            {'*', '0', '#', 'D'}
    };

    /** Polling interval in milliseconds used in {@link #readPin(int)}. */
    private static final int POLL_INTERVAL_MS = 50;

    // GPIO абстракція — може бути MockGpioFacade для тестування
    private final GpioFacade gpio;

    // Черга символів для mock-режиму — дозволяє тестувати без реального клавіатурного матриксу
    private final Queue<Character> mockQueue;

    /**
     * Creates a KeypadReader that scans the 4×4 matrix via the given GPIO facade.
     *
     * @param gpio the GPIO facade used to drive row pins and read column pins
     */
    public KeypadReader(GpioFacade gpio) {
        this.gpio = gpio;
        this.mockQueue = new ArrayDeque<>();

        // Ініціалізуємо всі рядкові піни LOW — матриця не активна при старті
        for (int rowPin : ROW_PINS) {
            gpio.setOutput(rowPin, false);
        }
    }

    /**
     * Scans the keypad matrix once and returns the character of the first pressed key.
     *
     * <p>The scan works by activating each row pin HIGH one at a time and reading all
     * four column pins. If a column reads HIGH while a row is HIGH, the key at that
     * (row, column) intersection is pressed.</p>
     *
     * <p>In mock mode: returns the next character from the internal queue,
     * or {@code '\0'} if the queue is empty.</p>
     *
     * @return the character of the pressed key, or {@code '\0'} if no key is pressed
     */
    public char readKey() {
        // Mock-режим: повертаємо наступний символ з черги без GPIO опитування
        if (!mockQueue.isEmpty()) {
            return mockQueue.poll();
        }

        // Реальне GPIO сканування матриці
        for (int row = 0; row < ROW_PINS.length; row++) {
            // Активуємо поточний рядок HIGH
            gpio.setOutput(ROW_PINS[row], true);

            // Читаємо всі стовпці при активному рядку
            for (int col = 0; col < COL_PINS.length; col++) {
                if (gpio.readInput(COL_PINS[col])) {
                    // Знайшли натиснуту клавішу — деактивуємо рядок і повертаємо символ
                    gpio.setOutput(ROW_PINS[row], false);
                    return KEYS[row][col];
                }
            }

            // Деактивуємо рядок перед переходом до наступного
            gpio.setOutput(ROW_PINS[row], false);
        }

        // Жодна клавіша не натиснута
        return '\0';
    }

    /**
     * Collects exactly {@code length} key presses and returns them as a PIN string.
     *
     * <p>This method blocks until the required number of characters has been collected.
     * Between polls the thread sleeps for {@value #POLL_INTERVAL_MS} ms to avoid
     * busy-spinning on the GPIO.</p>
     *
     * <p>Duplicate reads (key held down) are suppressed: the same key is accepted
     * again only after a {@code '\0'} (no-key) reading has been observed in between,
     * simulating a proper key-release detection.</p>
     *
     * @param length the number of key presses to collect (e.g. 4 for a 4-digit PIN)
     * @return a string of exactly {@code length} characters
     */
    public String readPin(int length) {
        StringBuilder pin = new StringBuilder(length);
        // Остання зафіксована клавіша — для фільтрації утримуваних клавіш
        char lastKey = '\0';

        while (pin.length() < length) {
            char key = readKey();

            if (key != '\0' && key != lastKey) {
                // Нова клавіша (не утримана та не порожній скан) — додаємо до PIN
                pin.append(key);
                lastKey = key;
            } else if (key == '\0') {
                // Клавіша відпущена — скидаємо "останню клавішу" щоб прийняти її знову при повторному натисканні
                lastKey = '\0';
            }

            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                // Переривання потоку — відновлюємо прапорець і повертаємо те, що встигли зібрати
                Thread.currentThread().interrupt();
                break;
            }
        }

        return pin.toString();
    }

    /**
     * Enqueues a character to be returned by the next call to {@link #readKey()} in mock mode.
     *
     * <p>This method is intended for testing only. Characters are consumed in FIFO order.</p>
     *
     * @param key the character to enqueue
     */
    public void offerMockKey(char key) {
        mockQueue.offer(key);
    }
}
