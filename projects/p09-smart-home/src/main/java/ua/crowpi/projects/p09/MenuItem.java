package ua.crowpi.projects.p09;

/**
 * Contract for a single entry in the Smart Home interactive menu.
 *
 * <p>Each menu item has a short label (≤ 16 chars for LCD 16×2) and an action
 * that runs when the user selects it. Implementations are responsible for
 * updating {@link DeviceState}, logging {@link DeviceEvent}s to the database,
 * and providing feedback on the LCD.</p>
 *
 * <p>All implementations in this package follow the naming convention
 * {@code *MenuItem} (e.g. {@link LightMenuItem}, {@link FanMenuItem}).</p>
 */
public interface MenuItem {

    /**
     * Returns the label displayed on the LCD when this item is highlighted.
     *
     * <p>Must be ≤ 16 characters to fit on a single LCD 16×2 row.</p>
     *
     * @return short display label
     */
    String getLabel();

    /**
     * Executes the action associated with this menu item.
     *
     * <p>Called by {@link MenuNavigator} when the user presses SELECT while
     * this item is highlighted. Implementations should complete quickly to
     * avoid blocking the menu loop; long operations (e.g. reading a sensor)
     * should display a brief result and return.</p>
     */
    void execute();
}
