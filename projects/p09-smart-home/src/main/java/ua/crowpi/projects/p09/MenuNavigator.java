package ua.crowpi.projects.p09;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.hardware.I2cFacade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Renders a scrollable menu on the 16×2 LCD and routes navigation inputs to
 * {@link MenuItem} implementations.
 *
 * <p>The LCD always shows the current item label on row 1 and the menu position
 * on row 0 ({@code "MENU [n/total]  "}).</p>
 *
 * <p>Interaction model:</p>
 * <ul>
 *   <li>UP / DOWN — move to adjacent items (wraps around)</li>
 *   <li>SELECT    — execute the current item</li>
 *   <li>BACK      — exit the menu loop (returns from {@link #run(NavigationInput)})</li>
 * </ul>
 *
 * <h3>Testability</h3>
 * <p>The {@link #step(NavigationInput.NavAction)} method applies one action without
 * blocking. Tests call {@code step(SELECT)} directly to verify that the correct
 * {@link MenuItem#execute()} is invoked:</p>
 * <pre>{@code
 *   navigator.addItem(new LightMenuItem(mockLight, state, repo, mockLcd));
 *   navigator.step(NavAction.SELECT);
 *   verify(mockLight).toggle();
 * }</pre>
 */
public class MenuNavigator {

    private static final Logger LOG = LogManager.getLogger(MenuNavigator.class);

    /** Poll timeout for the main menu loop in milliseconds. */
    private static final long POLL_TIMEOUT_MS = 5_000L;

    private final I2cFacade lcd;
    private final List<MenuItem> items = new ArrayList<>();
    private int currentIndex = 0;

    /**
     * Creates a MenuNavigator backed by the given LCD.
     *
     * @param lcd I2C LCD facade for rendering menu items
     */
    public MenuNavigator(I2cFacade lcd) {
        this.lcd = lcd;
    }

    /**
     * Adds a menu item to the end of the list.
     *
     * @param item menu item to add; must not be {@code null}
     */
    public void addItem(MenuItem item) {
        if (item == null) throw new IllegalArgumentException("MenuItem must not be null");
        items.add(item);
    }

    /**
     * Returns an unmodifiable view of the registered items.
     *
     * @return read-only list of items
     */
    public List<MenuItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Returns the index of the currently highlighted item.
     *
     * @return current item index (0-based)
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * Programmatically moves the highlight to a specific item index.
     *
     * <p>Used in unit tests to set up the navigator in a known state before
     * calling {@link #step(NavigationInput.NavAction)}.</p>
     *
     * @param index 0-based item index; clamped to valid range
     */
    public void setCurrentIndex(int index) {
        if (items.isEmpty()) return;
        this.currentIndex = Math.max(0, Math.min(index, items.size() - 1));
    }

    /**
     * Runs the interactive menu loop until the user presses BACK or the items list is empty.
     *
     * <p>Polls for input with a {@value #POLL_TIMEOUT_MS} ms timeout; on timeout the LCD
     * is re-rendered (useful if another thread has updated device state).</p>
     *
     * @param input navigation input source
     */
    public void run(NavigationInput input) {
        if (items.isEmpty()) {
            LOG.warn("MenuNavigator.run() called with empty item list");
            return;
        }
        renderCurrent();

        while (true) {
            NavigationInput.NavAction action = input.poll(POLL_TIMEOUT_MS);
            if (action == null) {
                // Таймаут без дії — перемальовуємо LCD на випадок якщо стан змінився
                renderCurrent();
                continue;
            }
            if (step(action)) {
                // step() повернув true — вихід з меню (BACK)
                break;
            }
        }
    }

    /**
     * Applies a single navigation action.
     *
     * <p>This is the core menu logic, extracted from the loop for easy unit testing.</p>
     *
     * @param action the navigation action to apply
     * @return {@code true} if the menu should exit (action was BACK), {@code false} otherwise
     */
    public boolean step(NavigationInput.NavAction action) {
        if (items.isEmpty()) return true;

        switch (action) {
            case UP:
                // Циклічний перехід догори — якщо вже перший пункт, переходимо на останній
                currentIndex = (currentIndex - 1 + items.size()) % items.size();
                renderCurrent();
                LOG.trace("Menu UP → index={}", currentIndex);
                break;

            case DOWN:
                // Циклічний перехід вниз
                currentIndex = (currentIndex + 1) % items.size();
                renderCurrent();
                LOG.trace("Menu DOWN → index={}", currentIndex);
                break;

            case SELECT:
                // Виконуємо обраний пункт
                MenuItem current = items.get(currentIndex);
                LOG.debug("Menu SELECT → executing '{}'", current.getLabel());
                current.execute();
                // Після execute() перемальовуємо — label могло змінитись (наприклад LIGHT ON/OFF)
                renderCurrent();
                break;

            case BACK:
                LOG.debug("Menu BACK — exiting navigator");
                return true;

            default:
                break;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Відображення поточного пункту на LCD
    // -------------------------------------------------------------------------

    /**
     * Writes the current menu position and item label to the LCD.
     *
     * <p>Row 0: {@code "MENU [n/total]  "}, Row 1: item label (16 chars).</p>
     */
    private void renderCurrent() {
        if (items.isEmpty()) return;
        String header = String.format("MENU [%d/%d]     ",
                currentIndex + 1, items.size());
        LcdHelper.writeLine(lcd, 0, header);
        LcdHelper.writeLine(lcd, 1, items.get(currentIndex).getLabel());
    }
}
