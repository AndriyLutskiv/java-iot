package ua.crowpi.projects.p09;

/**
 * Abstraction over the physical input device(s) used to navigate the Smart Home menu.
 *
 * <p>In real mode, {@link InputRouter} maps CrowPi keypad buttons A/B/C/D and
 * IR remote arrow keys to {@link NavAction} values. In mock (laptop) mode,
 * {@link ConsoleNavigationInput} reads key strokes from stdin.</p>
 *
 * <p>Implementations must be thread-safe because {@link MenuNavigator} calls
 * {@link #poll} from its dedicated loop thread.</p>
 */
public interface NavigationInput {

    /**
     * Enumerates the logical navigation actions available in the menu.
     *
     * <ul>
     *   <li>{@link #UP} — move highlight to the previous menu item</li>
     *   <li>{@link #DOWN} — move highlight to the next menu item</li>
     *   <li>{@link #SELECT} — execute the currently highlighted item</li>
     *   <li>{@link #BACK} — exit the current submenu / return to main menu</li>
     * </ul>
     */
    enum NavAction {
        UP, DOWN, SELECT, BACK
    }

    /**
     * Blocks until a navigation action is available or the timeout expires.
     *
     * <p>Returns {@code null} on timeout so the caller can perform periodic
     * housekeeping (e.g. refresh the LCD) even when the user is idle.</p>
     *
     * @param timeoutMs maximum time to wait in milliseconds; ≤ 0 waits indefinitely
     * @return the next {@link NavAction}, or {@code null} if the timeout elapsed
     */
    NavAction poll(long timeoutMs);
}
